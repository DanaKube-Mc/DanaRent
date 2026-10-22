package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.gui.CatalogGUI;
import net.danakube.danarent.gui.PlotAdminGUI;
import net.danakube.danarent.hook.SkyblockHook;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.wand.PlayerSelection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeatureTogglesAndLimitsTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private SkyblockHook skyblockHook;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);

        skyblockHook = mock(SkyblockHook.class);
        when(skyblockHook.hasIsland(any())).thenReturn(true);
        when(skyblockHook.getIslandId(any())).thenReturn("island-1");
        when(skyblockHook.isIslandAdminOrOwner(any(), anyString())).thenReturn(true);
        when(skyblockHook.isCuboidFullyInsideIsland(any(), anyString())).thenReturn(true);
        plugin.setSkyblockHook(skyblockHook);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait refuser /farm create si le type de parcelle est désactivé")
    void shouldRejectCreateWhenTypeDisabled() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "danarent.use", true);
        player.addAttachment(plugin, "danarent.owner", true);

        // Désactiver MINING dans la configuration
        plugin.getConfig().set("types.mining.enabled", false);
        plugin.getConfigManager().reload(plugin.getConfig());

        World world = player.getWorld();
        PlayerSelection sel = plugin.getSelectionManager().getOrCreateSelection(player.getUniqueId());
        sel.setPos1(new Location(world, 0, 60, 0));
        sel.setPos2(new Location(world, 10, 70, 10));

        player.performCommand("farm create MaCarriere mining");

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("désactivé");
    }

    @Test
    @DisplayName("Devrait bloquer le basculement de type dans PlotAdminGUI si le type cible est désactivé")
    void shouldBlockTypeToggleWhenTargetDisabled() {
        PlayerMock owner = server.addPlayer();
        World world = owner.getWorld();

        // Désactiver MINING
        plugin.getConfig().set("types.mining.enabled", false);
        plugin.getConfigManager().reload(plugin.getConfig());

        RentPlot plot = new RentPlot(1, owner.getUniqueId(), "island-1", "MonChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), false, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        PlotAdminGUI gui = new PlotAdminGUI(plugin, owner, plot);
        gui.open(owner);

        // Clic sur le slot 10 (toggle type vers MINING)
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                owner.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        assertThat(plot.getPlotType()).isEqualTo(PlotType.FARM);
        String msg = owner.nextMessage();
        assertThat(msg).isNotNull().contains("désactivé");
    }

    @Test
    @DisplayName("Devrait refuser la location dans CatalogGUI si le type de parcelle est désactivé")
    void shouldBlockRentalWhenTypeDisabledInCatalog() {
        PlayerMock player = server.addPlayer();
        World world = player.getWorld();

        plugin.getConfig().set("types.mining.enabled", false);
        plugin.getConfigManager().reload(plugin.getConfig());

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-2", "Carriere", PlotType.MINING,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        CatalogGUI gui = new CatalogGUI(plugin, player, 1);
        gui.open(player);

        // Clic gauche sur le slot 10
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        String msg = player.nextMessage();
        assertThat(msg).isNotNull().contains("désactivé");
    }

    @Test
    @DisplayName("Devrait bloquer la location dans CatalogGUI si la limite de baux simultanés est atteinte")
    void shouldEnforceMaxActiveRentalsQuota() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        // Par défaut limits.active-rentals.default = 1
        ActiveContract existing = new ActiveContract(1, 10, tenant.getUniqueId(),
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(existing);

        RentPlot plot2 = new RentPlot(2, UUID.randomUUID(), "island-2", "DeuxiemeChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot2);

        CatalogGUI gui = new CatalogGUI(plugin, tenant, 1);
        gui.open(tenant);

        // Clic gauche sur le slot 10
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                tenant.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        String msg = tenant.nextMessage();
        assertThat(msg).isNotNull().contains("limite de baux simultanés");
    }

    @Test
    @DisplayName("Devrait autoriser plusieurs locations simultanées si la permission danarent.limit.rent.2 est accordée")
    void shouldAllowMultipleRentalsWithPermission() throws InterruptedException {
        PlayerMock tenant = server.addPlayer();
        tenant.addAttachment(plugin, "danarent.limit.rent.2", true);
        World world = tenant.getWorld();

        // 1 contrat actif existant
        ActiveContract existing = new ActiveContract(1, 10, tenant.getUniqueId(),
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(existing);

        net.danakube.danarent.economy.EconomyHook mockHook = mock(net.danakube.danarent.economy.EconomyHook.class);
        when(mockHook.hasEnough(any(), org.mockito.ArgumentMatchers.anyDouble())).thenReturn(true);
        when(mockHook.withdraw(any(), org.mockito.ArgumentMatchers.anyDouble())).thenReturn(true);
        plugin.setEconomyHook(mockHook);

        RentPlot plot2 = new RentPlot(2, UUID.randomUUID(), "island-2", "DeuxiemeChamp", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100.0, 3600, Set.of(), true, 0.0, System.currentTimeMillis());
        plugin.getPlotRepository().save(plot2).join();
        plugin.getPlotManager().addPlot(plot2);

        CatalogGUI gui = new CatalogGUI(plugin, tenant, 1);
        gui.open(tenant);

        // Clic gauche sur le slot 10 (2ème location)
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                tenant.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        for (int i = 0; i < 40; i++) {
            if (plugin.getRentalManager().getActiveRentalCount(tenant.getUniqueId()) == 2) {
                break;
            }
            Thread.sleep(50);
        }

        assertThat(plugin.getRentalManager().getActiveRentalCount(tenant.getUniqueId())).isEqualTo(2);
    }
}
