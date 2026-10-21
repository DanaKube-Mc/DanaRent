package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.rental.RentalTimerTask;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ManageGUI extends AbstractGUI {

    public static final int[] USABLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Player owner;
    private final int page;

    public ManageGUI(DanaRentPlugin plugin, Player owner, int page) {
        super(plugin,
                plugin.getGuiManager().getConfig("manage").getString("title", "<b>Mes Parcelles</b>"),
                plugin.getGuiManager().getConfig("manage").getInt("size", 54));
        this.owner = owner;
        this.page = Math.max(1, page);
    }

    @Override
    protected void initialize() {
        FileConfiguration config = plugin.getGuiManager().getConfig("manage");
        if (config == null) return;

        // 1. Bordures
        setupBorders(config);

        // 2. Récupération des parcelles du propriétaire
        List<RentPlot> myPlots = new ArrayList<>();
        double totalPending = 0.0;
        for (RentPlot plot : plugin.getPlotManager().getAllPlots()) {
            if (plot.getOwnerUuid().equals(owner.getUniqueId())) {
                myPlots.add(plot);
                totalPending += plot.getPendingRevenue();
            }
        }

        // 3. Bouton Collecte des Revenus (Slot 4)
        setupCollectButton(config, totalPending);

        // 4. Parcelles & Pagination
        int totalPlots = myPlots.size();
        int pageSize = USABLE_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPlots / pageSize));
        int currentPage = Math.min(page, totalPages);

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalPlots);

        for (int i = startIndex; i < endIndex; i++) {
            RentPlot plot = myPlots.get(i);
            int slot = USABLE_SLOTS[i - startIndex];
            ItemStack card = createPlotCard(config, plot);
            setItem(slot, card, event -> new PlotAdminGUI(plugin, owner, plot).open(owner));
        }

        setupPagination(config, currentPage, totalPages, totalPlots);
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

    private void setupCollectButton(FileConfiguration config, double totalPending) {
        GUIManager gm = plugin.getGuiManager();
        int slot = config.getInt("buttons.collect_revenue.slot", 4);
        Material mat = Material.matchMaterial(config.getString("buttons.collect_revenue.material", "CHEST"));
        String name = config.getString("buttons.collect_revenue.name", "Cagnotte");
        List<String> lore = config.getStringList("buttons.collect_revenue.lore");

        ItemStack chestItem = gm.buildItem(mat != null ? mat : Material.CHEST, name, lore,
                Map.of("total_pending", String.format("%.2f", totalPending)), false);

        setItem(slot, chestItem, event -> {
            plugin.getEscrowService().collectPendingRevenue(owner.getUniqueId()).thenAccept(collected -> {
                if (collected > 0) {
                    for (RentPlot p : plugin.getPlotManager().getAllPlots()) {
                        if (p.getOwnerUuid().equals(owner.getUniqueId())) {
                            p.setPendingRevenue(0.0);
                        }
                    }
                    owner.sendMessage(plugin.getMessageManager().getMessage("economy.funds-collected",
                            Map.of("amount", String.format("%.2f", collected))));
                } else {
                    owner.sendMessage(plugin.getMessageManager().getMessage("economy.no-funds"));
                }
                new ManageGUI(plugin, owner, page).open(owner);
            });
        });
    }

    private void setupPagination(FileConfiguration config, int currentPage, int totalPages, int totalPlots) {
        GUIManager gm = plugin.getGuiManager();

        if (currentPage > 1) {
            int slot = config.getInt("buttons.previous_page.slot", 48);
            Material mat = Material.matchMaterial(config.getString("buttons.previous_page.material", "ARROW"));
            String name = config.getString("buttons.previous_page.name", "«");
            List<String> lore = config.getStringList("buttons.previous_page.lore");
            ItemStack prevItem = gm.buildItem(mat != null ? mat : Material.ARROW, name, lore, Map.of("page", String.valueOf(currentPage - 1)), false);
            setItem(slot, prevItem, event -> new ManageGUI(plugin, owner, currentPage - 1).open(owner));
        }

        int infoSlot = config.getInt("buttons.page_info.slot", 49);
        Material infoMat = Material.matchMaterial(config.getString("buttons.page_info.material", "BOOK"));
        String infoName = config.getString("buttons.page_info.name", "Page");
        List<String> infoLore = config.getStringList("buttons.page_info.lore");
        ItemStack infoItem = gm.buildItem(infoMat != null ? infoMat : Material.BOOK, infoName, infoLore,
                Map.of("current_page", String.valueOf(currentPage), "total_pages", String.valueOf(totalPages), "total_plots", String.valueOf(totalPlots)), false);
        setItem(infoSlot, infoItem);

        if (currentPage < totalPages) {
            int slot = config.getInt("buttons.next_page.slot", 50);
            Material mat = Material.matchMaterial(config.getString("buttons.next_page.material", "ARROW"));
            String name = config.getString("buttons.next_page.name", "»");
            List<String> lore = config.getStringList("buttons.next_page.lore");
            ItemStack nextItem = gm.buildItem(mat != null ? mat : Material.ARROW, name, lore, Map.of("page", String.valueOf(currentPage + 1)), false);
            setItem(slot, nextItem, event -> new ManageGUI(plugin, owner, currentPage + 1).open(owner));
        }
    }

    private ItemStack createPlotCard(FileConfiguration config, RentPlot plot) {
        GUIManager gm = plugin.getGuiManager();

        String matKey = plot.getPlotType() == PlotType.FARM ? "plot_card.material_farm" : "plot_card.material_mining";
        String defMat = plot.getPlotType() == PlotType.FARM ? "HAY_BLOCK" : "DEEPSLATE_DIAMOND_ORE";
        Material mat = Material.matchMaterial(config.getString(matKey, defMat));
        if (mat == null) mat = Material.OAK_SIGN;

        String name = config.getString("plot_card.name", "<plot_name>");
        List<String> lore = config.getStringList("plot_card.lore");

        String typeLabel = plot.getPlotType() == PlotType.FARM ? "Agriculture (FARM)" : "Minage (MINING)";
        String marketStatus = plot.isListed() ? "<#55ff55>En ligne (Public)</#55ff55>" : "<#808080>Masqué (Privé)</#808080>";

        ActiveContract activeContract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
        String contractStatus = activeContract != null
                ? "<#ffaa00>Loué (" + RentalTimerTask.formatDuration(activeContract.getRemainingSeconds()) + ")</#ffaa00>"
                : "<#55ff55>Disponible</#55ff55>";

        Map<String, String> placeholders = Map.of(
                "plot_name", plot.getName(),
                "type_label", typeLabel,
                "price", String.format("%.2f", plot.getPriceLumens()),
                "duration_formatted", RentalTimerTask.formatDuration(plot.getDurationSeconds()),
                "market_status", marketStatus,
                "contract_status", contractStatus
        );

        return gm.buildItem(mat, name, lore, placeholders, false);
    }
}
