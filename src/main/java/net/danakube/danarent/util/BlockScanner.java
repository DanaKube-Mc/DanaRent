package net.danakube.danarent.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockScanner {

    public static Set<Material> scanPlotMaterials(Cuboid cuboid) {
        Set<Material> materials = new HashSet<>();
        if (cuboid == null) {
            return materials;
        }

        World world = cuboid.getWorld();
        if (world == null) {
            return materials;
        }

        for (int x = cuboid.getMinX(); x <= cuboid.getMaxX(); x++) {
            for (int y = cuboid.getMinY(); y <= cuboid.getMaxY(); y++) {
                for (int z = cuboid.getMinZ(); z <= cuboid.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    // Ignorer l'air et les blocs structurels indestructibles
                    if (!type.isAir() && type != Material.BEDROCK && type != Material.BARRIER) {
                        materials.add(type);
                    }
                }
            }
        }

        return materials;
    }
}
