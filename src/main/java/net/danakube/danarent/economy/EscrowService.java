package net.danakube.danarent.economy;

import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.database.PlotRepository;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;
import net.danakube.danarent.model.RentPlot;
import net.danakube.danarent.model.TransactionLog;
import net.danakube.danarent.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EscrowService {

    private final EconomyHook economyHook;
    private final PlotRepository plotRepository;
    private final ContractRepository contractRepository;

    public EscrowService(EconomyHook economyHook, PlotRepository plotRepository, ContractRepository contractRepository) {
        this.economyHook = economyHook;
        this.plotRepository = plotRepository;
        this.contractRepository = contractRepository;
    }

    public CompletableFuture<ActiveContract> startRental(UUID tenantUuid, RentPlot plot) {
        return CompletableFuture.supplyAsync(() -> {
            if (!plot.isListed()) {
                throw new IllegalStateException("Ce champ n'est pas disponible sur le marché de location.");
            }

            double price = plot.getPriceLumens();
            if (!economyHook.hasEnough(tenantUuid, price)) {
                throw new IllegalStateException("Solde insuffisant pour louer ce champ (" + price + " requis).");
            }

            boolean withdrawn = economyHook.withdraw(tenantUuid, price);
            if (!withdrawn) {
                throw new IllegalStateException("Échec du prélèvement des Lumens auprès du locataire.");
            }

            plot.setListed(false);
            try {
                plotRepository.save(plot).get();
            } catch (Exception e) {
                // Rembourser le locataire si la sauvegarde échoue
                economyHook.deposit(tenantUuid, price);
                throw new RuntimeException("Erreur de mise à jour de la parcelle", e);
            }

            long now = System.currentTimeMillis();
            long endTime = now + (plot.getDurationSeconds() * 1000L);
            ActiveContract contract = new ActiveContract(
                    0, plot.getId(), tenantUuid, now, endTime,
                    price, price, ContractStatus.ACTIVE
            );

            try {
                int contractId = contractRepository.save(contract).get();
                contract.setId(contractId);

                TransactionLog log = new TransactionLog(
                        0, contractId, tenantUuid, plot.getOwnerUuid(),
                        price, TransactionType.ESCROW_LOCK, now
                );
                contractRepository.logTransaction(log).get();

                return contract;
            } catch (Exception e) {
                economyHook.deposit(tenantUuid, price);
                plot.setListed(true);
                plotRepository.save(plot);
                throw new RuntimeException("Erreur lors de la création du contrat d'escrow", e);
            }
        });
    }

    public CompletableFuture<Void> cancelRental(ActiveContract contract, RentPlot plot) {
        return CompletableFuture.runAsync(() -> {
            if (contract.getStatus() != ContractStatus.ACTIVE) {
                return;
            }

            long now = System.currentTimeMillis();
            long totalDurationSec = (contract.getEndTime() - contract.getStartTime()) / 1000L;
            if (totalDurationSec <= 0) totalDurationSec = 1L;

            long elapsedSec = Math.min(totalDurationSec, Math.max(0L, (now - contract.getStartTime()) / 1000L));
            double consumptionRatio = (double) elapsedSec / (double) totalDurationSec;

            double initialPrice = contract.getInitialPrice();
            double refundTenant = round(initialPrice * (1.0 - consumptionRatio));
            double ownerRevenue = round(initialPrice * consumptionRatio);

            if (refundTenant > 0) {
                economyHook.deposit(contract.getTenantUuid(), refundTenant);
            }

            plot.addPendingRevenue(ownerRevenue);
            contract.setEscrowBalance(0.0);
            contract.setStatus(ContractStatus.CANCELLED);

            try {
                contractRepository.save(contract).get();
                plotRepository.save(plot).get();

                if (refundTenant > 0) {
                    TransactionLog refundLog = new TransactionLog(
                            0, contract.getId(), plot.getOwnerUuid(), contract.getTenantUuid(),
                            refundTenant, TransactionType.PRORATA_REFUND, now
                    );
                    contractRepository.logTransaction(refundLog).get();
                }

                if (ownerRevenue > 0) {
                    TransactionLog payoutLog = new TransactionLog(
                            0, contract.getId(), contract.getTenantUuid(), plot.getOwnerUuid(),
                            ownerRevenue, TransactionType.OWNER_PAYOUT, now
                    );
                    contractRepository.logTransaction(payoutLog).get();
                }
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la résiliation au prorata", e);
            }
        });
    }

    public CompletableFuture<Void> completeRental(ActiveContract contract, RentPlot plot) {
        return CompletableFuture.runAsync(() -> {
            if (contract.getStatus() != ContractStatus.ACTIVE) {
                return;
            }

            long now = System.currentTimeMillis();
            double remainingEscrow = contract.getEscrowBalance();
            plot.addPendingRevenue(remainingEscrow);

            contract.setEscrowBalance(0.0);
            contract.setStatus(ContractStatus.COMPLETED);

            try {
                contractRepository.save(contract).get();
                plotRepository.save(plot).get();

                if (remainingEscrow > 0) {
                    TransactionLog log = new TransactionLog(
                            0, contract.getId(), contract.getTenantUuid(), plot.getOwnerUuid(),
                            remainingEscrow, TransactionType.OWNER_PAYOUT, now
                    );
                    contractRepository.logTransaction(log).get();
                }
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la clôture du bail", e);
            }
        });
    }

    public CompletableFuture<Double> collectPendingRevenue(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<RentPlot> plots = plotRepository.findByOwner(ownerUuid).get();
                double total = 0.0;
                for (RentPlot plot : plots) {
                    total += plot.getPendingRevenue();
                }

                if (total <= 0.0) {
                    return 0.0;
                }

                total = round(total);
                boolean deposited = economyHook.deposit(ownerUuid, total);
                if (!deposited) {
                    throw new IllegalStateException("Échec du versement des Lumens au propriétaire.");
                }

                for (RentPlot plot : plots) {
                    if (plot.getPendingRevenue() > 0) {
                        plot.setPendingRevenue(0.0);
                        plotRepository.save(plot).get();
                    }
                }

                return total;
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la collecte des Lumens", e);
            }
        });
    }

    public CompletableFuture<Void> refundRemainingEscrow(UUID tenantUuid, double amount) {
        return CompletableFuture.runAsync(() -> {
            if (amount > 0) {
                economyHook.deposit(tenantUuid, round(amount));
            }
        });
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
