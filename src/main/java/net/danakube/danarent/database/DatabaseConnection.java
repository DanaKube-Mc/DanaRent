package net.danakube.danarent.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private final HikariDataSource dataSource;

    public DatabaseConnection(String jdbcUrl, int maxPoolSize, long connectionTimeout) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName("DanaRent-HikariPool");

        // Optimisations SQLite pour le multithreading
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);
    }

    public DatabaseConnection(File dataFolder, String databaseFileName, int maxPoolSize, long connectionTimeout) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, databaseFileName);
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName("DanaRent-HikariPool");

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);
    }

    public void initDatabase() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rent_plots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid VARCHAR(36) NOT NULL,
                    island_id VARCHAR(64) NOT NULL,
                    name VARCHAR(32) NOT NULL,
                    plot_type VARCHAR(16) NOT NULL DEFAULT 'FARM',
                    world VARCHAR(64) NOT NULL,
                    min_x INTEGER NOT NULL,
                    min_y INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    max_y INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    price_lumens REAL NOT NULL DEFAULT 0.0,
                    duration_seconds INTEGER NOT NULL DEFAULT 3600,
                    allowed_materials TEXT NOT NULL DEFAULT '[]',
                    is_listed INTEGER NOT NULL DEFAULT 0,
                    pending_revenue REAL NOT NULL DEFAULT 0.0,
                    created_at INTEGER NOT NULL
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_contracts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plot_id INTEGER NOT NULL,
                    tenant_uuid VARCHAR(36) NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    initial_price REAL NOT NULL,
                    escrow_balance REAL NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                    FOREIGN KEY(plot_id) REFERENCES rent_plots(id) ON DELETE CASCADE
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transaction_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    contract_id INTEGER NOT NULL,
                    source_uuid VARCHAR(36) NOT NULL,
                    target_uuid VARCHAR(36) NOT NULL,
                    amount REAL NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_plots_island ON rent_plots(island_id);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_plots_owner ON rent_plots(owner_uuid);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_contracts_tenant ON active_contracts(tenant_uuid);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_contracts_status ON active_contracts(status);");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
