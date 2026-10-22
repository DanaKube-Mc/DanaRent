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
    private int defaultCreationLimit;
    private int defaultActiveRentalsLimit;

    private boolean farmEnabled;
    private boolean miningEnabled;

    private double taxPercentage;
    private String taxMode;

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

        this.currencySymbol = config.getString("economy.currency-symbol", config.getString("settings.currency-symbol", "⛀"));
        this.boundaryBuffer = config.getDouble("settings.boundary-buffer", 1.0);
        this.warningColorHex = config.getString("settings.particles.warning-color", "#FFA500");
        this.borderColorHex = config.getString("settings.particles.border-color", "#00FF00");

        this.farmEnabled = config.getBoolean("types.farm.enabled", true);
        this.miningEnabled = config.getBoolean("types.mining.enabled", true);

        this.taxPercentage = config.getDouble("economy.tax.percentage", 10.0);
        this.taxMode = config.getString("economy.tax.mode", "MONEY_SINK");

        this.defaultCreationLimit = config.getInt("limits.creation.default", config.getInt("limits.default-plots-per-island", 3));
        this.defaultActiveRentalsLimit = config.getInt("limits.active-rentals.default", 1);
        this.defaultPlotsPerIsland = this.defaultCreationLimit;

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

        this.farmEnabled = true;
        this.miningEnabled = true;

        this.taxPercentage = 10.0;
        this.taxMode = "MONEY_SINK";

        this.defaultCreationLimit = 3;
        this.defaultActiveRentalsLimit = 1;
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

    public int getDefaultCreationLimit() {
        return defaultCreationLimit;
    }

    public int getDefaultActiveRentalsLimit() {
        return defaultActiveRentalsLimit;
    }

    public boolean isFarmEnabled() {
        return farmEnabled;
    }

    public boolean isMiningEnabled() {
        return miningEnabled;
    }

    public boolean isTypeEnabled(net.danakube.danarent.model.PlotType type) {
        if (type == null) return false;
        return switch (type) {
            case FARM -> farmEnabled;
            case MINING -> miningEnabled;
        };
    }

    public double getTaxPercentage() {
        return taxPercentage;
    }

    public String getTaxMode() {
        return taxMode;
    }
}
