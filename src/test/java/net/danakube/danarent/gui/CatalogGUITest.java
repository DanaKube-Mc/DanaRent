package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.economy.EconomyHook;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogGUITest {

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
    @DisplayName("Devrait afficher les parcelles listées avec bordures et pagination")
    void shouldDisplayListedPlotsWithCorrectBordersAndPagination() {
        PlayerMock player = server.addPlayer();
        World world = player.getWorld();

        RentPlot plot1 = new RentPlot(1, UUID.randomUUID(), "island-1", "ChampBlé", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis());
        RentPlot plot2 = new RentPlot(2, UUID.randomUUID(), "island-1", "Carrière", PlotType.MINING,
                world.getName(), 20, 60, 20, 30, 70, 30, 200.0, 3600, Set.of(), false, 0.0, System.currentTimeMillis()); // Non listé
        plugin.getPlotManager().addPlot(plot1);
        plugin.getPlotManager().addPlot(plot2);

        CatalogGUI gui = new CatalogGUI(plugin, player, 1);
        gui.open(player);

        Inventory inv = gui.getInventory();

        // Coins (Black stained glass pane)
        assertThat(inv.getItem(0).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        assertThat(inv.getItem(8).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        assertThat(inv.getItem(45).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        assertThat(inv.getItem(53).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);

        // Premier slot utilisable (10) contient plot1
        ItemStack card = inv.getItem(10);
        assertThat(card).isNotNull();
        assertThat(card.getType()).isEqualTo(Material.HAY_BLOCK);

        // Deuxième slot utilisable (11) est vide car plot2 n'est pas listé
        assertThat(inv.getItem(11)).isNull();
    }

    @Test
    @DisplayName("Devrait téléporter en inspection (1 bloc dehors) au clic droit sur une carte")
    void shouldInspectPlotOnRightClick() {
        PlayerMock player = server.addPlayer();
        World world = player.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "ChampBlé", PlotType.FARM,
                world.getName(), 10, 60, 10, 20, 70, 20, 100.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        CatalogGUI gui = new CatalogGUI(plugin, player, 1);
        gui.open(player);

        // Clic droit sur le slot 10
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, ClickType.RIGHT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        assertThat(clickEvent.isCancelled()).isTrue();

        // Vérifier que le joueur a été téléporté à l'extérieur (Z = 10 - 1 = 9)
        assertThat(player.getLocation().getBlockZ()).isEqualTo(9);
        String msg = player.nextMessage();
        assertThat(msg).contains("inspection");
    }
}
