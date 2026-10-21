package net.danakube.danarent.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Material;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlotRepository {

    private final DatabaseConnection databaseConnection;
    private final Gson gson;
    private final Type materialSetType;

    public PlotRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.gson = new Gson();
        this.materialSetType = new TypeToken<Set<String>>() {}.getType();
    }

    public CompletableFuture<Integer> save(RentPlot plot) {
        return CompletableFuture.supplyAsync(() -> {
            if (plot.getId() <= 0) {
                return insertPlot(plot);
            } else {
                updatePlot(plot);
                return plot.getId();
            }
        });
    }

    private int insertPlot(RentPlot plot) {
        String sql = """
            INSERT INTO rent_plots (
                owner_uuid, island_id, name, plot_type, world,
                min_x, min_y, min_z, max_x, max_y, max_z,
                price_lumens, duration_seconds, allowed_materials,
                is_listed, pending_revenue, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindPlotParams(ps, plot);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    plot.setId(id);
                    return id;
                }
            }
            throw new SQLException("Failed to retrieve generated key for rent_plots");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting RentPlot into database", e);
        }
    }

    private void updatePlot(RentPlot plot) {
        String sql = """
            UPDATE rent_plots SET
                name = ?, plot_type = ?,
                price_lumens = ?, duration_seconds = ?,
                allowed_materials = ?, is_listed = ?, pending_revenue = ?
            WHERE id = ?;
        """;
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, plot.getName());
            ps.setString(2, plot.getPlotType().name());
            ps.setDouble(3, plot.getPriceLumens());
            ps.setLong(4, plot.getDurationSeconds());
            ps.setString(5, serializeMaterials(plot.getAllowedMaterials()));
            ps.setInt(6, plot.isListed() ? 1 : 0);
            ps.setDouble(7, plot.getPendingRevenue());
            ps.setInt(8, plot.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating RentPlot into database", e);
        }
    }

    private void bindPlotParams(PreparedStatement ps, RentPlot plot) throws SQLException {
        ps.setString(1, plot.getOwnerUuid().toString());
        ps.setString(2, plot.getIslandId());
        ps.setString(3, plot.getName());
        ps.setString(4, plot.getPlotType().name());
        ps.setString(5, plot.getWorld());
        ps.setInt(6, plot.getMinX());
        ps.setInt(7, plot.getMinY());
        ps.setInt(8, plot.getMinZ());
        ps.setInt(9, plot.getMaxX());
        ps.setInt(10, plot.getMaxY());
        ps.setInt(11, plot.getMaxZ());
        ps.setDouble(12, plot.getPriceLumens());
        ps.setLong(13, plot.getDurationSeconds());
        ps.setString(14, serializeMaterials(plot.getAllowedMaterials()));
        ps.setInt(15, plot.isListed() ? 1 : 0);
        ps.setDouble(16, plot.getPendingRevenue());
        ps.setLong(17, plot.getCreatedAt());
    }

    public CompletableFuture<Optional<RentPlot>> findById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM rent_plots WHERE id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToPlot(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding RentPlot by id", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<List<RentPlot>> findByIslandId(String islandId) {
        return CompletableFuture.supplyAsync(() -> {
            List<RentPlot> list = new ArrayList<>();
            String sql = "SELECT * FROM rent_plots WHERE island_id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, islandId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToPlot(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding RentPlots by islandId", e);
            }
            return list;
        });
    }

    public CompletableFuture<List<RentPlot>> findByOwner(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<RentPlot> list = new ArrayList<>();
            String sql = "SELECT * FROM rent_plots WHERE owner_uuid = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToPlot(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding RentPlots by ownerUuid", e);
            }
            return list;
        });
    }

    public CompletableFuture<List<RentPlot>> findListedPlots() {
        return CompletableFuture.supplyAsync(() -> {
            List<RentPlot> list = new ArrayList<>();
            String sql = "SELECT * FROM rent_plots WHERE is_listed = 1;";
            try (Connection conn = databaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToPlot(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding listed RentPlots", e);
            }
            return list;
        });
    }

    public CompletableFuture<Void> delete(int id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM rent_plots WHERE id = ?;";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error deleting RentPlot", e);
            }
        });
    }

    private RentPlot mapResultSetToPlot(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String islandId = rs.getString("island_id");
        String name = rs.getString("name");
        PlotType plotType = PlotType.valueOf(rs.getString("plot_type"));
        String world = rs.getString("world");
        int minX = rs.getInt("min_x");
        int minY = rs.getInt("min_y");
        int minZ = rs.getInt("min_z");
        int maxX = rs.getInt("max_x");
        int maxY = rs.getInt("max_y");
        int maxZ = rs.getInt("max_z");
        double price = rs.getDouble("price_lumens");
        long duration = rs.getLong("duration_seconds");
        Set<Material> allowedMaterials = deserializeMaterials(rs.getString("allowed_materials"));
        boolean isListed = rs.getInt("is_listed") == 1;
        double pendingRevenue = rs.getDouble("pending_revenue");
        long createdAt = rs.getLong("created_at");

        return new RentPlot(
                id, ownerUuid, islandId, name, plotType, world,
                minX, minY, minZ, maxX, maxY, maxZ,
                price, duration, allowedMaterials, isListed, pendingRevenue, createdAt
        );
    }

    private String serializeMaterials(Set<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return "[]";
        }
        Set<String> names = new HashSet<>();
        for (Material m : materials) {
            if (m != null) names.add(m.name());
        }
        return gson.toJson(names);
    }

    private Set<Material> deserializeMaterials(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return new HashSet<>();
        }
        try {
            Set<String> names = gson.fromJson(json, materialSetType);
            Set<Material> materials = new HashSet<>();
            if (names != null) {
                for (String name : names) {
                    try {
                        materials.add(Material.valueOf(name));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return materials;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
}
