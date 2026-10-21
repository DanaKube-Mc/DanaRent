package net.danakube.danarent.rental;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandWorldResetEvent;
import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class SkyblockTerminationListener implements Listener {

    private final DanaRentPlugin plugin;

    public SkyblockTerminationListener(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onIslandDisband(IslandDisbandEvent event) {
        if (event.getIsland() != null) {
            handleIslandTermination(event.getIsland().getUniqueId().toString());
        }
    }

    @EventHandler
    public void onIslandReset(IslandWorldResetEvent event) {
        if (event.getIsland() != null) {
            handleIslandTermination(event.getIsland().getUniqueId().toString());
        }
    }

    public void handleIslandTermination(String islandId) {
        List<RentPlot> plots = plugin.getPlotManager().getPlotsForIsland(islandId);

        for (RentPlot plot : plots) {
            ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
            if (contract != null) {
                // Résiliation forcée : Remboursement intégral du solde séquestre au locataire
                plugin.getEscrowService().refundRemainingEscrow(contract.getTenantUuid(), contract.getEscrowBalance());

                // Évacuation du locataire vers le spawn
                Player tenant = Bukkit.getPlayer(contract.getTenantUuid());
                if (tenant != null && tenant.isOnline()) {
                    tenant.teleport(tenant.getWorld().getSpawnLocation());
                    tenant.sendMessage(plugin.getMessageManager().getMessage("rental.cancelled-island-reset"));
                }
                contract.setStatus(ContractStatus.CANCELLED);
                plugin.getContractRepository().save(contract);
                plugin.getRentalManager().removeContract(contract.getId());
            }

            // Purge de la parcelle
            plugin.getPlotRepository().delete(plot.getId());
            plugin.getPlotManager().removePlot(plot.getId());
        }
    }
}
