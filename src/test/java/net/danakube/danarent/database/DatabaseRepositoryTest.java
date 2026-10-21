package net.danakube.danarent.database;

import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.model.TransactionLog;
import net.danakube.danarent.model.TransactionType;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseRepositoryTest {

    private DatabaseConnection databaseConnection;
    private PlotRepository plotRepository;
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() throws Exception {
        // Base SQLite en mémoire pour isolation et rapidité absolue
        databaseConnection = new DatabaseConnection("jdbc:sqlite::memory:", 1, 10000);
        databaseConnection.initDatabase();

        plotRepository = new PlotRepository(databaseConnection);
        contractRepository = new ContractRepository(databaseConnection);
    }

    @AfterEach
    void tearDown() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }

    @Test
    @DisplayName("Devrait sauvegarder, récupérer et mettre à jour une parcelle RentPlot")
    void shouldSaveAndRetrievePlot() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        RentPlot plot = new RentPlot(
                0, owner, "island-123", "Champ de Blé", PlotType.FARM,
                "world", 0, 60, 0, 10, 70, 10,
                150.0, 3600, Set.of(Material.WHEAT, Material.CARROTS),
                true, 0.0, System.currentTimeMillis()
        );

        int generatedId = plotRepository.save(plot).get();
        assertThat(generatedId).isPositive();

        Optional<RentPlot> retrieved = plotRepository.findById(generatedId).get();
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Champ de Blé");
        assertThat(retrieved.get().getPlotType()).isEqualTo(PlotType.FARM);
        assertThat(retrieved.get().getAllowedMaterials()).containsExactlyInAnyOrder(Material.WHEAT, Material.CARROTS);
        assertThat(retrieved.get().isListed()).isTrue();

        // Test update
        retrieved.get().setPriceLumens(200.0);
        retrieved.get().setPendingRevenue(75.5);
        plotRepository.save(retrieved.get()).get();

        Optional<RentPlot> updated = plotRepository.findById(generatedId).get();
        assertThat(updated).isPresent();
        assertThat(updated.get().getPriceLumens()).isEqualTo(200.0);
        assertThat(updated.get().getPendingRevenue()).isEqualTo(75.5);
    }

    @Test
    @DisplayName("Devrait filtrer les parcelles listées sur le marché et par île")
    void shouldFilterPlotsByIslandAndListing() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        RentPlot p1 = new RentPlot(0, owner, "island-1", "P1", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 100, 3600, Set.of(), true, 0, System.currentTimeMillis());
        RentPlot p2 = new RentPlot(0, owner, "island-1", "P2", PlotType.MINING, "world", 10, 0, 0, 15, 5, 5, 150, 3600, Set.of(), false, 0, System.currentTimeMillis());
        RentPlot p3 = new RentPlot(0, UUID.randomUUID(), "island-2", "P3", PlotType.FARM, "world", 20, 0, 0, 25, 5, 5, 200, 3600, Set.of(), true, 0, System.currentTimeMillis());

        plotRepository.save(p1).get();
        plotRepository.save(p2).get();
        plotRepository.save(p3).get();

        List<RentPlot> island1Plots = plotRepository.findByIslandId("island-1").get();
        assertThat(island1Plots).hasSize(2);

        List<RentPlot> listedPlots = plotRepository.findListedPlots().get();
        assertThat(listedPlots).hasSize(2); // P1 et P3
    }

    @Test
    @DisplayName("Devrait gérer le cycle complet d'un ActiveContract et les logs de transaction")
    void shouldManageContractLifecycleAndLogs() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "P1", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 100, 3600, Set.of(), true, 0, System.currentTimeMillis());
        int plotId = plotRepository.save(plot).get();

        long now = System.currentTimeMillis();
        ActiveContract contract = new ActiveContract(0, plotId, tenant, now, now + 3600000L, 100.0, 100.0, ContractStatus.ACTIVE);
        int contractId = contractRepository.save(contract).get();
        assertThat(contractId).isPositive();

        Optional<ActiveContract> activeByPlot = contractRepository.findActiveByPlotId(plotId).get();
        assertThat(activeByPlot).isPresent();
        assertThat(activeByPlot.get().getTenantUuid()).isEqualTo(tenant);
        assertThat(activeByPlot.get().getEscrowBalance()).isEqualTo(100.0);

        // Update escrow balance
        contractRepository.updateEscrowBalance(contractId, 40.0).get();
        Optional<ActiveContract> updatedContract = contractRepository.findById(contractId).get();
        assertThat(updatedContract).isPresent();
        assertThat(updatedContract.get().getEscrowBalance()).isEqualTo(40.0);

        // Update status to COMPLETED
        contractRepository.updateStatus(contractId, ContractStatus.COMPLETED).get();
        Optional<ActiveContract> completedCheck = contractRepository.findActiveByPlotId(plotId).get();
        assertThat(completedCheck).isEmpty(); // Plus de contrat ACTIF pour ce plot

        // Test logs
        TransactionLog log1 = new TransactionLog(0, contractId, tenant, owner, 100.0, TransactionType.ESCROW_LOCK, now);
        TransactionLog log2 = new TransactionLog(0, contractId, tenant, tenant, 40.0, TransactionType.PRORATA_REFUND, now + 1000);
        contractRepository.logTransaction(log1).get();
        contractRepository.logTransaction(log2).get();

        List<TransactionLog> logs = contractRepository.getTransactionLogs(contractId).get();
        assertThat(logs).hasSize(2);
    }
}
