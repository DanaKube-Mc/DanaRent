package net.danakube.danarent.plot;

import net.danakube.danarent.config.ConfigManager;
import net.danakube.danarent.hook.SkyblockHook;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Map;

public class PlotValidator {

    private final PlotManager plotManager;
    private final SkyblockHook skyblockHook;
    private final ConfigManager configManager;
    private final org.bukkit.plugin.Plugin plugin;

    public PlotValidator(PlotManager plotManager, SkyblockHook skyblockHook, ConfigManager configManager) {
        this(plotManager, skyblockHook, configManager, null);
    }

    public PlotValidator(PlotManager plotManager, SkyblockHook skyblockHook, ConfigManager configManager, org.bukkit.plugin.Plugin plugin) {
        this.plotManager = plotManager;
        this.skyblockHook = skyblockHook;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    public ValidationResult validate(Player player, String name, Cuboid cuboid) {
        if (!skyblockHook.hasIsland(player)) {
            return ValidationResult.error("plots.error-no-island");
        }

        String islandId = skyblockHook.getIslandId(player);
        if (!skyblockHook.isIslandAdminOrOwner(player, islandId)) {
            return ValidationResult.error("plots.error-not-owner");
        }

        if (!skyblockHook.isCuboidFullyInsideIsland(cuboid, islandId)) {
            return ValidationResult.error("plots.error-outside-island");
        }

        if (plotManager.isOverlapping(cuboid)) {
            return ValidationResult.error("plots.error-overlapping");
        }

        int maxAllowed = getMaxPlotsAllowed(player);
        int currentCount = plotManager.getPlotCountForIsland(islandId);
        if (currentCount >= maxAllowed) {
            return ValidationResult.error("limits.creation-reached", Map.of(
                    "current", String.valueOf(currentCount),
                    "max", String.valueOf(maxAllowed)
            ));
        }

        return ValidationResult.success();
    }

    public int getMaxPlotsAllowed(Player player) {
        if (player.hasPermission("danarent.admin") || player.hasPermission("danarent.limit.create.unlimited") || player.hasPermission("danarent.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int defaultMax = configManager != null ? configManager.getDefaultCreationLimit() : 3;
        int max = defaultMax;

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String name = perm.getPermission();
            if (perm.getValue()) {
                if (name.startsWith("danarent.limit.create.")) {
                    try {
                        int limit = Integer.parseInt(name.substring("danarent.limit.create.".length()));
                        if (limit > max) max = limit;
                    } catch (NumberFormatException ignored) {}
                } else if (name.startsWith("danarent.limit.")) {
                    try {
                        int limit = Integer.parseInt(name.substring("danarent.limit.".length()));
                        if (limit > max) max = limit;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return max;
    }
}
