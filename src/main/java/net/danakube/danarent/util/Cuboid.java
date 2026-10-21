package net.danakube.danarent.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * Représente une boîte englobante 3D (AABB) alignée sur les axes.
 */
public class Cuboid {

    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Cuboid(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public Cuboid(Location loc1, Location loc2) {
        Objects.requireNonNull(loc1, "loc1 cannot be null");
        Objects.requireNonNull(loc2, "loc2 cannot be null");
        if (loc1.getWorld() == null || loc2.getWorld() == null) {
            throw new IllegalArgumentException("World of locations cannot be null");
        }
        if (!loc1.getWorld().getName().equals(loc2.getWorld().getName())) {
            throw new IllegalArgumentException("Locations must be in the same world");
        }
        this.worldName = loc1.getWorld().getName();
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
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

    public long getVolume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(String world, int x, int y, int z) {
        if (!this.worldName.equalsIgnoreCase(world)) {
            return false;
        }
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        return contains(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean overlaps(Cuboid other) {
        if (other == null || !this.worldName.equalsIgnoreCase(other.worldName)) {
            return false;
        }
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minY <= other.maxY && this.maxY >= other.minY
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public Location[] getEightCorners() {
        World w = getWorld();
        return new Location[]{
                new Location(w, minX, minY, minZ),
                new Location(w, minX, minY, maxZ),
                new Location(w, minX, maxY, minZ),
                new Location(w, minX, maxY, maxZ),
                new Location(w, maxX, minY, minZ),
                new Location(w, maxX, minY, maxZ),
                new Location(w, maxX, maxY, minZ),
                new Location(w, maxX, maxY, maxZ)
        };
    }

    public Location getCenter() {
        World w = getWorld();
        return new Location(w, (minX + maxX) / 2.0 + 0.5, minY + 0.5, (minZ + maxZ) / 2.0 + 0.5);
    }

    public Cuboid expand(int margin) {
        return new Cuboid(worldName,
                minX - margin, minY - margin, minZ - margin,
                maxX + margin, maxY + margin, maxZ + margin);
    }

    public Cuboid expand(double margin) {
        int m = (int) Math.ceil(margin);
        return expand(m);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cuboid cuboid)) return false;
        return minX == cuboid.minX && minY == cuboid.minY && minZ == cuboid.minZ &&
                maxX == cuboid.maxX && maxY == cuboid.maxY && maxZ == cuboid.maxZ &&
                worldName.equalsIgnoreCase(cuboid.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName.toLowerCase(), minX, minY, minZ, maxX, maxY, maxZ);
    }
}
