package net.danakube.danarent.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageManagerTest {

    private MessageManager messageManager;

    @BeforeEach
    void setUp() {
        String yaml = """
            prefix: "<gradient:#F82F04:#FFEE2E>[Location]</gradient> | "
            rental:
              started-tenant: "<prefix><green>Vous avez loue <plot_name> pour <price> Lumens!</green>"
              expired-tenant: "<prefix><yellow>Votre bail pour <plot_name> a expire.</yellow>"
            security:
              boundary-warning: "<prefix><red>Restez dans les limites !</red>"
            """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        messageManager = new MessageManager(config);
    }

    @Test
    @DisplayName("Devrait parser les messages MiniMessage avec remplacement du prefix")
    void shouldParseMiniMessageWithPrefix() {
        Component component = messageManager.getMessage("security.boundary-warning");
        String plain = PlainTextComponentSerializer.plainText().serialize(component);

        assertThat(plain).contains("[Location] |");
        assertThat(plain).contains("Restez dans les limites !");
    }

    @Test
    @DisplayName("Devrait remplacer dynamiquement les placeholders")
    void shouldReplacePlaceholdersCorrectly() {
        Component component = messageManager.getMessage("rental.started-tenant", Map.of(
            "plot_name", "Ble-01",
            "price", "250"
        ));
        String plain = PlainTextComponentSerializer.plainText().serialize(component);

        assertThat(plain).contains("Vous avez loue Ble-01 pour 250 Lumens!");
    }

    @Test
    @DisplayName("Devrait retourner une valeur de fallback élégante si la clé n'existe pas")
    void shouldReturnFallbackOnMissingKey() {
        Component component = messageManager.getMessage("unknown.key");
        String plain = PlainTextComponentSerializer.plainText().serialize(component);

        assertThat(plain).isEqualTo("unknown.key");
    }
}
