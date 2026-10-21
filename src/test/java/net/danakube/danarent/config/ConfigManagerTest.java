package net.danakube.danarent.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigManagerTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        String yaml = """
            database:
              file: "test_data.db"
              pool:
                maximum-pool-size: 8
                connection-timeout: 25000
            settings:
              currency-symbol: "⛀"
              boundary-buffer: 1.5
              particles:
                warning-color: "#FFA500"
                border-color: "#00FF00"
            limits:
              default-plots-per-island: 5
            """;
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        configManager = new ConfigManager(yamlConfig);
    }

    @Test
    @DisplayName("Devrait charger correctement les paramètres de configuration")
    void shouldLoadConfigurationValues() {
        assertThat(configManager.getDatabaseFile()).isEqualTo("test_data.db");
        assertThat(configManager.getMaxPoolSize()).isEqualTo(8);
        assertThat(configManager.getConnectionTimeout()).isEqualTo(25000L);
        assertThat(configManager.getCurrencySymbol()).isEqualTo("⛀");
        assertThat(configManager.getBoundaryBuffer()).isEqualTo(1.5);
        assertThat(configManager.getWarningColorHex()).isEqualTo("#FFA500");
        assertThat(configManager.getBorderColorHex()).isEqualTo("#00FF00");
        assertThat(configManager.getDefaultPlotsPerIsland()).isEqualTo(5);
    }

    @Test
    @DisplayName("Devrait appliquer des valeurs par défaut sécurisées en cas de configuration partielle")
    void shouldUseSafeDefaultsWhenMissing() {
        YamlConfiguration emptyConfig = new YamlConfiguration();
        ConfigManager fallbackManager = new ConfigManager(emptyConfig);

        assertThat(fallbackManager.getDatabaseFile()).isEqualTo("data.db");
        assertThat(fallbackManager.getMaxPoolSize()).isEqualTo(5);
        assertThat(fallbackManager.getConnectionTimeout()).isEqualTo(30000L);
        assertThat(fallbackManager.getCurrencySymbol()).isEqualTo("⛀");
        assertThat(fallbackManager.getBoundaryBuffer()).isEqualTo(1.0);
        assertThat(fallbackManager.getDefaultPlotsPerIsland()).isEqualTo(3);
    }
}
