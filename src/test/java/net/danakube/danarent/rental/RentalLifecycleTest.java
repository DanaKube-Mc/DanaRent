package net.danakube.danarent.rental;

import net.danakube.danarent.DanaRentPlugin;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import org.bukkit.Location;
import org.bukkit.World;
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

class RentalLifecycleTest {

    private ServerMock server;
    private DanaRentPlugin plugin;
    private RentalManager rentalManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DanaRentPlugin.class);
        rentalManager = plugin.getRentalManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Devrait enregistrer et retrouver les contrats actifs par locataire et par parcelle")
    void shouldRegisterAndRetrieveActiveContracts() {
        UUID tenant = UUID.randomUUID();
        ActiveContract contract = new ActiveContract(1, 10, tenant, System.currentTimeMillis(), System.currentTimeMillis() + 3600000L, 100.0, 100.0, ContractStatus.ACTIVE);

        rentalManager.addContract(contract);

        assertThat(rentalManager.getActiveContractByPlot(10)).isNotNull();
        assertThat(rentalManager.getActiveContractByTenant(tenant)).isNotNull();

        rentalManager.removeContract(1);
        assertThat(rentalManager.getActiveContractByPlot(10)).isNull();
        assertThat(rentalManager.getActiveContractByTenant(tenant)).isNull();
    }

    @Test
    @DisplayName("Devrait téléporter le locataire au centre de sa parcelle avec /farm return")
    void shouldTeleportTenantWithFarmReturn() {
        PlayerMock tenant = server.addPlayer();
        tenant.addAttachment(plugin, "danarent.use", true);

        // Sans contrat actif
        tenant.performCommand("farm return");
        String noContractMsg = tenant.nextMessage();
        assertThat(noContractMsg).contains("aucun bail");

        // Créer une parcelle et un contrat actif
        World world = tenant.getWorld();
        RentPlot plot = new RentPlot(10, UUID.randomUUID(), "island-1", "FermeDuNord", PlotType.FARM, world.getName(), 0, 60, 0, 10, 70, 10, 100, 3600, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        ActiveContract contract = new ActiveContract(1, 10, tenant.getUniqueId(), System.currentTimeMillis(), System.currentTimeMillis() + 3600000L, 100.0, 100.0, ContractStatus.ACTIVE);
        rentalManager.addContract(contract);

        // Téléporter le joueur loin de la parcelle
        tenant.teleport(new Location(world, 500, 70, 500));

        // Avec contrat actif
        tenant.performCommand("farm return");
        String returnMsg = tenant.nextMessage();
        assertThat(returnMsg).contains("FermeDuNord");

        // Vérifier que le joueur est au centre du cuboid (0,60,0 à 10,70,10 => centre = 5.5, 60.5, 5.5)
        assertThat(tenant.getLocation().getBlockX()).isEqualTo(5);
        assertThat(tenant.getLocation().getBlockZ()).isEqualTo(5);
    }

    @Test
    @DisplayName("Devrait résilier le bail avec /farm cancel et rembourser au prorata")
    void shouldCancelRentalWithProrata() throws InterruptedException {
        PlayerMock tenant = server.addPlayer();
        tenant.addAttachment(plugin, "danarent.use", true);

        World world = tenant.getWorld();
        RentPlot plot = new RentPlot(10, UUID.randomUUID(), "island-1", "FermeDuSud", PlotType.FARM, world.getName(), 0, 60, 0, 10, 70, 10, 200, 100, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        long now = System.currentTimeMillis();
        ActiveContract contract = new ActiveContract(1, 10, tenant.getUniqueId(), now - 50000L, now + 50000L, 200.0, 200.0, ContractStatus.ACTIVE);
        rentalManager.addContract(contract);

        tenant.performCommand("farm cancel");

        // Attente de l'exécution asynchrone d'escrowService.cancelRental
        int retries = 0;
        while (rentalManager.getActiveContractByTenant(tenant.getUniqueId()) != null && retries++ < 20) {
            Thread.sleep(50);
        }

        String cancelMsg = tenant.nextMessage();
        assertThat(cancelMsg).contains("Bail résilié");

        assertThat(rentalManager.getActiveContractByTenant(tenant.getUniqueId())).isNull();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    }

    @Test
    @DisplayName("Devrait faire expirer automatiquement les baux échus lors de la tâche périodique 20t")
    void shouldExpireContractsOnTickTask() {
        PlayerMock tenant = server.addPlayer();
        World world = tenant.getWorld();
        RentPlot plot = new RentPlot(10, UUID.randomUUID(), "island-1", "Ferme", PlotType.FARM, world.getName(), 0, 60, 0, 10, 70, 10, 100, 10, Set.of(), false, 0, System.currentTimeMillis());
        plugin.getPlotManager().addPlot(plot);

        // Contrat déjà échu (end_time dans le passé)
        long past = System.currentTimeMillis() - 5000L;
        ActiveContract contract = new ActiveContract(1, 10, tenant.getUniqueId(), past - 10000L, past, 100.0, 100.0, ContractStatus.ACTIVE);
        rentalManager.addContract(contract);

        RentalTimerTask timerTask = new RentalTimerTask(plugin);
        timerTask.run();

        // Le contrat doit être retiré du rentalManager et terminé
        assertThat(rentalManager.getActiveContractByTenant(tenant.getUniqueId())).isNull();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);

        String msg = tenant.nextMessage();
        assertThat(msg).contains("arrivé à expiration");
    }
}
