package net.danakube.danarent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.assertj.core.api.Assertions.assertThat;

class DanaRentPluginTest {

    private ServerMock server;
    private DanaRentPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait initialiser proprement DanaRentPlugin, l'API et les managers")
    void shouldInitializePluginAndManagers() {
        assertThat(plugin).isNotNull();
        assertThat(plugin.isEnabled()).isTrue();
        assertThat(DanaRentPlugin.getInstance()).isEqualTo(plugin);

        assertThat(plugin.getConfigManager()).isNotNull();
        assertThat(plugin.getMessageManager()).isNotNull();
        assertThat(plugin.getPlotRepository()).isNotNull();
        assertThat(plugin.getContractRepository()).isNotNull();
        assertThat(plugin.getEscrowService()).isNotNull();
    }
}
