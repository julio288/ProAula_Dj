package com.taller.registro_login.service;

import com.taller.registro_login.dto.AdminActivityReport;
import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminReportService {

    private static final List<String> STATUS_COLORS = List.of(
            "#fbbf24", "#60a5fa", "#22d3ee", "#4ade80", "#fb7185");
    private static final List<String> RANKING_COLORS = List.of(
            "#7280ff", "#22d3ee", "#a78bfa", "#4ade80", "#fbbf24");
    private static final Locale REPORT_LOCALE = Locale.forLanguageTag("es-CO");

    private final FreightRequestRepository requestRepository;
    private final UserRepository userRepository;

    public AdminReportService(FreightRequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminActivityReport buildReport() {
        List<FreightRequest> requests = requestRepository.findAllByOrderByCreatedAtDesc();
        BigDecimal estimatedValue = requests.stream()
                .map(FreightRequest::getEstimatedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = requests.stream()
                .map(FreightRequest::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageValue = requests.isEmpty()
                ? BigDecimal.ZERO
                : estimatedValue.divide(BigDecimal.valueOf(requests.size()), 2, RoundingMode.HALF_UP);

        Map<FreightRequestStatus, Long> countsByStatus = requests.stream()
                .collect(Collectors.groupingBy(
                        FreightRequest::getStatus,
                        () -> new EnumMap<>(FreightRequestStatus.class),
                        Collectors.counting()));
        List<AdminActivityReport.BreakdownItem> statusBreakdown = new ArrayList<>();
        StringBuilder donutBackground = new StringBuilder("conic-gradient(");
        double accumulatedDegrees = 0;
        FreightRequestStatus[] statuses = FreightRequestStatus.values();
        for (int index = 0; index < statuses.length; index++) {
            FreightRequestStatus status = statuses[index];
            long count = countsByStatus.getOrDefault(status, 0L);
            double percentage = percentage(count, requests.size());
            statusBreakdown.add(new AdminActivityReport.BreakdownItem(
                    status.getLabel(), count, percentage, formatNumber(percentage, 1) + "%",
                    STATUS_COLORS.get(index)));
            double nextDegrees = accumulatedDegrees + percentage * 3.6;
            if (index > 0) {
                donutBackground.append(", ");
            }
            donutBackground.append(STATUS_COLORS.get(index))
                    .append(' ')
                    .append(formatDegrees(accumulatedDegrees))
                    .append("deg ")
                    .append(formatDegrees(nextDegrees))
                    .append("deg");
            accumulatedDegrees = nextDegrees;
        }
        if (requests.isEmpty()) {
            donutBackground = new StringBuilder("conic-gradient(#293142 0deg 360deg)");
        } else {
            donutBackground.append(')');
        }

        List<AdminActivityReport.TrendPoint> monthlyTrend = buildMonthlyTrend(requests);
        long trendAxisMaximum = monthlyTrend.stream()
                .mapToLong(AdminActivityReport.TrendPoint::getValue)
                .max()
                .orElse(0);
        String trendPolyline = monthlyTrend.stream()
                .map(point -> point.getX() + "," + point.getY())
                .collect(Collectors.joining(" "));

        List<AdminActivityReport.RankingItem> topRoutes = buildRanking(
                requests,
                request -> request.getLane().getOrigin() + " → " + request.getLane().getDestination(),
                "solicitudes");
        List<AdminActivityReport.RankingItem> topCustomers = buildCustomerRanking(requests);

        long completedRequests = countsByStatus.getOrDefault(FreightRequestStatus.DELIVERED, 0L);
        String comparisonLabel = buildComparisonLabel(monthlyTrend);
        return new AdminActivityReport(
                requests.size(),
                userRepository.countByRole(User.ROLE_USER),
                userRepository.countByRole(User.ROLE_ADMIN),
                completedRequests,
                percentage(completedRequests, requests.size()),
                formatNumber(percentage(completedRequests, requests.size()), 1) + "%",
                estimatedValue,
                formatNumber(estimatedValue, 0),
                averageValue,
                formatNumber(averageValue, 0),
                totalWeight,
                formatNumber(totalWeight, 1),
                comparisonLabel,
                trendAxisMaximum,
                Math.round(trendAxisMaximum / 2.0),
                monthlyTrend,
                trendPolyline,
                statusBreakdown,
                donutBackground.toString(),
                topRoutes,
                topCustomers);
    }

    private List<AdminActivityReport.TrendPoint> buildMonthlyTrend(List<FreightRequest> requests) {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = IntStream.rangeClosed(0, 5)
                .mapToObj(currentMonth::minusMonths)
                .sorted()
                .toList();
        Map<YearMonth, Long> monthlyCounts = requests.stream()
                .collect(Collectors.groupingBy(
                        request -> YearMonth.from(request.getCreatedAt()),
                        Collectors.counting()));
        long maxCount = months.stream()
                .mapToLong(month -> monthlyCounts.getOrDefault(month, 0L))
                .max()
                .orElse(0);

        return IntStream.range(0, months.size())
                .mapToObj(index -> {
                    YearMonth month = months.get(index);
                    String label = month.getMonth().getDisplayName(TextStyle.SHORT, REPORT_LOCALE);
                    label = label.substring(0, 1).toUpperCase(REPORT_LOCALE) + label.substring(1);
                    long value = monthlyCounts.getOrDefault(month, 0L);
                    int y = maxCount == 0 ? 170 : 170 - (int) Math.round(value * 135.0 / maxCount);
                    return new AdminActivityReport.TrendPoint(label, value, 60 + index * 96, y);
                })
                .toList();
    }

    private List<AdminActivityReport.RankingItem> buildRanking(
            List<FreightRequest> requests,
            java.util.function.Function<FreightRequest, String> labelExtractor,
            String detail) {
        Map<String, Long> counts = requests.stream()
                .collect(Collectors.groupingBy(labelExtractor, LinkedHashMap::new, Collectors.counting()));
        List<Map.Entry<String, Long>> ranked = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(5)
                .toList();
        long maxCount = ranked.stream().mapToLong(Map.Entry::getValue).max().orElse(0);
        return IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    Map.Entry<String, Long> item = ranked.get(index);
                    double width = maxCount == 0 ? 0 : item.getValue() * 100.0 / maxCount;
                    return new AdminActivityReport.RankingItem(
                            item.getKey(),
                            item.getValue(),
                            width,
                            detail + " · posición " + (index + 1));
                })
                .toList();
    }

    private List<AdminActivityReport.RankingItem> buildCustomerRanking(List<FreightRequest> requests) {
        Map<Long, Long> counts = requests.stream()
                .collect(Collectors.groupingBy(
                        request -> request.getCustomer().getId(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        List<Map.Entry<Long, Long>> ranked = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .toList();
        long maxCount = ranked.stream().mapToLong(Map.Entry::getValue).max().orElse(0);
        Map<Long, String> customerNames = requests.stream()
                .collect(Collectors.toMap(
                        request -> request.getCustomer().getId(),
                        request -> request.getCustomer().getFullName(),
                        (firstName, ignored) -> firstName));
        return IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    Map.Entry<Long, Long> item = ranked.get(index);
                    double width = maxCount == 0 ? 0 : item.getValue() * 100.0 / maxCount;
                    return new AdminActivityReport.RankingItem(
                            customerNames.get(item.getKey()),
                            item.getValue(),
                            width,
                            "solicitudes · posición " + (index + 1));
                })
                .toList();
    }

    private String buildComparisonLabel(List<AdminActivityReport.TrendPoint> trend) {
        long currentMonth = trend.get(trend.size() - 1).getValue();
        long previousMonth = trend.get(trend.size() - 2).getValue();
        if (previousMonth == 0) {
            return currentMonth == 0
                    ? "Sin solicitudes en los dos últimos meses"
                    : "Nuevas solicitudes este mes; el mes anterior no tuvo actividad";
        }
        double change = (currentMonth - previousMonth) * 100.0 / previousMonth;
        String sign = change > 0 ? "+" : "";
        return sign + String.format(REPORT_LOCALE, "%.0f%%", change)
                + " frente al mes anterior";
    }

    private double percentage(long value, long total) {
        return total == 0 ? 0 : value * 100.0 / total;
    }

    private String formatNumber(BigDecimal value, int fractionDigits) {
        NumberFormat formatter = NumberFormat.getNumberInstance(REPORT_LOCALE);
        formatter.setGroupingUsed(true);
        formatter.setMinimumFractionDigits(fractionDigits);
        formatter.setMaximumFractionDigits(fractionDigits);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(value);
    }

    private String formatNumber(double value, int fractionDigits) {
        NumberFormat formatter = NumberFormat.getNumberInstance(REPORT_LOCALE);
        formatter.setGroupingUsed(true);
        formatter.setMinimumFractionDigits(fractionDigits);
        formatter.setMaximumFractionDigits(fractionDigits);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(value);
    }

    private String formatDegrees(double degrees) {
        return String.format(Locale.ROOT, "%.2f", degrees);
    }
}
