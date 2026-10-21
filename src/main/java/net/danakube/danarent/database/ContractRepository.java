package net.danakube.danarent.database;

import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.TransactionLog;
import net.danakube.danarent.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ContractRepository {

    private final DatabaseConnection databaseConnection;

    public ContractRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public CompletableFuture<Integer> save(ActiveContract contract) {
        return CompletableFuture.supplyAsync(() -> {
            if (contract.getId() <= 0) {
                return insertContract(contract);
            } else {
                updateContract(contract);
                return contract.getId();
            }
        });
    }

    private int insertContract(ActiveContract contract) {
        String sql = """
            INSERT INTO active_contracts (
                plot_id, tenant_uuid, start_time, end_time,
                initial_price, escrow_balance, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, contract.getPlotId());
            ps.setString(2, contract.getTenantUuid().toString());
            ps.setLong(3, contract.getStartTime());
            ps.setLong(4, contract.getEndTime());
            ps.setDouble(5, contract.getInitialPrice());
            ps.setDouble(6, contract.getEscrowBalance());
            ps.setString(7, contract.getStatus().name());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    contract.setId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to retrieve generated key for active_contracts");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting ActiveContract into database", e);
        }
    }

    private void updateContract(ActiveContract contract) {
        String sql = """
            UPDATE active_contracts SET
                end_time = ?, escrow_balance = ?, status = ?
            WHERE id = ?;
        """;
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, contract.getEndTime());
            ps.setDouble(2, contract.getEscrowBalance());
            ps.setString(3, contract.getStatus().name());
            ps.setInt(4, contract.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating ActiveContract into database", e);
        }
    }

    public CompletableFuture<Optional<ActiveContract>> findById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM active_contracts WHERE id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToContract(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding ActiveContract by id", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<ActiveContract>> findActiveByPlotId(int plotId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM active_contracts WHERE plot_id = ? AND status = 'ACTIVE' LIMIT 1;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, plotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToContract(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding active contract for plot", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<List<ActiveContract>> findActiveByTenant(UUID tenantUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ActiveContract> list = new ArrayList<>();
            String sql = "SELECT * FROM active_contracts WHERE tenant_uuid = ? AND status = 'ACTIVE';";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tenantUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToContract(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding active contracts for tenant", e);
            }
            return list;
        });
    }

    public CompletableFuture<List<ActiveContract>> findAllActive() {
        return CompletableFuture.supplyAsync(() -> {
            List<ActiveContract> list = new ArrayList<>();
            String sql = "SELECT * FROM active_contracts WHERE status = 'ACTIVE';";
            try (Connection conn = databaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToContract(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding all active contracts", e);
            }
            return list;
        });
    }

    public CompletableFuture<Void> updateEscrowBalance(int contractId, double newBalance) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE active_contracts SET escrow_balance = ? WHERE id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, newBalance);
                ps.setInt(2, contractId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error updating escrow balance", e);
            }
        });
    }

    public CompletableFuture<Void> updateStatus(int contractId, ContractStatus status) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE active_contracts SET status = ? WHERE id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setInt(2, contractId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error updating contract status", e);
            }
        });
    }

    public CompletableFuture<Integer> logTransaction(TransactionLog log) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO transaction_logs (
                    contract_id, source_uuid, target_uuid, amount, type, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?);
            """;
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, log.getContractId());
                ps.setString(2, log.getSourceUuid().toString());
                ps.setString(3, log.getTargetUuid().toString());
                ps.setDouble(4, log.getAmount());
                ps.setString(5, log.getType().name());
                ps.setLong(6, log.getTimestamp());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        log.setId(id);
                        return id;
                    }
                }
                throw new SQLException("Failed to retrieve generated key for transaction_logs");
            } catch (SQLException e) {
                throw new RuntimeException("Error inserting TransactionLog", e);
            }
        });
    }

    public CompletableFuture<List<TransactionLog>> getTransactionLogs(int contractId) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransactionLog> list = new ArrayList<>();
            String sql = "SELECT * FROM transaction_logs WHERE contract_id = ? ORDER BY timestamp ASC;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, contractId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new TransactionLog(
                                rs.getInt("id"),
                                rs.getInt("contract_id"),
                                UUID.fromString(rs.getString("source_uuid")),
                                UUID.fromString(rs.getString("target_uuid")),
                                rs.getDouble("amount"),
                                TransactionType.valueOf(rs.getString("type")),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding TransactionLogs by contractId", e);
            }
            return list;
        });
    }

    private ActiveContract mapResultSetToContract(ResultSet rs) throws SQLException {
        return new ActiveContract(
                rs.getInt("id"),
                rs.getInt("plot_id"),
                UUID.fromString(rs.getString("tenant_uuid")),
                rs.getLong("start_time"),
                rs.getLong("end_time"),
                rs.getDouble("initial_price"),
                rs.getDouble("escrow_balance"),
                ContractStatus.valueOf(rs.getString("status"))
        );
    }
}
