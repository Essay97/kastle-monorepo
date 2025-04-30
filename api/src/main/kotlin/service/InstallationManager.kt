package com.saggiodev.kastle.service

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
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

    private val queries: GamesQueries

    init {
        val jdbcString = "jdbc:sqlite:${gamesDbFile.absolutePathString()}"
        val driver: SqlDriver = JdbcSqliteDriver(url = jdbcString, schema = Database.Schema)

        queries = Database(driver).gamesQueries
    }

    fun installGame(name: String, gameFile: Path, className: String): Either<ConfigError, Unit> = either {
        // Insert game into database
        val gameFileName = gameFile.fileName.name
        val game = queries.getFilteredGames(name, className, gameFileName).executeAsOneOrNull()
        ensure(game == null) { GameFileError.GameAlreadyExists }
        queries.insert(gameName = name, mainClass = className, fileName = gameFileName)

        // Copy game file into games folder. Needed for ServiceLoader so that all files are in a predictable folder
        val gamesFolder = handleGamesFolder().bind()
        Files.copy(gameFile, gamesFolder.resolve(gameFileName))

    }

    fun uninstallGame(name: String): Either<ConfigError, Unit> = either {
        val game = queries.getByGameName(name).executeAsOne()
        queries.deleteByGameName(name)

        val gameFile = handleGamesFolder().bind()
            .resolve(game.fileName)
        Files.delete(gameFile)
    }

    fun getByGameName(name: String): Either<ConfigError, InstalledGames> = either {
        queries.getByGameName(name).executeAsOne()
    }

    fun getGames(): List<InstalledGames> = queries.getAll().executeAsList()

    companion object {
        operator fun invoke(): Either<ConfigError, InstallationManager> = either {
            val gamesDbFile = handleGameDbFile().bind()
            InstallationManager(gamesDbFile)
        }

        private fun getUserHome(): Either<UserHomeError, String> =
            Either.catch { System.getProperty("user.home") }
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


        private fun handleGamesFolder(): Either<ConfigError, Path> {
            val kastleDir = handleKastleDirectory().getOrElse { return it.left() }
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