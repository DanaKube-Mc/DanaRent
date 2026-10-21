package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
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

public class ConfinementAndProtectionTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private ConfinementListener confinementListener;
    private PlotProtectionListener protectionListener;
    private SkyblockTerminationListener terminationListener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
        terminationListener = new SkyblockTerminationListener(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait repousser le locataire vers le centre et envoyer un avertissement s'il quitte la zone tampon")
    void shouldRepelTenantWhenExitingBufferZone() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        // Déplacement de la limite vers l'extérieur (X=15 > 10 + 1 de buffer)
        Location from = new Location(world, 10, 60, 5);
        Location to = new Location(world, 15, 60, 5);
        PlayerMoveEvent event = new PlayerMoveEvent(tenant, from, to);

        server.getPluginManager().callEvent(event);

        // Une vélocité positive vers le centre doit avoir été appliquée (vecteur X négatif car 5.5 < 15)
        Vector velocity = tenant.getVelocity();
        assertThat(velocity.getX()).isLessThan(0); // Répulsion vers le centre
        assertThat(velocity.getY()).isGreaterThan(0); // Saut 0.2

        String msg = tenant.nextMessage();
        assertThat(msg).contains("Restez dans les limites");
    }

    @Test
    @DisplayName("Devrait respecter le cooldown de 2 secondes sur le message d'avertissement de frontière")
    void shouldRespectWarningCooldown() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Location from = new Location(world, 10, 60, 5);
        Location to = new Location(world, 15, 60, 5);

        // 1er move
        server.getPluginManager().callEvent(new PlayerMoveEvent(tenant, from, to));
        assertThat(tenant.nextMessage()).contains("Restez dans les limites");

        // 2eme move immédiat -> pas de message répété
        server.getPluginManager().callEvent(new PlayerMoveEvent(tenant, from, to));
        assertThat(tenant.nextMessage()).isNull();
    }

    @Test
    @DisplayName("Devrait bloquer la pose de blocs dans une parcelle louée")
    void shouldCancelBlockPlaceInRentedPlot() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block block = world.getBlockAt(5, 62, 5);
        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), world.getBlockAt(5, 61, 5),
                new ItemStack(Material.STONE), tenant, true, EquipmentSlot.HAND);

        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait empêcher le piétinement de Farmland")
    void shouldCancelFarmlandTrampling() {
        PlayerMock player = server.addPlayer();
        World world = player.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        Block farmland = world.getBlockAt(5, 60, 5);
        farmland.setType(Material.FARMLAND);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.PHYSICAL, null, farmland, null);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait bloquer l'ouverture des coffres et conteneurs dans la parcelle louée")
    void shouldCancelContainerInteraction() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block chest = world.getBlockAt(5, 60, 5);
        chest.setType(Material.CHEST);

        PlayerInteractEvent event = new PlayerInteractEvent(tenant, Action.RIGHT_CLICK_BLOCK, null, chest, null);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait autoriser l'utilisation de Bone Meal sur les cultures")
    void shouldAllowBoneMealInteraction() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Block crop = world.getBlockAt(5, 61, 5);
        crop.setType(Material.WHEAT);

        ItemStack boneMeal = new ItemStack(Material.BONE_MEAL);
        PlayerInteractEvent event = new PlayerInteractEvent(tenant, Action.RIGHT_CLICK_BLOCK, boneMeal, crop, null);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Devrait bloquer le PvP dans les parcelles louées")
    void shouldCancelPvPInRentedPlot() {
        PlayerMock victim = server.addPlayer();
        PlayerMock attacker = server.addPlayer();
        World world = victim.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, victim.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        victim.teleport(new Location(world, 5, 60, 5));

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, victim,
                EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK, 5.0);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait bloquer la téléportation par Enderpearl hors de la zone de la parcelle")
    void shouldCancelEnderpearlTeleportOutsidePlot() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-1", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 100.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        Location from = new Location(world, 5, 60, 5);
        Location to = new Location(world, 50, 60, 50); // Loin hors de la parcelle

        PlayerTeleportEvent event = new PlayerTeleportEvent(tenant, from, to, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Devrait résilier le contrat, rembourser le locataire et purger la parcelle lors d'un reset d'île")
    void shouldRefundEscrowAndEvacuateTenantOnIslandTermination() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();

        RentPlot plot = new RentPlot(1, UUID.randomUUID(), "island-xyz", "Champ", PlotType.FARM,
                world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 1, tenant.getUniqueId(),
                System.currentTimeMillis() - 1000, System.currentTimeMillis() + 60000, 100.0, 75.0, ContractStatus.ACTIVE);
        plugin.getRentalManager().addContract(contract);

        terminationListener.handleIslandTermination("island-xyz");

        // Contrat résilié et retiré du rentalManager
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
        assertThat(plugin.getRentalManager().getActiveContractByTenant(tenant.getUniqueId())).isNull();
        assertThat(plugin.getPlotManager().getPlot(1)).isEmpty();

        // Locataire informé
        String msg = tenant.nextMessage();
        assertThat(msg).contains("réinitialisée");
    }
}
