package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.util.BlockScanner;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockSelectorGUI extends AbstractGUI {

    public static final int[] USABLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Player owner;
    private final RentPlot plot;
    private final int page;
    private final Set<Material> selectedMaterials;

    public BlockSelectorGUI(DanaRentPlugin plugin, Player owner, RentPlot plot, int page) {
        this(plugin, owner, plot, page, new HashSet<>(plot.getAllowedMaterials()));
    }

    public BlockSelectorGUI(DanaRentPlugin plugin, Player owner, RentPlot plot, int page, Set<Material> selectedMaterials) {
        super(plugin,
                plugin.getGuiManager().getConfig("block_selector").getString("title", "<b>Sélecteur de Blocs</b>"),
                plugin.getGuiManager().getConfig("block_selector").getInt("size", 54));
        this.owner = owner;
        this.plot = plot;
        this.page = Math.max(1, page);
        this.selectedMaterials = selectedMaterials != null ? selectedMaterials : new HashSet<>();
    }

    @Override
    protected void initialize() {
        FileConfiguration config = plugin.getGuiManager().getConfig("block_selector");
        if (config == null) return;

        setupBorders(config);

        // 1. Scanner les matériaux distincts du cuboid
        List<Material> scanned = new ArrayList<>(BlockScanner.scanPlotMaterials(plot.getCuboid()));
        scanned.sort(Comparator.comparing(Enum::name));

        int totalMaterials = scanned.size();
        int pageSize = USABLE_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalMaterials / pageSize));
        int currentPage = Math.min(page, totalPages);

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalMaterials);

        for (int i = startIndex; i < endIndex; i++) {
            Material mat = scanned.get(i);
            int slot = USABLE_SLOTS[i - startIndex];
            boolean isAllowed = selectedMaterials.contains(mat);
            ItemStack item = createMaterialItem(config, mat, isAllowed);

            setItem(slot, item, event -> {
                if (selectedMaterials.contains(mat)) {
                    selectedMaterials.remove(mat);
                } else {
                    selectedMaterials.add(mat);
                }
                new BlockSelectorGUI(plugin, owner, plot, currentPage, selectedMaterials).open(owner);
            });
        }

        setupButtons(config, currentPage, totalPages, totalMaterials);
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

    private void setupButtons(FileConfiguration config, int currentPage, int totalPages, int totalMaterials) {
        GUIManager gm = plugin.getGuiManager();

        // Retour
        int retSlot = config.getInt("buttons.return_button.slot", 45);
        Material retMat = Material.matchMaterial(config.getString("buttons.return_button.material", "ARROW"));
        ItemStack retItem = gm.buildItem(retMat != null ? retMat : Material.ARROW,
                config.getString("buttons.return_button.name", "Retour"),
                config.getStringList("buttons.return_button.lore"), Collections.emptyMap(), false);
        setItem(retSlot, retItem, event -> new PlotAdminGUI(plugin, owner, plot).open(owner));

        // Enregistrer
        int saveSlot = config.getInt("buttons.save_whitelist.slot", 49);
        Material saveMat = Material.matchMaterial(config.getString("buttons.save_whitelist.material", "EMERALD"));
        Map<String, String> savePlaceholders = Map.of(
                "allowed_count", String.valueOf(selectedMaterials.size()),
                "total_count", String.valueOf(totalMaterials)
        );
        ItemStack saveItem = gm.buildItem(saveMat != null ? saveMat : Material.EMERALD,
                config.getString("buttons.save_whitelist.name", "Enregistrer"),
                config.getStringList("buttons.save_whitelist.lore"), savePlaceholders, false);
        setItem(saveSlot, saveItem, event -> {
            plot.setAllowedMaterials(selectedMaterials);
            plugin.getPlotRepository().save(plot);
            owner.sendMessage(plugin.getMessageManager().getMessage("plots.whitelist-saved"));
            new PlotAdminGUI(plugin, owner, plot).open(owner);
        });

        // Pagination
        if (currentPage > 1) {
            int slot = config.getInt("buttons.previous_page.slot", 48);
            Material mat = Material.matchMaterial(config.getString("buttons.previous_page.material", "ARROW"));
            ItemStack prevItem = gm.buildItem(mat != null ? mat : Material.ARROW,
                    config.getString("buttons.previous_page.name", "«"),
                    config.getStringList("buttons.previous_page.lore"), Map.of("page", String.valueOf(currentPage - 1)), false);
            setItem(slot, prevItem, event -> new BlockSelectorGUI(plugin, owner, plot, currentPage - 1, selectedMaterials).open(owner));
        }

        if (currentPage < totalPages) {
            int slot = config.getInt("buttons.next_page.slot", 50);
            Material mat = Material.matchMaterial(config.getString("buttons.next_page.material", "ARROW"));
            ItemStack nextItem = gm.buildItem(mat != null ? mat : Material.ARROW,
                    config.getString("buttons.next_page.name", "»"),
                    config.getStringList("buttons.next_page.lore"), Map.of("page", String.valueOf(currentPage + 1)), false);
            setItem(slot, nextItem, event -> new BlockSelectorGUI(plugin, owner, plot, currentPage + 1, selectedMaterials).open(owner));
        }
    }

    private ItemStack createMaterialItem(FileConfiguration config, Material material, boolean isAllowed) {
        GUIManager gm = plugin.getGuiManager();
        String path = isAllowed ? "items.allowed" : "items.disallowed";

        String name = config.getString(path + ".name", "<block_name>");
        List<String> lore = config.getStringList(path + ".lore");
        boolean glow = config.getBoolean(path + ".glow", isAllowed);

        Map<String, String> placeholders = Map.of(
                "block_name", formatMaterialName(material)
        );

        return gm.buildItem(material, name, lore, placeholders, glow);
    }

    private String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
