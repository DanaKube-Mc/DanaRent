package net.danakube.danarent.wand;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class WandFactory {

    public static final String PDC_KEY = "selection_wand";

    public static NamespacedKey getKey(Plugin plugin) {
        return new NamespacedKey(plugin, PDC_KEY);
    }

    public static ItemStack createWand(DanaRentPlugin plugin) {
        ConfigManager config = plugin.getConfigManager();
        Material mat;
        try {
            mat = Material.valueOf(config.getWandMaterial().toUpperCase());
        } catch (Exception e) {
            mat = Material.GOLDEN_HOE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = getKey(plugin);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(mm.deserialize(config.getWandName()));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : config.getWandLore()) {
                loreComponents.add(mm.deserialize(line));
            }
            meta.lore(loreComponents);

            int cmd = config.getWandCustomModelData();
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isWand(ItemStack item, Plugin plugin) {
        if (item == null || !item.hasItemMeta() || plugin == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        NamespacedKey key = getKey(plugin);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
