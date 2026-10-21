package net.danakube.danarent.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final MiniMessage miniMessage;
    private final Map<String, String> rawMessages;
    private String prefix;

    public MessageManager(ConfigurationSection config) {
        this.miniMessage = MiniMessage.miniMessage();
        this.rawMessages = new HashMap<>();
        reload(config);
    }

    public void reload(ConfigurationSection config) {
        rawMessages.clear();
        if (config == null) {
            this.prefix = "";
            return;
        }

        this.prefix = config.getString("prefix", "");
        loadKeysRecursive("", config);
    }

    private void loadKeysRecursive(String currentPath, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection != null) {
                    loadKeysRecursive(fullPath, subSection);
                }
            } else if (section.isString(key)) {
                rawMessages.put(fullPath, section.getString(key));
            }
        }
    }

    public String getRawMessage(String path) {
        return rawMessages.getOrDefault(path, path);
    }

    public Component getMessage(String path) {
        return getMessage(path, Map.of());
    }

    public Component getMessage(String path, Map<String, String> placeholders) {
        String template = rawMessages.get(path);
        if (template == null) {
            return Component.text(path);
        }

        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.parsed("prefix", prefix));

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolvers.add(Placeholder.parsed(entry.getKey(), entry.getValue() != null ? entry.getValue() : ""));
            }
        }

        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }

    public String getPrefix() {
        return prefix;
    }
}
