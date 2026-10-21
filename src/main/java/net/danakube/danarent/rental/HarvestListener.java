package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class HarvestListener implements Listener {

    private final DanaRentPlugin plugin;

    public HarvestListener(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotAt(block.getLocation());
        if (plotOpt.isEmpty()) {
            return;
        }

        RentPlot plot = plotOpt.get();
        ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
        if (contract == null) {
            // Aucun bail actif sur cette parcelle : gestion normale SSB2/Vanilla
            return;
        }

        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(contract.getTenantUuid())) {
            // Un joueur autre que le locataire essaie de casser un bloc dans la parcelle louée
            event.setCancelled(true);
            OfflinePlayer tenantPlayer = Bukkit.getOfflinePlayer(contract.getTenantUuid());
            String tenantName = tenantPlayer.getName() != null ? tenantPlayer.getName() : contract.getTenantUuid().toString();
            String remainingTime = RentalTimerTask.formatDuration(contract.getRemainingSeconds());

            player.sendMessage(plugin.getMessageManager().getMessage("security.protected-zone", Map.of(
                    "tenant", tenantName,
                    "remaining_time", remainingTime
            )));
            return;
        }

        // Le joueur est le locataire légitime
        if (plot.getPlotType() == PlotType.FARM) {
            handleFarmHarvest(event, plot, block, player);
        } else if (plot.getPlotType() == PlotType.MINING) {
            handleMiningHarvest(event, plot, block, player);
        }
    }

    private void handleFarmHarvest(BlockBreakEvent event, RentPlot plot, Block block, Player tenant) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            event.setCancelled(true);
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            event.setCancelled(true);
            return;
        }

        Material seedMaterial = getSeedForCrop(block.getType());
        if (seedMaterial == null) {
            event.setCancelled(true);
            return;
        }

        // Bypass SSB2
        event.setCancelled(false);

        // Récolte & déduction de la graine
        ItemStack tool = tenant.getInventory().getItemInMainHand();
        Collection<ItemStack> drops;
        try {
            drops = block.getDrops(tool, tenant);
        } catch (Throwable t) {
            drops = java.util.Collections.emptyList();
        }
        boolean seedDeducted = false;

        for (ItemStack drop : drops) {
            if (!seedDeducted && drop.getType() == seedMaterial) {
                drop.setAmount(drop.getAmount() - 1);
                seedDeducted = true;
            }
            if (drop.getAmount() > 0) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        // Annulation du drop vanilla par défaut
        event.setDropItems(false);

        // Auto-replant au stade 0
        ageable.setAge(0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            block.setBlockData(ageable);
        });
    }

    private void handleMiningHarvest(BlockBreakEvent event, RentPlot plot, Block block, Player tenant) {
        if (!plot.getAllowedMaterials().contains(block.getType())) {
            event.setCancelled(true);
            tenant.sendMessage(plugin.getMessageManager().getMessage("security.unauthorized-block"));
            return;
        }

        // Bloc whitelisté : bypass SSB2 et drops normaux
        event.setCancelled(false);
    }

    public static Material getSeedForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            default -> null;
        };
    }
}
