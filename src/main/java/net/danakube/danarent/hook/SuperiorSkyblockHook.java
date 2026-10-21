package net.danakube.danarent.hook;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.PlayerRole;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockHook implements SkyblockHook {

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null;
    }

    @Override
    public boolean isHooked() {
        return isAvailable();
    }

    @Override
    public boolean hasIsland(Player player) {
        if (!isAvailable() || player == null) return false;
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        return sp != null && sp.getIsland() != null;
    }

    @Override
    public String getIslandId(Player player) {
        if (!isAvailable() || player == null) return null;
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        if (sp == null || sp.getIsland() == null) return null;
        return sp.getIsland().getUniqueId().toString();
    }

    @Override
    public boolean isIslandAdminOrOwner(Player player, String islandId) {
        if (!isAvailable() || player == null) return false;
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        if (sp == null || sp.getIsland() == null) return false;
        Island island = sp.getIsland();
        if (!island.getUniqueId().toString().equals(islandId)) return false;

        if (island.getOwner() != null && island.getOwner().getUniqueId().equals(player.getUniqueId())) {
            return true;
        }

        PlayerRole role = sp.getPlayerRole();
        return role != null && (role.isLastRole() || role.getWeight() >= 50);
    }

    @Override
    public boolean isCuboidFullyInsideIsland(Cuboid cuboid, String islandId) {
        if (!isAvailable() || cuboid == null) return false;
        Island island = SuperiorSkyblockAPI.getGrid().getIslandByUUID(java.util.UUID.fromString(islandId));
        if (island == null) return false;

        for (Location corner : cuboid.getEightCorners()) {
            if (!island.isInsideRange(corner)) {
                return false;
            }
        }
        return true;
    }
}
