package net.danakube.danarent.rental;

import net.danakube.danarent.database.ContractRepository;
import net.danakube.danarent.model.ActiveContract;
import net.danakube.danarent.model.ContractStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RentalManager {

    private final ContractRepository contractRepository;
    private final Map<Integer, ActiveContract> contractsById = new ConcurrentHashMap<>();
    private final Map<Integer, ActiveContract> contractsByPlotId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<ActiveContract>> contractsByTenant = new ConcurrentHashMap<>();

    public RentalManager(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public void addContract(ActiveContract contract) {
        if (contract != null && contract.getStatus() == ContractStatus.ACTIVE) {
            contractsById.put(contract.getId(), contract);
            contractsByPlotId.put(contract.getPlotId(), contract);
            contractsByTenant.computeIfAbsent(contract.getTenantUuid(), k -> ConcurrentHashMap.newKeySet()).add(contract);
        }
    }

    public void removeContract(int contractId) {
        ActiveContract removed = contractsById.remove(contractId);
        if (removed != null) {
            contractsByPlotId.remove(removed.getPlotId());
            Set<ActiveContract> tenantContracts = contractsByTenant.get(removed.getTenantUuid());
            if (tenantContracts != null) {
                tenantContracts.remove(removed);
                if (tenantContracts.isEmpty()) {
                    contractsByTenant.remove(removed.getTenantUuid());
                }
            }
        }
    }

    public ActiveContract getActiveContractByPlot(int plotId) {
        return contractsByPlotId.get(plotId);
    }

    public ActiveContract getActiveContractByTenant(UUID tenantUuid) {
        if (tenantUuid == null) return null;
        Set<ActiveContract> tenantContracts = contractsByTenant.get(tenantUuid);
        if (tenantContracts == null || tenantContracts.isEmpty()) return null;
        return tenantContracts.iterator().next();
    }

    public Set<ActiveContract> getActiveContractsByTenant(UUID tenantUuid) {
        if (tenantUuid == null) return Collections.emptySet();
        return contractsByTenant.getOrDefault(tenantUuid, Collections.emptySet());
    }

    public int getActiveRentalCount(UUID tenantUuid) {
        if (tenantUuid == null) return 0;
        Set<ActiveContract> tenantContracts = contractsByTenant.get(tenantUuid);
        return tenantContracts != null ? tenantContracts.size() : 0;
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
