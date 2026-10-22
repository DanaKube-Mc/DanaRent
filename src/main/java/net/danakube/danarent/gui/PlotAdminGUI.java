package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.rental.RentalTimerTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PlotAdminGUI extends AbstractGUI {

    private final Player owner;
    private final RentPlot plot;

    public PlotAdminGUI(DanaRentPlugin plugin, Player owner, RentPlot plot) {
        super(plugin,
                plugin.getGuiManager().getConfig("plot_admin").getString("title", "<b>Édition : <plot_name></b>")
                        .replace("<plot_name>", plot.getName()),
                plugin.getGuiManager().getConfig("plot_admin").getInt("size", 27));
        this.owner = owner;
        this.plot = plot;
    }

    @Override
    protected void initialize() {
        FileConfiguration config = plugin.getGuiManager().getConfig("plot_admin");
        if (config == null) return;

        setupBorders(config);
        setupButtons(config);
    }

    private void setupBorders(FileConfiguration config) {
        GUIManager gm = plugin.getGuiManager();

        String cornerMatName = config.getString("borders.corners.material", "BLACK_STAINED_GLASS_PANE");
        Material cornerMat = Material.matchMaterial(cornerMatName);
        if (cornerMat != null) {
            String name = config.getString("borders.corners.name", " ");
            ItemStack cornerItem = gm.buildItem(cornerMat, name, Collections.emptyList(), Collections.emptyMap(), false);
            for (int slot : config.getIntegerList("borders.corners.slots")) {
                setItem(slot, cornerItem);
            }
        }

        String innerMatName = config.getString("borders.inner.material", "GRAY_STAINED_GLASS_PANE");
        Material innerMat = Material.matchMaterial(innerMatName);
        if (innerMat != null) {
            String name = config.getString("borders.inner.name", " ");
            ItemStack innerItem = gm.buildItem(innerMat, name, Collections.emptyList(), Collections.emptyMap(), false);
            for (int slot : config.getIntegerList("borders.inner.slots")) {
                setItem(slot, innerItem);
            }
        }
    }

    private void setupButtons(FileConfiguration config) {
        GUIManager gm = plugin.getGuiManager();

        String typeLabel = plot.getPlotType() == PlotType.FARM ? "Agriculture (FARM)" : "Minage (MINING)";
        String marketStatus = plot.isListed() ? "<#55ff55>En ligne (Public)</#55ff55>" : "<#808080>Masqué (Privé)</#808080>";

        ActiveContract activeContract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
        String contractStatus = activeContract != null
                ? "<#ffaa00>Loué (" + RentalTimerTask.formatDuration(activeContract.getRemainingSeconds()) + ")</#ffaa00>"
                : "<#55ff55>Aucun bail en cours</#55ff55>";
        String tenantName = "Aucun";
        String remainingTime = "0s";
        if (activeContract != null) {
            OfflinePlayer tenant = Bukkit.getOfflinePlayer(activeContract.getTenantUuid());
            tenantName = tenant.getName() != null ? tenant.getName() : activeContract.getTenantUuid().toString().substring(0, 8);
            remainingTime = RentalTimerTask.formatDuration(activeContract.getRemainingSeconds());
        }

        Map<String, String> placeholders = Map.of(
                "plot_name", plot.getName(),
                "type_label", typeLabel,
                "price", String.format("%.2f", plot.getPriceLumens()),
                "duration_formatted", RentalTimerTask.formatDuration(plot.getDurationSeconds()),
                "market_status", marketStatus,
                "contract_status", contractStatus,
                "tenant_name", tenantName,
                "remaining_time", remainingTime,
                "count", String.valueOf(plot.getAllowedMaterials().size())
        );

        // 1. Bouton Retour (Slot 19)
        int retSlot = config.getInt("buttons.return_button.slot", 19);
        Material retMat = Material.matchMaterial(config.getString("buttons.return_button.material", "ARROW"));
        ItemStack retItem = gm.buildItem(retMat != null ? retMat : Material.ARROW,
                config.getString("buttons.return_button.name", "Retour"),
                config.getStringList("buttons.return_button.lore"), placeholders, false);
        setItem(retSlot, retItem, event -> new ManageGUI(plugin, owner, 1).open(owner));

        // 2. Basculer Type (Slot 10)
        int typeSlot = config.getInt("buttons.toggle_type.slot", 10);
        String typeMatKey = plot.getPlotType() == PlotType.FARM ? "buttons.toggle_type.material_farm" : "buttons.toggle_type.material_mining";
        Material typeMat = Material.matchMaterial(config.getString(typeMatKey, "WHEAT"));
        ItemStack typeItem = gm.buildItem(typeMat != null ? typeMat : Material.WHEAT,
                config.getString("buttons.toggle_type.name", "Type"),
                config.getStringList("buttons.toggle_type.lore"), placeholders, false);
        setItem(typeSlot, typeItem, event -> {
            PlotType newType = plot.getPlotType() == PlotType.FARM ? PlotType.MINING : PlotType.FARM;
            if (!plugin.getConfigManager().isTypeEnabled(newType)) {
                owner.sendMessage(plugin.getMessageManager().getMessage("types.type-disabled", Map.of(
                        "type", newType.name()
                )));
                return;
            }
            plot.setPlotType(newType);
            plugin.getPlotRepository().save(plot);
            new PlotAdminGUI(plugin, owner, plot).open(owner);
        });

        // 3. Modifier Tarif (Slot 11)
        int priceSlot = config.getInt("buttons.change_price.slot", 11);
        Material priceMat = Material.matchMaterial(config.getString("buttons.change_price.material", "GOLD_INGOT"));
        ItemStack priceItem = gm.buildItem(priceMat != null ? priceMat : Material.GOLD_INGOT,
                config.getString("buttons.change_price.name", "Tarif"),
                config.getStringList("buttons.change_price.lore"), placeholders, false);
        setItem(priceSlot, priceItem, event -> {
            if (event.getClick() == ClickType.RIGHT) {
                plot.setPriceLumens(Math.max(10.0, plot.getPriceLumens() - 10.0));
            } else {
                plot.setPriceLumens(plot.getPriceLumens() + 10.0);
            }
            plugin.getPlotRepository().save(plot);
            new PlotAdminGUI(plugin, owner, plot).open(owner);
        });

        // 4. Modifier Durée (Slot 12)
        int durSlot = config.getInt("buttons.change_duration.slot", 12);
        Material durMat = Material.matchMaterial(config.getString("buttons.change_duration.material", "CLOCK"));
        ItemStack durItem = gm.buildItem(durMat != null ? durMat : Material.CLOCK,
                config.getString("buttons.change_duration.name", "Durée"),
                config.getStringList("buttons.change_duration.lore"), placeholders, false);
        setItem(durSlot, durItem, event -> {
            if (event.getClick() == ClickType.RIGHT) {
                plot.setDurationSeconds(Math.max(900L, plot.getDurationSeconds() - 900L));
            } else {
                plot.setDurationSeconds(plot.getDurationSeconds() + 900L);
            }
            plugin.getPlotRepository().save(plot);
            new PlotAdminGUI(plugin, owner, plot).open(owner);
        });

        // 5. Visibilité Marché (Slot 13)
        int listSlot = config.getInt("buttons.toggle_listed.slot", 13);
        String listMatKey = plot.isListed() ? "buttons.toggle_listed.material_listed" : "buttons.toggle_listed.material_unlisted";
        Material listMat = Material.matchMaterial(config.getString(listMatKey, "LIME_DYE"));
        ItemStack listItem = gm.buildItem(listMat != null ? listMat : Material.LIME_DYE,
                config.getString("buttons.toggle_listed.name", "Marché"),
                config.getStringList("buttons.toggle_listed.lore"), placeholders, false);
        setItem(listSlot, listItem, event -> {
            plot.setListed(!plot.isListed());
            plugin.getPlotRepository().save(plot);
            new PlotAdminGUI(plugin, owner, plot).open(owner);
        });

        // 6. Whitelist Blocs Minage (Slot 14)
        if (plot.getPlotType() == PlotType.MINING) {
            int wlSlot = config.getInt("buttons.whitelist_blocks.slot", 14);
            Material wlMat = Material.matchMaterial(config.getString("buttons.whitelist_blocks.material", "DEEPSLATE_COAL_ORE"));
            ItemStack wlItem = gm.buildItem(wlMat != null ? wlMat : Material.DEEPSLATE_COAL_ORE,
                    config.getString("buttons.whitelist_blocks.name", "Whitelist"),
                    config.getStringList("buttons.whitelist_blocks.lore"), placeholders, false);
            setItem(wlSlot, wlItem, event -> {
                new BlockSelectorGUI(plugin, owner, plot, 1).open(owner);
            });
        }

        // 7. Statut du bail / Résiliation d'urgence (Slot 15)
        int contractSlot = config.getInt("buttons.contract_info.slot", 15);
        String cMatKey = activeContract != null ? "buttons.contract_info.material_active" : "buttons.contract_info.material_none";
        Material cMat = Material.matchMaterial(config.getString(cMatKey, "BOOK"));
        ItemStack contractItem = gm.buildItem(cMat != null ? cMat : Material.BOOK,
                config.getString("buttons.contract_info.name", "Bail"),
                config.getStringList("buttons.contract_info.lore"), placeholders, false);
        setItem(contractSlot, contractItem, event -> {
            if (event.isShiftClick() && activeContract != null) {
                plugin.getEscrowService().cancelRental(activeContract, plot).thenRun(() -> {
                    plugin.getRentalManager().removeContract(activeContract.getId());
                    owner.sendMessage(plugin.getMessageManager().getMessage("rental.cancelled-owner", Map.of(
                            "plot_name", plot.getName(),
                            "revenue", String.format("%.2f", plot.getPendingRevenue())
                    )));
                    new PlotAdminGUI(plugin, owner, plot).open(owner);
                });
            }
        });

        // 8. Supprimer la parcelle (Slot 16)
        int delSlot = config.getInt("buttons.delete_plot.slot", 16);
        Material delMat = Material.matchMaterial(config.getString("buttons.delete_plot.material", "TNT"));
        ItemStack delItem = gm.buildItem(delMat != null ? delMat : Material.TNT,
                config.getString("buttons.delete_plot.name", "Supprimer"),
                config.getStringList("buttons.delete_plot.lore"), placeholders, false);
        setItem(delSlot, delItem, event -> {
            if (event.isShiftClick()) {
                if (activeContract != null) {
                    owner.sendMessage(plugin.getMessageManager().getMessage("plots.error-has-active-contract"));
                    return;
                }
                plugin.getPlotRepository().delete(plot.getId()).thenRun(() -> {
                    plugin.getPlotManager().removePlot(plot.getId());
                    owner.sendMessage(plugin.getMessageManager().getMessage("plots.deleted-success", Map.of("plot_name", plot.getName())));
                    new ManageGUI(plugin, owner, 1).open(owner);
                });
            }
        });
    }
}
