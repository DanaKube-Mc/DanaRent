package net.danakube.danarent.hook;

import net.danakube.danarent.util.Cuboid;
import org.bukkit.entity.Player;

public interface SkyblockHook {

    boolean hasIsland(Player player);

    String getIslandId(Player player);

    boolean isIslandAdminOrOwner(Player player, String islandId);

    boolean isCuboidFullyInsideIsland(Cuboid cuboid, String islandId);

    default boolean isHooked() {
        return false;
    }
}
