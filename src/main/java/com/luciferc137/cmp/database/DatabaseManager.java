package com.luciferc137.cmp.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database connection manager.
 * Singleton pattern to ensure a single connection instance.
 */
public class DatabaseManager {

    private static final String APP_FOLDER_NAME = ".cmp";
    private static final String DATABASE_FILE_NAME = "library.db";
    private static final int CURRENT_SCHEMA_VERSION = 2;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeConnection();
        initializeSchema();
    }

    /**
     * Returns the unique instance of DatabaseManager.
     *
     * @return The DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes the database connection.
     */
    private void initializeConnection() {
        try {
            String dbPath = getDatabasePath().toString();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(true);

            // Enable foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            throw new RuntimeException("Unable to initialize database", e);
        }
    }

    /**
     * Returns the path to the database file.
     *
     * @return The database file path
     */
    private Path getDatabasePath() {
        String userHome = System.getProperty("user.home");
        Path appFolder = Paths.get(userHome, APP_FOLDER_NAME);

        // Create folder if needed
        if (!appFolder.toFile().exists()) {
            appFolder.toFile().mkdirs();
        }

        return appFolder.resolve(DATABASE_FILE_NAME);
    }

    /**
     * Initializes the database schema.
     */
    private void initializeSchema() {
        try {
            int currentVersion = getSchemaVersion();

            if (currentVersion == 0) {
                // Fresh install
                createTables();
                setSchemaVersion(CURRENT_SCHEMA_VERSION);
            } else if (currentVersion < CURRENT_SCHEMA_VERSION) {
                // Run migrations
                migrateSchema(currentVersion);
                setSchemaVersion(CURRENT_SCHEMA_VERSION);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing schema: " + e.getMessage());
            throw new RuntimeException("Unable to initialize database schema", e);
        }
    }

    /**
     * Runs schema migrations from oldVersion to CURRENT_SCHEMA_VERSION.
     */
    private void migrateSchema(int oldVersion) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            if (oldVersion < 2) {
                // Migration to version 2: Add rating column to music table
                stmt.execute("ALTER TABLE music ADD COLUMN rating INTEGER DEFAULT 0");
                System.out.println("Migration to schema version 2 completed: Added rating column.");
            }
            // Future migrations go here
        }
    }

    /**
     * Gets the current schema version.
     *
     * @return The schema version
     */
    private int getSchemaVersion() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("PRAGMA user_version");
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Sets the schema version.
     *
     * @param version The new version
     */
    private void setSchemaVersion(int version) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version);
        }
    }

    /**
     * Creates all database tables.
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Music table - stores audio file information
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS music (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    path TEXT NOT NULL UNIQUE,
                    title TEXT,
                    artist TEXT,
                    album TEXT,
                    duration INTEGER DEFAULT 0,
                    hash TEXT NOT NULL,
                    rating INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Index for fast hash lookup
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_music_hash ON music(hash)");

            // Index for artist/album search
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_music_artist ON music(artist)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_music_album ON music(album)");

            // Playlist table - stores playlists
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS playlist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Playlist_music table - many-to-many relationship between playlists and music
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS playlist_music (
                    playlist_id INTEGER NOT NULL,
                    music_id INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (playlist_id, music_id),
                    FOREIGN KEY (playlist_id) REFERENCES playlist(id) ON DELETE CASCADE,
                    FOREIGN KEY (music_id) REFERENCES music(id) ON DELETE CASCADE
                )
            """);

            // Index for ordering music in playlist
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_playlist_music_position ON playlist_music(playlist_id, position)");

            // Tag table - stores tags/labels
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tag (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    color TEXT DEFAULT '#808080',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Music_tag table - many-to-many relationship between music and tags
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS music_tag (
                    music_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (music_id, tag_id),
                    FOREIGN KEY (music_id) REFERENCES music(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
                )
            """);

            // Sync_log table - synchronization log
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sync_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    folder_path TEXT NOT NULL,
                    files_added INTEGER DEFAULT 0,
                    files_updated INTEGER DEFAULT 0,
                    files_removed INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'completed'
                )
            """);

            System.out.println("Database tables created successfully.");
        }
    }

    /**
     * Returns the database connection.
     *
     * @return The SQL connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Starts a transaction.
     */
    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    /**
     * Commits a transaction.
     */
    public void commit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    /**
     * Rolls back a transaction.
     */
    public void rollback() throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }
}

