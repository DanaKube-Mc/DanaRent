package net.danakube.danarent.command;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.config.MessageManager;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.plot.PlotValidator;
import net.danakube.danarent.plot.ValidationResult;
import net.danakube.danarent.util.Cuboid;
import net.danakube.danarent.wand.PlayerSelection;
import net.danakube.danarent.wand.WandFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FarmCommand implements CommandExecutor, TabCompleter {

    private final DanaRentPlugin plugin;

    public FarmCommand(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut executer cette commande.");
            return true;
        }

        MessageManager msg = plugin.getMessageManager();

        if (args.length == 0) {
            new net.danakube.danarent.gui.CatalogGUI(plugin, player, 1).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> new net.danakube.danarent.gui.CatalogGUI(plugin, player, 1).open(player);
            case "wand" -> handleWand(player);
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "return" -> handleReturn(player);
            case "cancel" -> handleCancel(player);
            default -> player.sendMessage(msg.getMessage("wand.given"));
        }

        return true;
    }

    private void handleWand(Player player) {
        if (!player.hasPermission("danarent.owner")) {
            player.sendMessage(plugin.getMessageManager().getMessage("plots.error-not-owner"));
            return;
        }

        player.getInventory().addItem(WandFactory.createWand(plugin));
        player.sendMessage(plugin.getMessageManager().getMessage("wand.given"));
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("danarent.owner")) {
            player.sendMessage(plugin.getMessageManager().getMessage("plots.error-not-owner"));
            return;
        }

        MessageManager msg = plugin.getMessageManager();
        if (args.length < 2) {
            player.sendMessage(msg.getMessage("plots.error-no-selection"));
            return;
        }

        String plotName = args[1];
        PlayerSelection sel = plugin.getSelectionManager().getSelection(player.getUniqueId());
        if (sel == null || !sel.hasBothPositions()) {
            player.sendMessage(msg.getMessage("plots.error-no-selection"));
            return;
        }

        Cuboid cuboid;
        try {
            cuboid = sel.toCuboid();
        } catch (IllegalStateException e) {
            player.sendMessage(msg.getMessage("plots.error-different-worlds"));
            return;
        }

        PlotValidator validator = plugin.getPlotValidator();
        ValidationResult result = validator.validate(player, plotName, cuboid);
        if (!result.isValid()) {
            player.sendMessage(msg.getMessage(result.getMessageKey(), result.getPlaceholders()));
            return;
        }

        PlotType plotType = PlotType.FARM;
        if (args.length > 2 && args[2].equalsIgnoreCase("mining")) {
            plotType = PlotType.MINING;
        }

        String islandId = plugin.getSkyblockHook().getIslandId(player);
        RentPlot plot = new RentPlot(
                0, player.getUniqueId(), islandId, plotName, plotType,
                cuboid.getWorldName(), cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ(),
                100.0, 3600, Set.of(), false, 0.0, System.currentTimeMillis()
        );

        plugin.getPlotRepository().save(plot).thenAccept(id -> {
            plot.setId(id);
            plugin.getPlotManager().addPlot(plot);
            plugin.getSelectionManager().clearSelection(player.getUniqueId());
            player.sendMessage(msg.getMessage("plots.created-success", Map.of("plot_name", plotName)));
        }).exceptionally(ex -> {
            player.sendMessage(msg.getMessage("plots.error-overlapping"));
            return null;
        });
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("danarent.owner")) {
            player.sendMessage(plugin.getMessageManager().getMessage("plots.error-not-owner"));
            return;
        }

        MessageManager msg = plugin.getMessageManager();
        if (args.length < 2) {
            player.sendMessage(msg.getMessage("plots.error-not-found", Map.of("plot_name", "?")));
            return;
        }

        String plotName = args[1];
        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlotByName(plotName, player.getUniqueId());
        if (plotOpt.isEmpty()) {
            player.sendMessage(msg.getMessage("plots.error-not-found", Map.of("plot_name", plotName)));
            return;
        }

        RentPlot plot = plotOpt.get();
        plugin.getContractRepository().findActiveByPlotId(plot.getId()).thenAccept(activeContractOpt -> {
            if (activeContractOpt.isPresent()) {
                player.sendMessage(msg.getMessage("plots.error-has-active-contract"));
                return;
            }

            plugin.getPlotRepository().delete(plot.getId()).thenRun(() -> {
                plugin.getPlotManager().removePlot(plot.getId());
                player.sendMessage(msg.getMessage("plots.deleted-success", Map.of("plot_name", plotName)));
            });
        });
    }

    private void handleReturn(Player player) {
        MessageManager msg = plugin.getMessageManager();
        net.danakube.danarent.model.ActiveContract contract = plugin.getRentalManager().getActiveContractByTenant(player.getUniqueId());
        if (contract == null) {
            player.sendMessage(msg.getMessage("rental.no-active-contract"));
            return;
        }

        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlot(contract.getPlotId());
        if (plotOpt.isEmpty()) {
            player.sendMessage(msg.getMessage("rental.no-active-contract"));
            return;
        }

        RentPlot plot = plotOpt.get();
        player.teleport(plot.getCuboid().getCenter());
        player.sendMessage(msg.getMessage("rental.returned-to-plot", Map.of("plot_name", plot.getName())));
    }

    private void handleCancel(Player player) {
        MessageManager msg = plugin.getMessageManager();
        net.danakube.danarent.model.ActiveContract contract = plugin.getRentalManager().getActiveContractByTenant(player.getUniqueId());
        if (contract == null) {
            player.sendMessage(msg.getMessage("rental.no-active-contract"));
            return;
        }

        Optional<RentPlot> plotOpt = plugin.getPlotManager().getPlot(contract.getPlotId());
        if (plotOpt.isEmpty()) {
            player.sendMessage(msg.getMessage("rental.no-active-contract"));
            return;
        }

        RentPlot plot = plotOpt.get();
        plugin.getEscrowService().cancelRental(contract, plot).thenRun(() -> {
            plugin.getRentalManager().removeContract(contract.getId());
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(msg.getMessage("rental.cancelled-tenant", Map.of(
                    "refund", String.format("%.2f", contract.getEscrowBalance()),
                    "plot_name", plot.getName()
            )));

            org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(plot.getOwnerUuid());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(msg.getMessage("rental.cancelled-owner", Map.of(
                        "plot_name", plot.getName(),
                        "revenue", String.format("%.2f", plot.getPendingRevenue())
                )));
            }
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("list", "wand", "create", "delete", "return", "cancel")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete") && sender instanceof Player player) {
            for (RentPlot plot : plugin.getPlotManager().getAllPlots()) {
                if (plot.getOwnerUuid().equals(player.getUniqueId())) {
                    if (plot.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(plot.getName());
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            for (String type : List.of("farm", "mining")) {
                if (type.startsWith(args[2].toLowerCase())) {
                    completions.add(type);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
