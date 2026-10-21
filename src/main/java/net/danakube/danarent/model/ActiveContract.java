package net.danakube.danarent.model;

import java.util.Objects;
import java.util.UUID;

public class ActiveContract {

    private int id;
    private final int plotId;
    private final UUID tenantUuid;
    private final long startTime;
    private long endTime;
    private final double initialPrice;
    private double escrowBalance;
    private ContractStatus status;

    public ActiveContract(int id, int plotId, UUID tenantUuid, long startTime, long endTime,
                          double initialPrice, double escrowBalance, ContractStatus status) {
        this.id = id;
        this.plotId = plotId;
        this.tenantUuid = Objects.requireNonNull(tenantUuid, "tenantUuid cannot be null");
        this.startTime = startTime;
        this.endTime = endTime;
        this.initialPrice = initialPrice;
        this.escrowBalance = escrowBalance;
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlotId() {
        return plotId;
    }

    public UUID getTenantUuid() {
        return tenantUuid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public double getEscrowBalance() {
        return escrowBalance;
    }

    public void setEscrowBalance(double escrowBalance) {
        this.escrowBalance = escrowBalance;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    public long getRemainingSeconds() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0L, remaining);
    }
}
