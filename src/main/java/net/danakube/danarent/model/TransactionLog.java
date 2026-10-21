package net.danakube.danarent.model;

import java.util.Objects;
import java.util.UUID;

public class TransactionLog {

    private int id;
    private final int contractId;
    private final UUID sourceUuid;
    private final UUID targetUuid;
    private final double amount;
    private final TransactionType type;
    private final long timestamp;

    public TransactionLog(int id, int contractId, UUID sourceUuid, UUID targetUuid,
                          double amount, TransactionType type, long timestamp) {
        this.id = id;
        this.contractId = contractId;
        this.sourceUuid = Objects.requireNonNull(sourceUuid, "sourceUuid cannot be null");
        this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid cannot be null");
        this.amount = amount;
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getContractId() {
        return contractId;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
