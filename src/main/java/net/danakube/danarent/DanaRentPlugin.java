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
    private net.danakube.danarent.plot.PlotManager plotManager;
    private net.danakube.danarent.plot.PlotValidator plotValidator;
    private net.danakube.danarent.hook.SkyblockHook skyblockHook;
    private net.danakube.danarent.visual.ParticleVisualizer particleVisualizer;
    private net.danakube.danarent.rental.RentalManager rentalManager;

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

        // 6. Initialisation de la gestion spatiale, validation et hook Skyblock
        this.plotManager = new net.danakube.danarent.plot.PlotManager(plotRepository);
        this.skyblockHook = new net.danakube.danarent.hook.SuperiorSkyblockHook();
        this.plotValidator = new net.danakube.danarent.plot.PlotValidator(plotManager, skyblockHook, configManager);
        this.particleVisualizer = new net.danakube.danarent.visual.ParticleVisualizer();

        // 7. Enregistrement des commandes
        org.bukkit.command.PluginCommand farmCmd = getCommand("farm");
        if (farmCmd != null) {
            net.danakube.danarent.command.FarmCommand fc = new net.danakube.danarent.command.FarmCommand(this);
            farmCmd.setExecutor(fc);
            farmCmd.setTabCompleter(fc);
        }

        // 8. Initialisation du gestionnaire de location et tâche périodique 20t
        this.rentalManager = new net.danakube.danarent.rental.RentalManager(contractRepository);
        getServer().getScheduler().runTaskTimer(this, new net.danakube.danarent.rental.RentalTimerTask(this), 20L, 20L);

        // 9. Écouteurs de récolte & bypass SSB2
        getServer().getPluginManager().registerEvents(new net.danakube.danarent.rental.HarvestListener(this), this);

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

    @Override
    public net.danakube.danarent.plot.PlotManager getPlotManager() {
        return plotManager;
    }

    @Override
    public net.danakube.danarent.plot.PlotValidator getPlotValidator() {
        return plotValidator;
    }

    @Override
    public net.danakube.danarent.hook.SkyblockHook getSkyblockHook() {
        return skyblockHook;
    }

    public void setSkyblockHook(net.danakube.danarent.hook.SkyblockHook skyblockHook) {
        this.skyblockHook = skyblockHook;
        if (plotManager != null && configManager != null) {
            this.plotValidator = new net.danakube.danarent.plot.PlotValidator(plotManager, skyblockHook, configManager);
        }
    }

    @Override
    public net.danakube.danarent.visual.ParticleVisualizer getParticleVisualizer() {
        return particleVisualizer;
    }

    @Override
    public net.danakube.danarent.rental.RentalManager getRentalManager() {
        return rentalManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }
}
