package com.saggiodev.kastle.service

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.saggiodev.kastle.db.Database
import com.saggiodev.kastle.db.GamesQueries
import com.saggiodev.kastle.db.InstalledGames
import com.saggiodev.kastle.error.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class InstallationManager private constructor(gamesDbFile: Path) {

    private val driver: JdbcSqliteDriver
    private val queries: GamesQueries
    private val installationDirectory = gamesDbFile.parent

    init {
        val jdbcString = "jdbc:sqlite:${gamesDbFile.absolutePathString()}"
        driver = JdbcSqliteDriver(url = jdbcString, schema = Database.Schema)

        queries = Database(driver).gamesQueries
    }

    /** Names, provider classes and JAR filenames are unique. Duplicates are never replaced. */
    fun installGame(name: String, gameFile: Path, className: String): Either<ConfigError, Unit> = either {
        ensure(name.isNotBlank() && className.isNotBlank()) {
            GameFileError("Game name and provider class must not be blank")
        }
        val gameFileName = ensureNotNull(gameFile.fileName?.toString()) { GameFileError("A game JAR file is required") }
        val duplicate = databaseOperation {
            queries.getFilteredGames(name, className, gameFileName).executeAsList().isNotEmpty()
        }.bind()
        ensure(!duplicate) { GameFileError.GameAlreadyExists }
        val gamesFolder = handleGamesFolder(installationDirectory).bind()
        fileOperation("Could not install game") {
            val staged = Files.createTempFile(gamesFolder.parent, "install-", ".jar")
            var copied = false
            val target = gamesFolder.resolve(gameFileName)
            try {
                Files.copy(gameFile, staged, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                validateProvider(staged, className)
                try {
                    transaction {
                        Files.move(staged, target)
                        copied = true
                        queries.insert(gameName = name, mainClass = className, fileName = gameFileName)
                    }
                } catch (failure: Exception) {
                    if (copied) {
                        try { Files.delete(target) } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
                    }
                    throw failure
                }
            } finally {
                Files.deleteIfExists(staged)
            }
        }.bind()
    }

    /** Missing files are tolerated so a stale installation can still be removed. */
    fun uninstallGame(name: String): Either<ConfigError, Unit> = either {
        val game = getByGameName(name).bind()
        val gamesFolder = handleGamesFolder(installationDirectory).bind()
        fileOperation("Could not uninstall game") {
            val target = gamesFolder.resolve(game.fileName)
            require(target.parent == gamesFolder) { "Invalid installed filename" }
            require(!Files.exists(target) || Files.isRegularFile(target)) { "Installed game is not a regular file" }
            val backup = if (Files.exists(target)) {
                val path = Files.createTempFile(gamesFolder.parent, "uninstall-", ".jar")
                try { Files.copy(target, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                catch (failure: Exception) { Files.deleteIfExists(path); throw failure }
                path
            } else null
            var retainBackup = false
            try {
                transaction {
                    queries.deleteByGameName(name)
                    Files.deleteIfExists(target)
                }
            } catch (failure: Exception) {
                if (backup != null && !Files.exists(target)) {
                    try { Files.copy(backup, target) }
                    catch (restore: Exception) {
                        retainBackup = true
                        failure.addSuppressed(IOException("Recovery failed; backup retained at $backup", restore))
                        throw failure
                    }
                }
                throw failure
            } finally {
                // Retain the backup if recovery failed, rather than discard the only remaining copy.
                if (backup != null && !retainBackup) {
                    Files.deleteIfExists(backup)
                }
            }
        }.bind()
    }

    fun getByGameName(name: String): Either<ConfigError, InstalledGames> = either {
        val game = databaseOperation { queries.getByGameName(name).executeAsOneOrNull() }.bind()
        ensureNotNull(game) { GameFileError.NonExistentGame }
    }

    fun getGames(): List<InstalledGames> = queries.getAll().executeAsList()

    private fun transaction(operation: () -> Unit) {
        try {
            queries.transaction { operation() }
        } catch (failure: Exception) {
            // SQLDelight 2.0.2 leaves the JDBC transaction active when SQLite COMMIT fails.
            driver.transaction?.let { pending ->
                try {
                    driver.run { pending.connection.rollbackTransaction() }
                } catch (rollback: Exception) {
                    failure.addSuppressed(rollback)
                } finally {
                    driver.transaction = null
                    try { driver.closeConnection(pending.connection) }
                    catch (close: Exception) { failure.addSuppressed(close) }
                }
            }
            throw failure
        }
    }

    private fun <T> databaseOperation(operation: () -> T): Either<ConfigError, T> =
        Either.catch(operation).mapLeft { DbFileError("Database operation failed: ${it.message ?: it.javaClass.simpleName}") }

    private fun <T> fileOperation(message: String, operation: () -> T): Either<ConfigError, T> =
        try {
            operation().let { Either.Right(it) }
        } catch (failure: Exception) {
            val details = (listOf(failure) + failure.suppressed).joinToString("; ") {
                it.message ?: it.javaClass.simpleName
            }
            GameFileError("$message: $details").left()
        } catch (failure: LinkageError) {
            GameFileError("$message: incompatible provider (${failure.message ?: failure.javaClass.simpleName})").left()
        }

    private fun validateProvider(jar: Path, className: String) {
        val providers = java.util.jar.JarFile(jar.toFile()).use { archive ->
            val entry = archive.getJarEntry("META-INF/services/${GameProvider::class.java.name}")
                ?: error("JAR does not register a GameProvider")
            val names = archive.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().map { it.substringBefore('#').trim() }
                    .filter { it.isNotEmpty() }.distinct().toList()
            }
            require(className in names) { "Provider $className is not registered in the JAR" }
            names.forEach { name ->
                require(archive.getJarEntry(name.replace('.', '/') + ".class") != null) {
                    "Provider $name is not included in the JAR"
                }
            }
            names
        }
        java.net.URLClassLoader(arrayOf(jar.toUri().toURL()), GameProvider::class.java.classLoader).use { loader ->
            // The runtime ServiceLoader visits all registered providers, not only the requested one.
            providers.forEach { name ->
                val provider = Class.forName(name, false, loader)
                require(provider.classLoader == loader) { "Provider must be loaded from the game JAR" }
                require(GameProvider::class.java.isAssignableFrom(provider)) { "$name is not a GameProvider" }
                require(java.lang.reflect.Modifier.isPublic(provider.modifiers) &&
                    !java.lang.reflect.Modifier.isAbstract(provider.modifiers)) { "Provider must be public and concrete" }
                provider.getConstructor().newInstance()
            }
        }
    }

    companion object {
        operator fun invoke(): Either<ConfigError, InstallationManager> = either {
            val gamesDbFile = handleGameDbFile().bind()
            Either.catch { InstallationManager(gamesDbFile) }
                .mapLeft { DbFileError("Could not open games database: ${it.message ?: it.javaClass.simpleName}") }.bind()
        }

        private fun getUserHome(): Either<UserHomeError, String> =
            Either.catch {
                val home = System.getProperty("user.home") ?: throw NullPointerException("Missing user.home")
                require(home.isNotBlank()) { "Empty user.home" }
                home
            }
                .mapLeft {
                    when (it) {
                        is SecurityException -> UserHomeError.NoPermission
                        is NullPointerException -> UserHomeError.NoHomeDirectory
                        is IllegalArgumentException -> UserHomeError.EmptyProperty
                        else -> UserHomeError("Generic error when working with home directory")
                    }
                }

        private fun handleKastleDirectory(): Either<ConfigError, Path> {
            val home = getUserHome().getOrElse { return it.left() }
            return Either.catch {
                val kastleFolder = Paths.get(home).resolve(".kastle")
                if (!Files.exists(kastleFolder)) {
                    Files.createDirectory(kastleFolder)
                }
                kastleFolder
            }.mapLeft {
                when (it) {
                    is SecurityException -> KastleDirectoryError.NoPermission
                    else -> KastleDirectoryError("Generic error when working with \$HOME/.kastle directory")
                }
            }
        }


        private fun handleGameDbFile(): Either<ConfigError, Path> {
            val kastleDir = handleKastleDirectory().getOrElse { return it.left() }
            return Either.catch {
                val gamesDbFile = kastleDir.resolve("games.db")
                if (!Files.exists(gamesDbFile)) {
                    Files.createFile(gamesDbFile)
                }
                gamesDbFile
            }.mapLeft {
                when (it) {
                    is SecurityException -> DbFileError.NoPermission
                    is IOException -> DbFileError.IOError
                    else -> DbFileError("Generic error when working with \$HOME/.kastle/games.db file")
                }
            }
        }


        private fun handleGamesFolder(kastleDir: Path): Either<ConfigError, Path> {
            return Either.catch {
                val gamesFolder = kastleDir.resolve("games")
                if (!Files.exists(gamesFolder)) {
                    Files.createDirectory(gamesFolder)
                }
                gamesFolder
            }.mapLeft {
                when (it) {
                    is SecurityException -> GamesDirectoryError.NoPermission
                    else -> GamesDirectoryError("Generic error when working with \$HOME/.kastle/games directory")
                }
            }
        }

    }
}