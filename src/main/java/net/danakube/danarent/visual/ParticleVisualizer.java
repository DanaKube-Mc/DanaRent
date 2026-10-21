package net.danakube.danarent.visual;

import net.danakube.danarent.util.Cuboid;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ParticleVisualizer {

    public void drawCuboidOutline(Player player, Cuboid cuboid, Color color) {
        if (player == null || cuboid == null || !player.isOnline()) {
            return;
        }

        World playerWorld = player.getWorld();
        if (!playerWorld.getName().equalsIgnoreCase(cuboid.getWorldName())) {
            return;
        }

        Particle.DustOptions dust = new Particle.DustOptions(color != null ? color : Color.YELLOW, 1.0f);
        double step = 0.5;

        int minX = cuboid.getMinX();
        int maxX = cuboid.getMaxX() + 1;
        int minY = cuboid.getMinY();
        int maxY = cuboid.getMaxY() + 1;
        int minZ = cuboid.getMinZ();
        int maxZ = cuboid.getMaxZ() + 1;

        // Arêtes horizontales selon l'axe X
        for (double x = minX; x <= maxX; x += step) {
            spawnParticle(player, playerWorld, dust, x, minY, minZ);
            spawnParticle(player, playerWorld, dust, x, maxY, minZ);
            spawnParticle(player, playerWorld, dust, x, minY, maxZ);
            spawnParticle(player, playerWorld, dust, x, maxY, maxZ);
        }

        // Arêtes horizontales selon l'axe Z
        for (double z = minZ; z <= maxZ; z += step) {
            spawnParticle(player, playerWorld, dust, minX, minY, z);
            spawnParticle(player, playerWorld, dust, maxX, minY, z);
            spawnParticle(player, playerWorld, dust, minX, maxY, z);
            spawnParticle(player, playerWorld, dust, maxX, maxY, z);
        }

        // Arêtes verticales selon l'axe Y
        for (double y = minY; y <= maxY; y += step) {
            spawnParticle(player, playerWorld, dust, minX, y, minZ);
            spawnParticle(player, playerWorld, dust, maxX, y, minZ);
            spawnParticle(player, playerWorld, dust, minX, y, maxZ);
            spawnParticle(player, playerWorld, dust, maxX, y, maxZ);
        }
    }

    private void spawnParticle(Player player, World world, Particle.DustOptions dust, double x, double y, double z) {
        player.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }
}
