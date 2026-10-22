package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfinementListener implements Listener {

    private final DanaRentPlugin plugin;
    private final Map<UUID, Long> lastWarningTimes = new ConcurrentHashMap<>();

    public ConfinementListener(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // 1. Filtrer 95% des événements (rotations de tête et micro-pas)
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 2. Vérification en RAM du locataire
        Set<ActiveContract> contracts = plugin.getRentalManager().getActiveContractsByTenant(uuid);
        if (contracts.isEmpty()) {
            return;
        }

        // Cas 1 : Dans la zone normale d'un de ses champs
        for (ActiveContract contract : contracts) {
            RentPlot plot = plugin.getPlotManager().getPlot(contract.getPlotId()).orElse(null);
            if (plot != null && plot.getCuboid().contains(to)) {
                return;
            }
        }

        // Cas 2 : Dans la zone tampon (+1 bloc) d'un de ses champs -> Avertissement visuel doux
        for (ActiveContract contract : contracts) {
            RentPlot plot = plugin.getPlotManager().getPlot(contract.getPlotId()).orElse(null);
            if (plot != null && plot.getCuboid().expand(1.0).contains(to)) {
                spawnWarningParticles(player, to);
                return;
            }
        }

        // Cas 3 : Dépassement de la zone tampon -> Répulsion physique vers le centre du champ le plus proche
        RentPlot nearestPlot = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (ActiveContract contract : contracts) {
            RentPlot plot = plugin.getPlotManager().getPlot(contract.getPlotId()).orElse(null);
            if (plot != null && plot.getWorld().equals(to.getWorld().getName())) {
                double dist = plot.getCuboid().getCenter().distanceSquared(to);
                if (dist < nearestDistanceSq) {
                    nearestDistanceSq = dist;
                    nearestPlot = plot;
                }
            }
        }

        if (nearestPlot != null) {
            Cuboid coreCuboid = nearestPlot.getCuboid();
            Vector direction = coreCuboid.getCenter().toVector().subtract(to.toVector()).normalize();
            direction.setY(0.2); // Léger saut pour éviter de frotter au sol
            player.setVelocity(direction.multiply(0.8));

            // Message d'alerte avec cooldown de 2 secondes
            long now = System.currentTimeMillis();
            Long lastWarn = lastWarningTimes.get(uuid);
            if (lastWarn == null || now - lastWarn >= 2000L) {
                lastWarningTimes.put(uuid, now);
                player.sendMessage(plugin.getMessageManager().getMessage("security.boundary-warning"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastWarningTimes.remove(event.getPlayer().getUniqueId());
    }

    private void spawnWarningParticles(Player player, Location loc) {
        try {
            player.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.01);
        } catch (Exception ignored) {
            // Ignorer si les particules ne sont pas supportées dans l'environnement de test
        }
    }
}
