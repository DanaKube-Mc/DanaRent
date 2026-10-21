package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class GUIFrameworkTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private GUIManager guiManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
        guiManager = new GUIManager(plugin);
        guiManager.loadConfigs();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait charger toutes les configurations YAML de GUIs sans erreur")
    void shouldLoadGUIConfigsWithoutErrors() {
        assertThat(guiManager.getConfig("catalog")).isNotNull();
        assertThat(guiManager.getConfig("manage")).isNotNull();
        assertThat(guiManager.getConfig("plot_admin")).isNotNull();
        assertThat(guiManager.getConfig("block_selector")).isNotNull();

        assertThat(guiManager.getConfig("catalog").getString("title")).contains("Catalogue des Terrains");
    }

    @Test
    @DisplayName("Devrait annuler le clic (anti-vol) et exécuter l'action enregistrée sur le slot")
    void shouldCancelClickAndExecuteAction() {
        PlayerMock player = server.addPlayer();
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        AbstractGUI gui = new AbstractGUI(plugin, "<b>Test GUI</b>", 27) {
            @Override
            protected void initialize() {
                setItem(10, new ItemStack(Material.DIAMOND), event -> actionExecuted.set(true));
            }
        };

        gui.open(player);
        InventoryView view = player.getOpenInventory();

        InventoryClickEvent clickEvent = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );

        server.getPluginManager().callEvent(clickEvent);

        assertThat(clickEvent.isCancelled()).isTrue();
        assertThat(actionExecuted.get()).isTrue();
    }

    @Test
    @DisplayName("Devrait annuler le glisser-déposer (InventoryDragEvent) pour protéger l'inventaire")
    void shouldCancelDragEvent() {
        PlayerMock player = server.addPlayer();

        AbstractGUI gui = new AbstractGUI(plugin, "<b>Test GUI</b>", 27) {
            @Override
            protected void initialize() {
                setItem(0, new ItemStack(Material.STONE));
            }
        };

        gui.open(player);
        InventoryView view = player.getOpenInventory();

        InventoryDragEvent dragEvent = new InventoryDragEvent(
                view, new ItemStack(Material.DIRT), new ItemStack(Material.DIRT), false,
                java.util.Collections.singletonMap(0, new ItemStack(Material.DIRT))
        );

        server.getPluginManager().callEvent(dragEvent);

        assertThat(dragEvent.isCancelled()).isTrue();
    }
}
