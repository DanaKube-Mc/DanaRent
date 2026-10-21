package net.danakube.danarent.gui;

import net.danakube.danarent.DanaRentPlugin;
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

public class ManageGUITest {

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
    @DisplayName("Devrait afficher les parcelles de l'île et le coffre des gains accumulés")
    void shouldDisplayOwnerPlotsAndPendingRevenue() {
        PlayerMock owner = server.addPlayer();
        World world = owner.getWorld();

        RentPlot plot1 = new RentPlot(1, owner.getUniqueId(), "island-1", "MonChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), true, 250.0, System.currentTimeMillis());
        RentPlot otherPlot = new RentPlot(2, UUID.randomUUID(), "island-2", "AutreChamp", PlotType.FARM,
                world.getName(), 20, 60, 20, 30, 70, 30, 100.0, 3600, Set.of(), true, 50.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot1);
        plugin.getPlotManager().addPlot(otherPlot);

        ManageGUI gui = new ManageGUI(plugin, owner, 1);
        gui.open(owner);

        Inventory inv = gui.getInventory();

        // Coffre des gains au slot 4
        ItemStack chest = inv.getItem(4);
        assertThat(chest).isNotNull();
        assertThat(chest.getType()).isEqualTo(Material.CHEST);

        // Seule la parcelle du propriétaire est affichée au slot 10
        ItemStack card = inv.getItem(10);
        assertThat(card).isNotNull();
        assertThat(card.getType()).isEqualTo(Material.HAY_BLOCK);

        // Slot 11 doit être vide car otherPlot appartient à un autre joueur
        assertThat(inv.getItem(11)).isNull();
    }

    @Test
    @DisplayName("Devrait basculer la visibilité publique dans PlotAdminGUI")
    void shouldToggleListedInPlotAdminGUI() {
        PlayerMock owner = server.addPlayer();
        World world = owner.getWorld();

        RentPlot plot = new RentPlot(1, owner.getUniqueId(), "island-1", "MonChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), false, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        PlotAdminGUI gui = new PlotAdminGUI(plugin, owner, plot);
        gui.open(owner);

        // Slot 13 : toggle listed
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                owner.getOpenInventory(), InventoryType.SlotType.CONTAINER, 13, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        assertThat(clickEvent.isCancelled()).isTrue();
        assertThat(plot.isListed()).isTrue();
    }

    @Test
    @DisplayName("Devrait ajuster le prix de location dans PlotAdminGUI")
    void shouldAdjustPriceInPlotAdminGUI() {
        PlayerMock owner = server.addPlayer();
        World world = owner.getWorld();

        RentPlot plot = new RentPlot(1, owner.getUniqueId(), "island-1", "MonChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), false, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        PlotAdminGUI gui = new PlotAdminGUI(plugin, owner, plot);
        gui.open(owner);

        // Clic gauche sur le slot 11 (+10 Lumens)
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                owner.getOpenInventory(), InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        assertThat(plot.getPriceLumens()).isEqualTo(110.0);
    }
}
