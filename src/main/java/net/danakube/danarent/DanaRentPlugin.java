package net.danakube.danarent;

import net.danakube.danarent.api.DanaRentAPI;
import net.danakube.danarent.config.ConfigManager;
import net.danakube.danarent.config.MessageManager;
import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.database.DatabaseConnection;
import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.economy.EconomyHook;
import net.danakube.danarent.economy.EscrowService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;

public class DanaRentPlugin extends JavaPlugin implements DanaRentAPI {

    private static DanaRentPlugin instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseConnection databaseConnection;
    private PlotRepository plotRepository;
    private ContractRepository contractRepository;
    private EconomyHook economyHook;
    private EscrowService escrowService;
    private net.danakube.danarent.wand.SelectionManager selectionManager;

    public static DanaRentPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialisation des configurations YAML
        saveDefaultConfig();
        this.configManager = new ConfigManager(getConfig());

        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultMessagesStream = getResource("messages.yml");
        if (defaultMessagesStream != null) {
            messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultMessagesStream, StandardCharsets.UTF_8)));
        }
        this.messageManager = new MessageManager(messagesConfig);

        // 2. Initialisation de la base de données SQLite & Pool HikariCP
        try {
            this.databaseConnection = new DatabaseConnection(
                    getDataFolder(),
                    configManager.getDatabaseFile(),
                    configManager.getMaxPoolSize(),
                    configManager.getConnectionTimeout()
            );
            this.databaseConnection.initDatabase();
            getLogger().info("Base de données SQLite initialisée avec succès.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Impossible d'initialiser la base de données SQLite DanaRent !", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialisation des Repositories
        this.plotRepository = new PlotRepository(databaseConnection);
        this.contractRepository = new ContractRepository(databaseConnection);

        // 4. Initialisation du Hook Vault & Service d'Escrow
        this.economyHook = new EconomyHook();
        if (this.economyHook.setupEconomy()) {
            getLogger().info("Intégration Vault Economy détectée et activée.");
        } else {
            getLogger().warning("Vault Economy non disponible au démarrage de DanaRent. Les fonctionnalités financières attendront son chargement.");
        }
        this.escrowService = new EscrowService(economyHook, plotRepository, contractRepository);

        // 5. Initialisation du gestionnaire de sélection et écouteurs Wand
        this.selectionManager = new net.danakube.danarent.wand.SelectionManager();
        getServer().getPluginManager().registerEvents(new net.danakube.danarent.wand.WandListener(this, selectionManager), this);

        getLogger().info("DanaRent v" + getDescription().getVersion() + " activé avec succès.");
    }

    @Override
    public void onDisable() {
        if (databaseConnection != null) {
            databaseConnection.close();
            databaseConnection = null;
        }

        instance = null;
        getLogger().info("DanaRent désactivé proprement.");
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public MessageManager getMessageManager() {
        return messageManager;
    }

    @Override
    public PlotRepository getPlotRepository() {
        return plotRepository;
    }

    @Override
    public ContractRepository getContractRepository() {
        return contractRepository;
    }

    @Override
    public EscrowService getEscrowService() {
        return escrowService;
    }

    @Override
    public net.danakube.danarent.wand.SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }
}
