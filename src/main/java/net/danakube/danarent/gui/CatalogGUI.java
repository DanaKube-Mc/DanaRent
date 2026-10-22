package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.rental.RentalTimerTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CatalogGUI extends AbstractGUI {

    public static final int[] USABLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Player viewer;
    private final int page;

    public CatalogGUI(DanaRentPlugin plugin, Player viewer, int page) {
        super(plugin,
                plugin.getGuiManager().getConfig("catalog").getString("title", "<b>Catalogue des Terrains</b>"),
                plugin.getGuiManager().getConfig("catalog").getInt("size", 54));
        this.viewer = viewer;
        this.page = Math.max(1, page);
    }

    @Override
    protected void initialize() {
        FileConfiguration config = plugin.getGuiManager().getConfig("catalog");
        if (config == null) return;

        // 1. Décoration des bordures
        setupBorders(config);

        // 2. Récupération des parcelles listées
        List<RentPlot> listedPlots = new ArrayList<>(plugin.getPlotManager().getListedPlots());
        int totalPlots = listedPlots.size();
        int pageSize = USABLE_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPlots / pageSize));
        int currentPage = Math.min(page, totalPages);

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalPlots);

        // 3. Affichage des parcelles
        for (int i = startIndex; i < endIndex; i++) {
            RentPlot plot = listedPlots.get(i);
            int slot = USABLE_SLOTS[i - startIndex];
            ItemStack card = createPlotCard(config, plot);
            setItem(slot, card, event -> handlePlotClick(event.getClick(), plot));
        }

        // 4. Boutons de pagination
        setupPagination(config, currentPage, totalPages, totalPlots);
    }

    private void setupBorders(FileConfiguration config) {
        GUIManager gm = plugin.getGuiManager();

        // Corners
        String cornerMatName = config.getString("borders.corners.material", "BLACK_STAINED_GLASS_PANE");
        Material cornerMat = Material.matchMaterial(cornerMatName);
        if (cornerMat != null) {
            String name = config.getString("borders.corners.name", " ");
            ItemStack cornerItem = gm.buildItem(cornerMat, name, Collections.emptyList(), Collections.emptyMap(), false);
            for (int slot : config.getIntegerList("borders.corners.slots")) {
                setItem(slot, cornerItem);
            }
        }

        // Inner borders
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

    private void setupPagination(FileConfiguration config, int currentPage, int totalPages, int totalPlots) {
        GUIManager gm = plugin.getGuiManager();

        // Bouton Précédent
        if (currentPage > 1) {
            int slot = config.getInt("buttons.previous_page.slot", 48);
            Material mat = Material.matchMaterial(config.getString("buttons.previous_page.material", "ARROW"));
            String name = config.getString("buttons.previous_page.name", "«");
            List<String> lore = config.getStringList("buttons.previous_page.lore");
            ItemStack prevItem = gm.buildItem(mat != null ? mat : Material.ARROW, name, lore, Map.of("page", String.valueOf(currentPage - 1)), false);
            setItem(slot, prevItem, event -> new CatalogGUI(plugin, viewer, currentPage - 1).open(viewer));
        }

        // Info Page
        int infoSlot = config.getInt("buttons.page_info.slot", 49);
        Material infoMat = Material.matchMaterial(config.getString("buttons.page_info.material", "BOOK"));
        String infoName = config.getString("buttons.page_info.name", "Page");
        List<String> infoLore = config.getStringList("buttons.page_info.lore");
        ItemStack infoItem = gm.buildItem(infoMat != null ? infoMat : Material.BOOK, infoName, infoLore,
                Map.of("current_page", String.valueOf(currentPage), "total_pages", String.valueOf(totalPages), "total_plots", String.valueOf(totalPlots)), false);
        setItem(infoSlot, infoItem);

        // Bouton Suivant
        if (currentPage < totalPages) {
            int slot = config.getInt("buttons.next_page.slot", 50);
            Material mat = Material.matchMaterial(config.getString("buttons.next_page.material", "ARROW"));
            String name = config.getString("buttons.next_page.name", "»");
            List<String> lore = config.getStringList("buttons.next_page.lore");
            ItemStack nextItem = gm.buildItem(mat != null ? mat : Material.ARROW, name, lore, Map.of("page", String.valueOf(currentPage + 1)), false);
            setItem(slot, nextItem, event -> new CatalogGUI(plugin, viewer, currentPage + 1).open(viewer));
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

        OfflinePlayer owner = Bukkit.getOfflinePlayer(plot.getOwnerUuid());
        String ownerName = owner.getName() != null ? owner.getName() : plot.getOwnerUuid().toString().substring(0, 8);
        String typeLabel = plot.getPlotType() == PlotType.FARM ? "Agriculture (FARM)" : "Minage (MINING)";

        int width = plot.getMaxX() - plot.getMinX() + 1;
        int height = plot.getMaxY() - plot.getMinY() + 1;
        int length = plot.getMaxZ() - plot.getMinZ() + 1;

        ActiveContract activeContract = plugin.getRentalManager().getActiveContractByPlot(plot.getId());
        String statusColored = activeContract != null
                ? "<#ffaa00>Loué (" + RentalTimerTask.formatDuration(activeContract.getRemainingSeconds()) + ")</#ffaa00>"
                : "<#55ff55>Disponible</#55ff55>";

        List<String> cardLore = new ArrayList<>(lore);
        if (!plugin.getConfigManager().isTypeEnabled(plot.getPlotType())) {
            statusColored = "<#ff5555>Suspendu (Désactivé)</#ff5555>";
            for (int i = 0; i < cardLore.size(); i++) {
                if (cardLore.get(i).contains("Clic gauche")) {
                    cardLore.set(i, plugin.getMessageManager().getRawMessage("types.type-suspended-lore"));
                }
            }
        }

        Map<String, String> placeholders = Map.of(
                "plot_name", plot.getName(),
                "owner_name", ownerName,
                "type_label", typeLabel,
                "world", plot.getWorld(),
                "width", String.valueOf(width),
                "length", String.valueOf(length),
                "height", String.valueOf(height),
                "price", String.format("%.2f", plot.getPriceLumens()),
                "duration_formatted", RentalTimerTask.formatDuration(plot.getDurationSeconds()),
                "status_colored", statusColored
        );

        return gm.buildItem(mat, name, cardLore, placeholders, false);
    }

    private void handlePlotClick(ClickType clickType, RentPlot plot) {
        if (clickType == ClickType.RIGHT) {
            // Clic Droit : Visiter / Inspecter 1 bloc à l'extérieur
            World world = Bukkit.getWorld(plot.getWorld());
            if (world != null) {
                viewer.closeInventory();
                Location inspectLoc = new Location(world,
                        plot.getCuboid().getCenter().getX(),
                        plot.getMinY(),
                        plot.getMinZ() - 1.0,
                        0f, 0f);
                viewer.teleport(inspectLoc);
                viewer.sendMessage(plugin.getMessageManager().getMessage("plots.inspecting", Map.of("plot_name", plot.getName())));
            }
            return;
        }

        if (clickType == ClickType.LEFT) {
            // Clic Gauche : Louer immédiatement
            if (!plugin.getConfigManager().isTypeEnabled(plot.getPlotType())) {
                viewer.sendMessage(plugin.getMessageManager().getMessage("types.type-disabled", Map.of(
                        "type", plot.getPlotType().name()
                )));
                return;
            }

            int activeRentals = plugin.getRentalManager().getActiveRentalCount(viewer.getUniqueId());
            int maxRentals = net.danakube.danarent.util.LimitResolver.getMaxActiveRentals(plugin, viewer);
            if (activeRentals >= maxRentals) {
                viewer.sendMessage(plugin.getMessageManager().getMessage("limits.rental-reached", Map.of(
                        "current", String.valueOf(activeRentals),
                        "max", String.valueOf(maxRentals)
                )));
                return;
            }

            if (plugin.getRentalManager().getActiveContractByPlot(plot.getId()) != null) {
                viewer.sendMessage(plugin.getMessageManager().getMessage("rental.plot-already-rented"));
                return;
            }

            if (!plugin.getEconomyHook().hasEnough(viewer.getUniqueId(), plot.getPriceLumens())) {
                viewer.sendMessage(plugin.getMessageManager().getMessage("economy.insufficient-funds", Map.of(
                        "price", String.format("%.2f", plot.getPriceLumens())
                )));
                return;
            }

            plugin.getEscrowService().startRental(viewer.getUniqueId(), plot).thenAccept(contract -> {
                plugin.getRentalManager().addContract(contract);
                viewer.closeInventory();
                viewer.teleport(plot.getCuboid().getCenter());
                viewer.sendMessage(plugin.getMessageManager().getMessage("rental.started-tenant", Map.of(
                        "plot_name", plot.getName(),
                        "duration", RentalTimerTask.formatDuration(plot.getDurationSeconds()),
                        "price", String.format("%.2f", plot.getPriceLumens())
                )));

                Player owner = Bukkit.getPlayer(plot.getOwnerUuid());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(plugin.getMessageManager().getMessage("rental.started-owner", Map.of(
                            "tenant", viewer.getName(),
                            "plot_name", plot.getName()
                    )));
                }
            }).exceptionally(ex -> {
                viewer.sendMessage(plugin.getMessageManager().getMessage("rental.plot-already-rented"));
                return null;
            });
        }
    }
}
