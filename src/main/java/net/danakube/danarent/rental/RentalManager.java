package net.danakube.danarent.rental;

import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RentalManager {

    private final ContractRepository contractRepository;
    private final Map<Integer, ActiveContract> contractsById = new ConcurrentHashMap<>();
    private final Map<Integer, ActiveContract> contractsByPlotId = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveContract> contractsByTenant = new ConcurrentHashMap<>();

    public RentalManager(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public void addContract(ActiveContract contract) {
        if (contract != null && contract.getStatus() == ContractStatus.ACTIVE) {
            contractsById.put(contract.getId(), contract);
            contractsByPlotId.put(contract.getPlotId(), contract);
            contractsByTenant.put(contract.getTenantUuid(), contract);
        }
    }

    public void removeContract(int contractId) {
        ActiveContract removed = contractsById.remove(contractId);
        if (removed != null) {
            contractsByPlotId.remove(removed.getPlotId());
            contractsByTenant.remove(removed.getTenantUuid());
        }
    }

    public ActiveContract getActiveContractByPlot(int plotId) {
        return contractsByPlotId.get(plotId);
    }

    public ActiveContract getActiveContractByTenant(UUID tenantUuid) {
        if (tenantUuid == null) return null;
        return contractsByTenant.get(tenantUuid);
    }

    public ActiveContract getContract(int contractId) {
        return contractsById.get(contractId);
    }

    public Collection<ActiveContract> getAllActiveContracts() {
        return contractsById.values();
    }

    public CompletableFuture<Void> reloadCache() {
        return contractRepository.findAllActive().thenAccept(activeContracts -> {
            contractsById.clear();
            contractsByPlotId.clear();
            contractsByTenant.clear();
            for (ActiveContract c : activeContracts) {
                addContract(c);
            }
        });
    }
}
