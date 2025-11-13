package dic1.projet.trans.backend.dtos;

import java.math.BigDecimal;

public class OrganizerStatsResponse {
    private int activeEventsCount;
    private long totalReservations;
    private BigDecimal totalRevenue;
    private double fillRate; // 0.0 - 1.0

    public OrganizerStatsResponse() {}

    public OrganizerStatsResponse(int activeEventsCount, long totalReservations, BigDecimal totalRevenue, double fillRate) {
        this.activeEventsCount = activeEventsCount;
        this.totalReservations = totalReservations;
        this.totalRevenue = totalRevenue;
        this.fillRate = fillRate;
    }

    public int getActiveEventsCount() {
        return activeEventsCount;
    }

    public void setActiveEventsCount(int activeEventsCount) {
        this.activeEventsCount = activeEventsCount;
    }

    public long getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(long totalReservations) {
        this.totalReservations = totalReservations;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getFillRate() {
        return fillRate;
    }

    public void setFillRate(double fillRate) {
        this.fillRate = fillRate;
    }
}