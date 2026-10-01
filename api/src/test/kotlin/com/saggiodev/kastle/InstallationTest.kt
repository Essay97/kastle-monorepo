package com.saggiodev.kastle

import com.saggiodev.kastle.error.GameFileError
import com.saggiodev.kastle.service.GameProvider
import com.saggiodev.kastle.service.InstallationManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

class InstallationTest {
    @TempDir lateinit var home: Path
    private var previousHome: String? = null
    private lateinit var manager: InstallationManager
    private val provider = "fixture.TestGame"
    private val providerBytes: ByteArray by lazy {
        val source = home.resolve("TestGame.java")
        val classes = Files.createDirectory(home.resolve("classes"))
        Files.writeString(source, """
            package fixture;
            public class TestGame implements com.saggiodev.kastle.service.GameProvider {
                public com.saggiodev.kastle.dto.GameConfiguration provideConfiguration() {
                    throw new AssertionError("Installation must not run the game");
                }
            }
        """.trimIndent())
        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
        assertEquals(0, compiler.run(null, null, null, "-classpath",
            Path.of(GameProvider::class.java.protectionDomain.codeSource.location.toURI()).toString(),
            "-d", classes.toString(), source.toString()))
        Files.readAllBytes(classes.resolve("fixture/TestGame.class"))
    }
    private val games get() = home.resolve(".kastle/games")

    @BeforeEach fun setup() {
        previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        manager = InstallationManager().success()
    }
    @AfterEach fun restore() {
        if (previousHome == null) System.clearProperty("user.home")
        else System.setProperty("user.home", previousHome!!)
    }
    private fun jar(name: String = "game.jar", registered: Boolean = true, extraProvider: String = ""): Path {
        val path = home.resolve(name)
        JarOutputStream(Files.newOutputStream(path)).use { jar ->
            val classPath = provider.replace('.', '/') + ".class"
            jar.putNextEntry(JarEntry(classPath))
            jar.write(providerBytes)
            jar.closeEntry()
            if (registered) {
                jar.putNextEntry(JarEntry("META-INF/services/${GameProvider::class.java.name}"))
                jar.write((provider + "\n" + extraProvider).toByteArray())
                jar.closeEntry()
            }
        }
        return path
    }
    @Test fun `install and uninstall keep files and records together`() {
        val source = jar()
        manager.installGame("Game", source, provider).success()
        assertContentEquals(Files.readAllBytes(source), Files.readAllBytes(games.resolve("game.jar")))
        assertEquals(provider, manager.getByGameName("Game").success().mainClass)
        manager.uninstallGame("Game").success()
        assertTrue(manager.getGames().isEmpty())
        assertFalse(Files.exists(games.resolve("game.jar")))
        assertTrue(Files.exists(source))
    }
    @Test fun `invalid jar and absent provider leave no installation`() {
        val invalid = home.resolve("invalid.jar")
        Files.writeString(invalid, "not a jar")
        manager.installGame("Invalid", invalid, provider).failure()
        manager.installGame("No provider", jar(registered = false), provider).failure()
        manager.installGame("Wrong provider", jar(), "missing.Provider").failure()
        manager.installGame("Missing", home.resolve("missing.jar"), provider).failure()
        assertTrue(manager.getGames().isEmpty())
    }
    @Test fun `invalid additional service provider is rejected`() {
        manager.installGame("Game", jar(extraProvider = "missing.Provider"), provider).failure()
        assertTrue(manager.getGames().isEmpty())
    }
    @Test fun `unloadable provider is rejected without registering game`() {
        val invalid = home.resolve("broken.jar")
        JarOutputStream(Files.newOutputStream(invalid)).use { jar ->
            jar.putNextEntry(JarEntry("fixture/Broken.class"))
            jar.write(byteArrayOf(1, 2, 3))
            jar.closeEntry()
            jar.putNextEntry(JarEntry("META-INF/services/${GameProvider::class.java.name}"))
            jar.write("fixture.Broken".toByteArray())
            jar.closeEntry()
        }
        manager.installGame("Broken", invalid, "fixture.Broken").failure()
        assertTrue(manager.getGames().isEmpty())
        assertFalse(Files.exists(games.resolve("broken.jar")))
    }
    @Test fun `blank names are rejected before copying`() {
        manager.installGame("  ", jar(), provider).failure()
        manager.installGame("Game", jar(), "  ").failure()
        assertTrue(manager.getGames().isEmpty())
    }
    @Test fun `nonexistent uninstall and lookup return domain errors`() {
        assertEquals(GameFileError.NonExistentGame, manager.uninstallGame("Missing").failure())
        assertEquals(GameFileError.NonExistentGame, manager.getByGameName("Missing").failure())
    }
    @Test fun `copy failure leaves database unchanged and preserves existing file`() {
        val source = jar()
        Files.createDirectories(games)
        val target = games.resolve("game.jar")
        Files.writeString(target, "existing")
        manager.installGame("Game", source, provider).failure()
        assertTrue(manager.getGames().isEmpty())
        assertEquals("existing", Files.readString(target))
    }
    @Test fun `duplicates reject any conflicting identity without modifying original`() {
        val source = jar()
        manager.installGame("Game", source, provider).success()
        assertEquals(GameFileError.GameAlreadyExists, manager.installGame("Game", jar("other.jar"), "other.Provider").failure())
        assertEquals(GameFileError.GameAlreadyExists, manager.installGame("Other", jar("other.jar"), provider).failure())
        assertEquals(GameFileError.GameAlreadyExists, manager.installGame("Other", source, "other.Provider").failure())
        assertEquals(1, manager.getGames().size)
        assertContentEquals(Files.readAllBytes(source), Files.readAllBytes(games.resolve("game.jar")))
    }
    @Test fun `failed file removal preserves database record`() {
        manager.installGame("Game", jar(), provider).success()
        val target = games.resolve("game.jar")
        Files.delete(target)
        Files.createDirectory(target)
        Files.writeString(target.resolve("child"), "obstruction")
        manager.uninstallGame("Game").failure()
        assertEquals("Game", manager.getByGameName("Game").success().gameName)
        assertTrue(Files.exists(target.resolve("child")))
    }
    private fun sql(statement: String) {
        java.sql.DriverManager.getConnection("jdbc:sqlite:${home.resolve(".kastle/games.db")}").use { connection ->
            connection.createStatement().use { it.execute(statement) }
        }
    }
    @Test fun `database insert failure removes copied jar`() {
        sql("CREATE TRIGGER reject_install BEFORE INSERT ON installedGames BEGIN SELECT RAISE(ABORT, 'rejected'); END")
        manager.installGame("Game", jar(), provider).failure()
        assertTrue(manager.getGames().isEmpty())
        assertFalse(Files.exists(games.resolve("game.jar")))
        Files.list(home.resolve(".kastle")).use { paths ->
            assertFalse(paths.anyMatch { it.fileName.toString().startsWith("install-") })
        }
    }
    @Test fun `database delete failure preserves jar and record`() {
        val source = jar()
        manager.installGame("Game", source, provider).success()
        sql("CREATE TRIGGER reject_uninstall BEFORE DELETE ON installedGames BEGIN SELECT RAISE(ABORT, 'rejected'); END")
        manager.uninstallGame("Game").failure()
        assertEquals("Game", manager.getByGameName("Game").success().gameName)
        assertContentEquals(Files.readAllBytes(source), Files.readAllBytes(games.resolve("game.jar")))
    }
    @Test fun `database commit failure removes newly installed jar`() {
        val source = jar()
        java.sql.DriverManager.getConnection("jdbc:sqlite:${home.resolve(".kastle/games.db")}").use { reader ->
            reader.autoCommit = false
            reader.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM installedGames").use { assertFalse(it.next()) }
            }
            manager.installGame("Game", source, provider).failure()
            reader.rollback()
        }
        assertTrue(manager.getGames().isEmpty())
        assertFalse(Files.exists(games.resolve("game.jar")))
        manager.installGame("Game", source, provider).success()
    }
    @Test fun `database commit failure restores removed jar`() {
        val source = jar()
        manager.installGame("Game", source, provider).success()
        java.sql.DriverManager.getConnection("jdbc:sqlite:${home.resolve(".kastle/games.db")}").use { reader ->
            reader.autoCommit = false
            reader.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM installedGames").use { assertTrue(it.next()) }
            }
            // A real SQLite read transaction prevents the writer from committing after file removal.
            manager.uninstallGame("Game").failure()
            reader.rollback()
        }
        assertEquals("Game", manager.getByGameName("Game").success().gameName)
        assertContentEquals(Files.readAllBytes(source), Files.readAllBytes(games.resolve("game.jar")))
        Files.list(home.resolve(".kastle")).use { paths ->
            assertFalse(paths.anyMatch { it.fileName.toString().startsWith("uninstall-") })
        }
        manager.uninstallGame("Game").success()
    }
    @Test fun `corrupt database initialization returns domain error`() {
        val otherHome = home.resolve("other")
        Files.createDirectories(otherHome.resolve(".kastle"))
        Files.writeString(otherHome.resolve(".kastle/games.db"), "not a database")
        System.setProperty("user.home", otherHome.toString())
        InstallationManager().failure()
    }
    @Test fun `missing installed file can be uninstalled to repair stale record`() {
        manager.installGame("Game", jar(), provider).success()
        Files.delete(games.resolve("game.jar"))
        manager.uninstallGame("Game").success()
        assertTrue(manager.getGames().isEmpty())
    }
}
