package net.danakube.danarent.economy;

import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.database.DatabaseConnection;
import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.PlotType;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.model.TransactionLog;
import net.danakube.danarent.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EscrowServiceTest {

    private DatabaseConnection databaseConnection;
    private PlotRepository plotRepository;
    private ContractRepository contractRepository;
    private EconomyHook economyHook;
    private EscrowService escrowService;

    @BeforeEach
    void setUp() throws Exception {
        databaseConnection = new DatabaseConnection("jdbc:sqlite::memory:", 1, 10000);
        databaseConnection.initDatabase();

        plotRepository = new PlotRepository(databaseConnection);
        contractRepository = new ContractRepository(databaseConnection);
        economyHook = Mockito.mock(EconomyHook.class);

        escrowService = new EscrowService(economyHook, plotRepository, contractRepository);
    }

    @AfterEach
    void tearDown() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }

    @Test
    @DisplayName("Devrait refuser la location si le solde du locataire est insuffisant")
    void shouldRejectRentalOnInsufficientFunds() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "Ferme", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 500.0, 3600, Set.of(), true, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        when(economyHook.hasEnough(tenant, 500.0)).thenReturn(false);

        assertThatThrownBy(() -> escrowService.startRental(tenant, plot).get())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    @DisplayName("Devrait débuter un bail en séquestrant le montant total payé")
    void shouldStartRentalAndLockEscrow() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "Ferme", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 300.0, 3600, Set.of(), true, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        when(economyHook.hasEnough(tenant, 300.0)).thenReturn(true);
        when(economyHook.withdraw(tenant, 300.0)).thenReturn(true);

        ActiveContract contract = escrowService.startRental(tenant, plot).get();

        assertThat(contract).isNotNull();
        assertThat(contract.getEscrowBalance()).isEqualTo(300.0);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(plot.isListed()).isFalse();

        verify(economyHook).withdraw(tenant, 300.0);
    }

    @Test
    @DisplayName("Devrait calculer le remboursement au prorata exact lors d'une résiliation à mi-parcours")
    void shouldCalculateProrataOnEarlyCancellation() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "Ferme", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 200.0, 100, Set.of(), false, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        // Bail démarré il y a 50 secondes sur 100 secondes totales (50% consommé)
        long startTime = System.currentTimeMillis() - 50_000L;
        long endTime = startTime + 100_000L;
        ActiveContract contract = new ActiveContract(0, plot.getId(), tenant, startTime, endTime, 200.0, 200.0, ContractStatus.ACTIVE);
        contractRepository.save(contract).get();

        when(economyHook.deposit(eq(tenant), anyDouble())).thenReturn(true);

        escrowService.cancelRental(contract, plot).get();

        // 50% de 200 = 100 remboursé au locataire, 100 en pending_revenue pour le proprio
        verify(economyHook).deposit(tenant, 100.0);
        assertThat(plot.getPendingRevenue()).isEqualTo(100.0);
        assertThat(contract.getEscrowBalance()).isEqualTo(0.0);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    }

    @Test
    @DisplayName("Devrait transférer 100% des fonds séquestrés en fin normale de contrat")
    void shouldTransferAllFundsOnCompletion() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "Ferme", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 150.0, 3600, Set.of(), false, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        long now = System.currentTimeMillis();
        ActiveContract contract = new ActiveContract(0, plot.getId(), tenant, now - 3600000L, now, 150.0, 150.0, ContractStatus.ACTIVE);
        contractRepository.save(contract).get();

        escrowService.completeRental(contract, plot).get();

        assertThat(plot.getPendingRevenue()).isEqualTo(150.0);
        assertThat(contract.getEscrowBalance()).isEqualTo(0.0);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    @DisplayName("Devrait permettre au propriétaire de collecter ses revenus accumulés")
    void shouldCollectPendingRevenue() throws ExecutionException, InterruptedException {
        UUID owner = UUID.randomUUID();
        RentPlot p1 = new RentPlot(0, owner, "island-1", "P1", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 100, 3600, Set.of(), true, 120.0, System.currentTimeMillis());
        RentPlot p2 = new RentPlot(0, owner, "island-1", "P2", PlotType.MINING, "world", 10, 0, 0, 15, 5, 5, 100, 3600, Set.of(), true, 80.0, System.currentTimeMillis());
        plotRepository.save(p1).get();
        plotRepository.save(p2).get();

        when(economyHook.deposit(eq(owner), anyDouble())).thenReturn(true);

        double collected = escrowService.collectPendingRevenue(owner).get();

        assertThat(collected).isEqualTo(200.0);
        verify(economyHook).deposit(owner, 200.0);

        // Vérifier que le pending revenue est réinitialisé à 0
        assertThat(plotRepository.findById(p1.getId()).get().get().getPendingRevenue()).isEqualTo(0.0);
        assertThat(plotRepository.findById(p2.getId()).get().get().getPendingRevenue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Devrait prélever la taxe serveur (Option A - Money Sink) en fin normale de contrat")
    void shouldApplyServerTaxOnNormalCompletion() throws ExecutionException, InterruptedException {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("economy.tax.percentage", 10.0);
        config.set("economy.tax.mode", "MONEY_SINK");
        net.danakube.danarent.config.ConfigManager configManager = new net.danakube.danarent.config.ConfigManager(config);
        escrowService.setConfigManager(configManager);

        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "FermeTaxe", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 1000.0, 3600, Set.of(), false, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        long now = System.currentTimeMillis();
        ActiveContract contract = new ActiveContract(0, plot.getId(), tenant, now - 3600000L, now, 1000.0, 1000.0, ContractStatus.ACTIVE);
        int contractId = contractRepository.save(contract).get();
        contract.setId(contractId);

        escrowService.completeRental(contract, plot).get();

        // 1000 total : 10% taxe (100) détruits, 900 crédités au propriétaire
        assertThat(plot.getPendingRevenue()).isEqualTo(900.0);
        assertThat(contract.getEscrowBalance()).isEqualTo(0.0);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);

        // Vérifier les logs financiers
        List<TransactionLog> logs = contractRepository.getTransactionLogs(contractId).get();
        assertThat(logs).hasSize(2);

        TransactionLog payoutLog = logs.stream().filter(l -> l.getType() == TransactionType.OWNER_PAYOUT).findFirst().orElseThrow();
        assertThat(payoutLog.getAmount()).isEqualTo(900.0);
        assertThat(payoutLog.getTargetUuid()).isEqualTo(owner);

        TransactionLog taxLog = logs.stream().filter(l -> l.getType() == TransactionType.SERVER_TAX).findFirst().orElseThrow();
        assertThat(taxLog.getAmount()).isEqualTo(100.0);
        assertThat(taxLog.getTargetUuid()).isEqualTo(EscrowService.SERVER_SINK_UUID);
    }

    @Test
    @DisplayName("Devrait prélever la taxe serveur uniquement sur la part consommée lors d'une résiliation anticipée")
    void shouldApplyServerTaxOnlyOnConsumedShareOnEarlyCancellation() throws ExecutionException, InterruptedException {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("economy.tax.percentage", 10.0);
        config.set("economy.tax.mode", "MONEY_SINK");
        net.danakube.danarent.config.ConfigManager configManager = new net.danakube.danarent.config.ConfigManager(config);
        escrowService.setConfigManager(configManager);

        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        RentPlot plot = new RentPlot(0, owner, "island-1", "FermeTaxeProrata", PlotType.FARM, "world", 0, 0, 0, 5, 5, 5, 1000.0, 100, Set.of(), false, 0, System.currentTimeMillis());
        plotRepository.save(plot).get();

        // 50% du temps écoulé (50s sur 100s)
        long startTime = System.currentTimeMillis() - 50_000L;
        long endTime = startTime + 100_000L;
        ActiveContract contract = new ActiveContract(0, plot.getId(), tenant, startTime, endTime, 1000.0, 1000.0, ContractStatus.ACTIVE);
        int contractId = contractRepository.save(contract).get();
        contract.setId(contractId);

        when(economyHook.deposit(eq(tenant), anyDouble())).thenReturn(true);

        escrowService.cancelRental(contract, plot).get();

        // Locataire récupère 500 (50% restant)
        verify(economyHook).deposit(tenant, 500.0);

        // Part consommée = 500. Taxe 10% = 50. Net propriétaire = 450.
        assertThat(plot.getPendingRevenue()).isEqualTo(450.0);
        assertThat(contract.getEscrowBalance()).isEqualTo(0.0);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);

        // Vérifier les logs financiers
        List<TransactionLog> logs = contractRepository.getTransactionLogs(contractId).get();
        assertThat(logs).hasSize(3);

        TransactionLog refundLog = logs.stream().filter(l -> l.getType() == TransactionType.PRORATA_REFUND).findFirst().orElseThrow();
        assertThat(refundLog.getAmount()).isEqualTo(500.0);

        TransactionLog payoutLog = logs.stream().filter(l -> l.getType() == TransactionType.OWNER_PAYOUT).findFirst().orElseThrow();
        assertThat(payoutLog.getAmount()).isEqualTo(450.0);

        TransactionLog taxLog = logs.stream().filter(l -> l.getType() == TransactionType.SERVER_TAX).findFirst().orElseThrow();
        assertThat(taxLog.getAmount()).isEqualTo(50.0);
        assertThat(taxLog.getTargetUuid()).isEqualTo(EscrowService.SERVER_SINK_UUID);
    }
}
