package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIManager {

    private final DanaRentPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public GUIManager(DanaRentPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        File guisDir = new File(plugin.getDataFolder(), "guis");
        if (!guisDir.exists()) {
            guisDir.mkdirs();
        }

        String[] guiFiles = {"catalog", "manage", "plot_admin", "block_selector"};
        for (String name : guiFiles) {
            File file = new File(guisDir, name + ".yml");
            if (!file.exists()) {
                plugin.saveResource("guis/" + name + ".yml", false);
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            InputStream defStream = plugin.getResource("guis/" + name + ".yml");
            if (defStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8)));
            }
            configs.put(name, config);
        }
    }

    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    public ItemStack buildItem(Material material, String name, List<String> lore, Map<String, String> placeholders, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            MiniMessage mm = MiniMessage.miniMessage();
            if (name != null) {
                meta.displayName(mm.deserialize(applyPlaceholders(name, placeholders)));
            }

            if (lore != null) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(mm.deserialize(applyPlaceholders(line, placeholders)));
                }
                meta.lore(loreComponents);
            }

            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return text != null ? text : "";
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }
}
