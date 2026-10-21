package net.danakube.danarent.model;

import net.danakube.danarent.util.Cuboid;
import org.bukkit.Material;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class RentPlot {

    private int id;
    private final UUID ownerUuid;
    private final String islandId;
    private String name;
    private PlotType plotType;
    private final String world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private double priceLumens;
    private long durationSeconds;
    private Set<Material> allowedMaterials;
    private boolean isListed;
    private double pendingRevenue;
    private final long createdAt;

    public RentPlot(int id, UUID ownerUuid, String islandId, String name, PlotType plotType,
                    String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                    double priceLumens, long durationSeconds, Set<Material> allowedMaterials,
                    boolean isListed, double pendingRevenue, long createdAt) {
        this.id = id;
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        this.islandId = Objects.requireNonNull(islandId, "islandId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.plotType = Objects.requireNonNull(plotType, "plotType cannot be null");
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.priceLumens = priceLumens;
        this.durationSeconds = durationSeconds;
        this.allowedMaterials = allowedMaterials != null ? new HashSet<>(allowedMaterials) : new HashSet<>();
        this.isListed = isListed;
        this.pendingRevenue = pendingRevenue;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getIslandId() {
        return islandId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlotType getPlotType() {
        return plotType;
    }

    public void setPlotType(PlotType plotType) {
        this.plotType = plotType;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public double getPriceLumens() {
        return priceLumens;
    }

    public void setPriceLumens(double priceLumens) {
        this.priceLumens = priceLumens;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Set<Material> getAllowedMaterials() {
        return Collections.unmodifiableSet(allowedMaterials);
    }

    public void setAllowedMaterials(Set<Material> materials) {
        this.allowedMaterials = materials != null ? new HashSet<>(materials) : new HashSet<>();
    }

    public void addAllowedMaterial(Material material) {
        if (material != null) {
            this.allowedMaterials.add(material);
        }
    }

    public void removeAllowedMaterial(Material material) {
        if (material != null) {
            this.allowedMaterials.remove(material);
        }
    }

    public boolean isMaterialAllowed(Material material) {
        return allowedMaterials.contains(material);
    }

    public boolean isListed() {
        return isListed;
    }

    public void setListed(boolean listed) {
        isListed = listed;
    }

    public double getPendingRevenue() {
        return pendingRevenue;
    }

    public void setPendingRevenue(double pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }

    public void addPendingRevenue(double amount) {
        this.pendingRevenue += amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Cuboid getCuboid() {
        return new Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
