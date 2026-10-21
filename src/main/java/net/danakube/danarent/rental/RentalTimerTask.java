package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.config.MessageManager;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class RentalTimerTask implements Runnable {

    private final DanaRentPlugin plugin;

    public RentalTimerTask(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        RentalManager rentalManager = plugin.getRentalManager();
        if (rentalManager == null) return;

        MessageManager msg = plugin.getMessageManager();

        for (ActiveContract contract : rentalManager.getAllActiveContracts()) {
            Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlot(contract.getPlotId());
            String plotName = plotOpt.map(RentPlot::getName).orElse("Champ");

            if (contract.isExpired()) {
                // Bail échu : clôture financière et évacuation
                plotOpt.ifPresent(plot -> plugin.getEscrowService().completeRental(contract, plot));
                rentalManager.removeContract(contract.getId());

                Player tenant = Bukkit.getPlayer(contract.getTenantUuid());
                if (tenant != null && tenant.isOnline()) {
                    tenant.teleport(tenant.getWorld().getSpawnLocation());
                    tenant.sendMessage(msg.getMessage("rental.expired-tenant", Map.of("plot_name", plotName)));
                }
            } else {
                // Bail actif : mise à jour de l'ActionBar
                Player tenant = Bukkit.getPlayer(contract.getTenantUuid());
                if (tenant != null && tenant.isOnline()) {
                    String formattedTime = formatDuration(contract.getRemainingSeconds());
                    tenant.sendActionBar(msg.getMessage("rental.actionbar-timer", Map.of(
                            "time_left", formattedTime,
                            "plot_name", plotName
                    )));
                }
            }
        }
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, secs);
        } else {
            return String.format("%02ds", secs);
        }
    }
}
