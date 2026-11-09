package com.taller.registro_login.dto;

import java.math.BigDecimal;
import java.util.List;

public class AdminActivityReport {

    private final long totalRequests;
    private final long customerCount;
    private final long administratorCount;
    private final long completedRequests;
    private final double completionRate;
    private final String completionRateLabel;
    private final BigDecimal estimatedShipmentValue;
    private final String estimatedShipmentValueLabel;
    private final BigDecimal averageEstimatedValue;
    private final String averageEstimatedValueLabel;
    private final BigDecimal totalWeightKg;
    private final String totalWeightKgLabel;
    private final String comparisonLabel;
    private final long trendAxisMaximum;
    private final long trendAxisMidpoint;
    private final List<TrendPoint> monthlyTrend;
    private final String trendPolyline;
    private final List<BreakdownItem> statusBreakdown;
    private final String statusDonutBackground;
    private final List<RankingItem> topRoutes;
    private final List<RankingItem> topCustomers;

    public AdminActivityReport(
            long totalRequests,
            long customerCount,
            long administratorCount,
            long completedRequests,
            double completionRate,
            String completionRateLabel,
            BigDecimal estimatedShipmentValue,
            String estimatedShipmentValueLabel,
            BigDecimal averageEstimatedValue,
            String averageEstimatedValueLabel,
            BigDecimal totalWeightKg,
            String totalWeightKgLabel,
            String comparisonLabel,
            long trendAxisMaximum,
            long trendAxisMidpoint,
            List<TrendPoint> monthlyTrend,
            String trendPolyline,
            List<BreakdownItem> statusBreakdown,
            String statusDonutBackground,
            List<RankingItem> topRoutes,
            List<RankingItem> topCustomers) {
        this.totalRequests = totalRequests;
        this.customerCount = customerCount;
        this.administratorCount = administratorCount;
        this.completedRequests = completedRequests;
        this.completionRate = completionRate;
        this.completionRateLabel = completionRateLabel;
        this.estimatedShipmentValue = estimatedShipmentValue;
        this.estimatedShipmentValueLabel = estimatedShipmentValueLabel;
        this.averageEstimatedValue = averageEstimatedValue;
        this.averageEstimatedValueLabel = averageEstimatedValueLabel;
        this.totalWeightKg = totalWeightKg;
        this.totalWeightKgLabel = totalWeightKgLabel;
        this.comparisonLabel = comparisonLabel;
        this.trendAxisMaximum = trendAxisMaximum;
        this.trendAxisMidpoint = trendAxisMidpoint;
        this.monthlyTrend = List.copyOf(monthlyTrend);
        this.trendPolyline = trendPolyline;
        this.statusBreakdown = List.copyOf(statusBreakdown);
        this.statusDonutBackground = statusDonutBackground;
        this.topRoutes = List.copyOf(topRoutes);
        this.topCustomers = List.copyOf(topCustomers);
    }

    public long getTotalRequests() { return totalRequests; }
    public long getCustomerCount() { return customerCount; }
    public long getAdministratorCount() { return administratorCount; }
    public long getCompletedRequests() { return completedRequests; }
    public double getCompletionRate() { return completionRate; }
    public String getCompletionRateLabel() { return completionRateLabel; }
    public BigDecimal getEstimatedShipmentValue() { return estimatedShipmentValue; }
    public String getEstimatedShipmentValueLabel() { return estimatedShipmentValueLabel; }
    public BigDecimal getAverageEstimatedValue() { return averageEstimatedValue; }
    public String getAverageEstimatedValueLabel() { return averageEstimatedValueLabel; }
    public BigDecimal getTotalWeightKg() { return totalWeightKg; }
    public String getTotalWeightKgLabel() { return totalWeightKgLabel; }
    public String getComparisonLabel() { return comparisonLabel; }
    public long getTrendAxisMaximum() { return trendAxisMaximum; }
    public long getTrendAxisMidpoint() { return trendAxisMidpoint; }
    public List<TrendPoint> getMonthlyTrend() { return monthlyTrend; }
    public String getTrendPolyline() { return trendPolyline; }
    public List<BreakdownItem> getStatusBreakdown() { return statusBreakdown; }
    public String getStatusDonutBackground() { return statusDonutBackground; }
    public List<RankingItem> getTopRoutes() { return topRoutes; }
    public List<RankingItem> getTopCustomers() { return topCustomers; }

    public static class TrendPoint {
        private final String label;
        private final long value;
        private final int x;
        private final int y;

        public TrendPoint(String label, long value, int x, int y) {
            this.label = label;
            this.value = value;
            this.x = x;
            this.y = y;
        }

        public String getLabel() { return label; }
        public long getValue() { return value; }
        public int getX() { return x; }
        public int getY() { return y; }
    }

    public static class BreakdownItem {
        private final String label;
        private final long value;
        private final double percentage;
        private final String percentageLabel;
        private final String color;

        public BreakdownItem(String label, long value, double percentage, String percentageLabel, String color) {
            this.label = label;
            this.value = value;
            this.percentage = percentage;
            this.percentageLabel = percentageLabel;
            this.color = color;
        }

        public String getLabel() { return label; }
        public long getValue() { return value; }
        public double getPercentage() { return percentage; }
        public String getPercentageLabel() { return percentageLabel; }
        public String getColor() { return color; }
    }

    public static class RankingItem {
        private final String label;
        private final long value;
        private final double relativeWidth;
        private final String detail;

        public RankingItem(String label, long value, double relativeWidth, String detail) {
            this.label = label;
            this.value = value;
            this.relativeWidth = relativeWidth;
            this.detail = detail;
        }

        public String getLabel() { return label; }
        public long getValue() { return value; }
        public double getRelativeWidth() { return relativeWidth; }
        public String getDetail() { return detail; }
    }
}
