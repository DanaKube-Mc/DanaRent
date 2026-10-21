package net.danakube.danarent.rental;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class HarvestListenerTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private HarvestListener harvestListener;

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
    @DisplayName("Devrait autoriser le locataire à récolter une culture mûre, annuler le drop par défaut et replanter")
    void shouldAllowTenantToHarvestMatureCropAndAutoReplant() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "ChampBle", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.WHEAT);
        Ageable ageable = org.mockito.Mockito.mock(Ageable.class);
        org.mockito.Mockito.when(ageable.getMaterial()).thenReturn(Material.WHEAT);
        org.mockito.Mockito.when(ageable.getAge()).thenReturn(7);
        org.mockito.Mockito.when(ageable.getMaximumAge()).thenReturn(7);
        block.setBlockData(ageable);

        BlockBreakEvent event = new BlockBreakEvent(block, tenant);
        event.setCancelled(true); // Simule SSB2 qui bloque le visiteur

        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse(); // Bypassé par DanaRent !
        assertThat(event.isDropItems()).isFalse(); // Drop géré par DanaRent

        // Exécute les tâches pour vérifier le replantage
        server.getScheduler().performOneTick();
        org.mockito.Mockito.verify(ageable).setAge(0);
    }

    @Test
    @DisplayName("Devrait bloquer le locataire s'il tente de casser une culture non mûre")
    void shouldBlockTenantFromBreakingUnripeCrop() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "ChampBle", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.WHEAT);
        Ageable ageable = org.mockito.Mockito.mock(Ageable.class);
        org.mockito.Mockito.when(ageable.getMaterial()).thenReturn(Material.WHEAT);
        org.mockito.Mockito.when(ageable.getAge()).thenReturn(2); // Non mûr (max = 7)
        org.mockito.Mockito.when(ageable.getMaximumAge()).thenReturn(7);
        block.setBlockData(ageable);

        BlockBreakEvent event = new BlockBreakEvent(block, tenant);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait bloquer le locataire s'il tente de casser de la terre ou un bloc non-culture en mode FARM")
    void shouldBlockTenantFromBreakingNonCropBlockInFarm() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "ChampBle", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 60, 5);
        block.setType(Material.FARMLAND);

        BlockBreakEvent event = new BlockBreakEvent(block, tenant);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait interdire à un tiers ou au propriétaire de casser dans la parcelle louée")
    void shouldBlockThirdPartyFromBreakingBlocksDuringActiveLease() {
        PlayerMock owner = server.addPlayer();
        PlayerMock tenant = server.addPlayer();
        World world = owner.getWorld();

        RentPlot plot = new RentPlot(1, owner.getUniqueId(), "island-1", "ChampBle", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.WHEAT);

        BlockBreakEvent event = new BlockBreakEvent(block, owner);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
        String msg = owner.nextMessage();
        assertThat(msg).contains("actuellement louée");
    }

    @Test
    @DisplayName("Devrait autoriser le propriétaire à casser quand aucun bail n'est actif")
    void shouldAllowOwnerToBreakBlocksWhenNoActiveLease() {
        PlayerMock owner = server.addPlayer();
        World world = owner.getWorld();

        RentPlot plot = new RentPlot(1, owner.getUniqueId(), "island-1", "ChampBle", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.WHEAT);

        BlockBreakEvent event = new BlockBreakEvent(block, owner);
        event.setCancelled(false);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Devrait autoriser le locataire à miner un matériau autorisé en mode MINING")
    void shouldAllowTenantToMineWhitelistedBlocksInMiningPlot() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(2, UUID.randomUUID(), "island-1", "Carriere", PlotType.MINING,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(Material.COBBLESTONE, Material.DIAMOND_ORE), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(2, 2, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.DIAMOND_ORE);

        BlockBreakEvent event = new BlockBreakEvent(block, tenant);
        event.setCancelled(true); // Simule SSB2 qui bloque le visiteur

        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Devrait bloquer le locataire s'il mine un matériau non autorisé en mode MINING")
    void shouldBlockTenantFromMiningNonWhitelistedBlocksInMiningPlot() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(2, UUID.randomUUID(), "island-1", "Carriere", PlotType.MINING,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(Material.COBBLESTONE), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(2, 2, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        block.setType(Material.STONE); // Non whitelisté

        BlockBreakEvent event = new BlockBreakEvent(block, tenant);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
        String msg = tenant.nextMessage();
        assertThat(msg).contains("pas autorisé à casser ce type de bloc");
    }

    @Test
    @DisplayName("Devrait mapper correctement chaque culture avec son matériau de replantation (Graine/Produit)")
    void shouldMapCropsToSeedsCorrectly() {
        assertThat(HarvestListener.getSeedForCrop(Material.WHEAT)).isEqualTo(Material.WHEAT_SEEDS);
        assertThat(HarvestListener.getSeedForCrop(Material.CARROTS)).isEqualTo(Material.CARROT);
        assertThat(HarvestListener.getSeedForCrop(Material.POTATOES)).isEqualTo(Material.POTATO);
        assertThat(HarvestListener.getSeedForCrop(Material.BEETROOTS)).isEqualTo(Material.BEETROOT_SEEDS);
        assertThat(HarvestListener.getSeedForCrop(Material.NETHER_WART)).isEqualTo(Material.NETHER_WART);
        assertThat(HarvestListener.getSeedForCrop(Material.COCOA)).isEqualTo(Material.COCOA_BEANS);
        assertThat(HarvestListener.getSeedForCrop(Material.OAK_LOG)).isNull();
    }
}
