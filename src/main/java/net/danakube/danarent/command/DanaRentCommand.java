package net.danakube.danarent.command;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.config.MessageManager;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DanaRentCommand implements CommandExecutor, TabCompleter {

    private final DanaRentPlugin plugin;

    public DanaRentCommand(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length == 0) {
            sender.sendMessage(msg.getMessage("admin.unknown-command"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "collect" -> handleCollect(sender);
            case "listall" -> handleListAll(sender);
            default -> sender.sendMessage(msg.getMessage("admin.unknown-command"));
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();
        if (!sender.hasPermission("danarent.admin")) {
            sender.sendMessage(msg.getMessage("admin.no-permission"));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(plugin.getMessageManager().getMessage("admin.reloaded"));
    }

    private void handleCollect(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.getMessage("admin.only-player"));
            return;
        }

        if (!player.hasPermission("danarent.owner")) {
            player.sendMessage(msg.getMessage("admin.no-permission"));
            return;
        }

        plugin.getEscrowService().collectPendingRevenue(player.getUniqueId()).thenAccept(amount -> {
            if (amount > 0) {
                for (RentPlot p : plugin.getPlotManager().getAllPlots()) {
                    if (p.getOwnerUuid().equals(player.getUniqueId())) {
                        p.setPendingRevenue(0.0);
                    }
                }
                player.sendMessage(msg.getMessage("economy.funds-collected", Map.of("amount", String.format("%.2f", amount))));
            } else {
                player.sendMessage(msg.getMessage("economy.no-funds"));
            }
        });
    }

    private void handleListAll(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();
        if (!sender.hasPermission("danarent.admin")) {
            sender.sendMessage(msg.getMessage("admin.no-permission"));
            return;
        }

        Collection<RentPlot> plots = plugin.getPlotManager().getAllPlots();
        sender.sendMessage(msg.getMessage("admin.listall-header"));

        if (plots.isEmpty()) {
            sender.sendMessage(msg.getMessage("admin.listall-empty"));
            return;
        }

        for (RentPlot plot : plots) {
            OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(plot.getOwnerUuid());
            String ownerName = ownerPlayer.getName() != null ? ownerPlayer.getName() : plot.getOwnerUuid().toString().substring(0, 8);

            ActiveContract contract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
            String status;
            if (contract != null) {
                OfflinePlayer tenantPlayer = Bukkit.getOfflinePlayer(contract.getTenantUuid());
                String tenantName = tenantPlayer.getName() != null ? tenantPlayer.getName() : contract.getTenantUuid().toString().substring(0, 8);
                status = msg.getRawMessage("admin.status-rented").replace("<tenant>", tenantName);
            } else if (plot.isListed()) {
                status = msg.getRawMessage("admin.status-available");
            } else {
                status = msg.getRawMessage("admin.status-unlisted");
            }

            sender.sendMessage(msg.getMessage("admin.listall-item", Map.of(
                    "id", String.valueOf(plot.getId()),
                    "plot_name", plot.getName(),
                    "type", plot.getPlotType().name(),
                    "owner", ownerName,
                    "status", status
            )));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> allowed = new ArrayList<>();

            if (sender.hasPermission("danarent.owner")) {
                allowed.add("collect");
            }
            if (sender.hasPermission("danarent.admin")) {
                allowed.add("reload");
                allowed.add("listall");
            }

            for (String sub : allowed) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
