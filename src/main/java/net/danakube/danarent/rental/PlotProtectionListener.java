package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public class PlotProtectionListener implements Listener {

    private final DanaRentPlugin plugin;

    public PlotProtectionListener(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotAt(event.getBlock().getLocation());
        if (plotOpt.isPresent()) {
            ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plotOpt.get().getId());
            if (contract != null) {
                // Interdiction formelle de poser des blocs dans une parcelle sous bail actif
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        // 1. Protection anti-piétinement des terres labourées (Farmland)
        if (event.getAction() == Action.PHYSICAL && clickedBlock.getType() == Material.FARMLAND) {
            Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotAt(clickedBlock.getLocation());
            if (plotOpt.isPresent()) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. Clic droit
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Autoriser le fertilisant (Bone Meal)
            if (event.getItem() != null && event.getItem().getType() == Material.BONE_MEAL) {
                return;
            }

            // Protection des conteneurs (Coffres, Barils, etc.)
            if (clickedBlock.getState() instanceof Container) {
                Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotAt(clickedBlock.getLocation());
                if (plotOpt.isPresent()) {
                    ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plotOpt.get().getId());
                    if (contract != null) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) {
            return;
        }

        // Zone 100% No-PvP si la victime ou l'attaquant est dans une parcelle louée
        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotAt(victim.getLocation());
        if (plotOpt.isPresent()) {
            ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plotOpt.get().getId());
            if (contract != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnderpearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        ActiveContract contract = plugin.getRentalManager().getActiveContractByTenant(player.getUniqueId());
        if (contract == null) {
            return;
        }

        RentPlot plot = plugin.getPlotManager().getPlot(contract.getPlotId()).orElse(null);
        if (plot == null) {
            return;
        }

        Cuboid bufferCuboid = plot.getCuboid().expand(1.0);
        if (event.getTo() != null && !bufferCuboid.contains(event.getTo())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("security.boundary-warning"));
        }
    }
}
