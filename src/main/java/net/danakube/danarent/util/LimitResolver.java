package net.danakube.danarent.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class LimitResolver {

    // 1. Quota de création pour le propriétaire
    public static int getMaxCreations(Plugin plugin, Player player) {
        if (player == null) return 0;
        if (player.hasPermission("danarent.admin") || player.hasPermission("danarent.limit.create.unlimited") || player.hasPermission("danarent.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int defaultVal = plugin != null ? plugin.getConfig().getInt("limits.creation.default", 3) : 3;
        int highest = resolvePermissionNumber(player, "danarent.limit.create.", defaultVal);
        highest = resolvePermissionNumber(player, "danarent.limit.", highest);
        return highest;
    }

    // 2. Quota de baux simultanés pour le locataire
    public static int getMaxActiveRentals(Plugin plugin, Player player) {
        if (player == null) return 0;
        if (player.hasPermission("danarent.admin") || player.hasPermission("danarent.limit.rent.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int defaultVal = plugin != null ? plugin.getConfig().getInt("limits.active-rentals.default", 1) : 1;
        return resolvePermissionNumber(player, "danarent.limit.rent.", defaultVal);
    }

    private static int resolvePermissionNumber(Player player, String prefix, int defaultVal) {
        int highest = defaultVal;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission();
            if (perm.startsWith(prefix) && pai.getValue()) {
                try {
                    int val = Integer.parseInt(perm.substring(prefix.length()));
                    if (val > highest) {
                        highest = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return highest;
    }
}
