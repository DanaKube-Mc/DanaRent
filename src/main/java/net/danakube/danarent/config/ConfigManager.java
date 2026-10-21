package net.danakube.danarent.config;

import org.bukkit.configuration.ConfigurationSection;

public class ConfigManager {

    private String databaseFile;
    private int maxPoolSize;
    private long connectionTimeout;

    private String currencySymbol;
    private double boundaryBuffer;
    private String warningColorHex;
    private String borderColorHex;

    private int defaultPlotsPerIsland;

    private String wandMaterial;
    private String wandName;
    private java.util.List<String> wandLore;
    private int wandCustomModelData;

    public ConfigManager(ConfigurationSection config) {
        reload(config);
    }

    public void reload(ConfigurationSection config) {
        if (config == null) {
            applyDefaults();
            return;
        }

        this.databaseFile = config.getString("database.file", "data.db");
        this.maxPoolSize = config.getInt("database.pool.maximum-pool-size", 5);
        this.connectionTimeout = config.getLong("database.pool.connection-timeout", 30000L);

        this.currencySymbol = config.getString("settings.currency-symbol", "⛀");
        this.boundaryBuffer = config.getDouble("settings.boundary-buffer", 1.0);
        this.warningColorHex = config.getString("settings.particles.warning-color", "#FFA500");
        this.borderColorHex = config.getString("settings.particles.border-color", "#00FF00");

        this.defaultPlotsPerIsland = config.getInt("limits.default-plots-per-island", 3);

        this.wandMaterial = config.getString("wand.material", "GOLDEN_HOE");
        this.wandName = config.getString("wand.name", "<b><gradient:#F82F04:#FFEE2E>Baguette de Sélection</gradient></b>");
        this.wandLore = config.getStringList("wand.lore");
        this.wandCustomModelData = config.getInt("wand.custom-model-data", 0);
    }

    private void applyDefaults() {
        this.databaseFile = "data.db";
        this.maxPoolSize = 5;
        this.connectionTimeout = 30000L;
        this.currencySymbol = "⛀";
        this.boundaryBuffer = 1.0;
        this.warningColorHex = "#FFA500";
        this.borderColorHex = "#00FF00";
        this.defaultPlotsPerIsland = 3;

        this.wandMaterial = "GOLDEN_HOE";
        this.wandName = "<b><gradient:#F82F04:#FFEE2E>Baguette de Sélection</gradient></b>";
        this.wandLore = java.util.List.of(
                "<#c0c0c0>Délimitez votre parcelle de location.",
                "<#55ff55>Clic gauche <#c0c0c0>: Définir le <#ffffff>Coin 1",
                "<#55ff55>Clic droit  <#c0c0c0>: Définir le <#ffffff>Coin 2"
        );
        this.wandCustomModelData = 0;
    }

    public String getWandMaterial() {
        return wandMaterial;
    }

    public String getWandName() {
        return wandName;
    }

    public java.util.List<String> getWandLore() {
        return wandLore;
    }

    public int getWandCustomModelData() {
        return wandCustomModelData;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public double getBoundaryBuffer() {
        return boundaryBuffer;
    }

    public String getWarningColorHex() {
        return warningColorHex;
    }

    public String getBorderColorHex() {
        return borderColorHex;
    }

    public int getDefaultPlotsPerIsland() {
        return defaultPlotsPerIsland;
    }
}
