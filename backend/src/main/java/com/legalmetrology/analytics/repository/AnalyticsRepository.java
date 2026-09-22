package com.legalmetrology.analytics.repository;

import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.model.FieldCategoryCount;
import com.legalmetrology.analytics.model.KpiSnapshot;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.model.RegionSeverityCount;
import com.legalmetrology.analytics.model.RiskFactors;
import com.legalmetrology.analytics.model.RuleSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * The one place in this system that writes raw aggregation SQL over
 * inspections/violations/rules/products/manufacturers. Everything above
 * this class — {@code AnalyticsQueryService} and, transitively, the
 * insight/prediction/risk layers — works only with the typed records this
 * returns, never with a ResultSet or a table name.
 * <p>
 * Plain JdbcTemplate rather than Spring Data JPA/derived queries, for the
 * same reason {@code ai.embedding} uses it: these are GROUP BY/date_trunc
 * aggregate queries with no natural entity-shaped result, so a repository
 * interface would buy nothing over direct SQL.
 */
@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MonthlyMetric> monthlyInspectionCounts(int monthsBack) {
        String sql = """
                select date_trunc('month', started_at) as bucket, count(*) as cnt
                from inspections
                where started_at >= ?
                group by bucket
                order by bucket
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> MonthlyMetric.countOnly(
                toYearMonth(rs.getTimestamp("bucket").toInstant()), rs.getLong("cnt")), cutoff(monthsBack));
    }

    public List<MonthlyMetric> monthlyViolationCounts(int monthsBack) {
        String sql = """
                select date_trunc('month', v.created_at) as bucket, count(*) as cnt
                from violations v
                where v.created_at >= ?
                group by bucket
                order by bucket
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> MonthlyMetric.countOnly(
                toYearMonth(rs.getTimestamp("bucket").toInstant()), rs.getLong("cnt")), cutoff(monthsBack));
    }

    public List<MonthlyMetric> monthlyViolationCountsForRule(String ruleCode, int monthsBack) {
        String sql = """
                select date_trunc('month', v.created_at) as bucket, count(*) as cnt
                from violations v join rules r on r.id = v.rule_id
                where r.rule_code = ? and v.created_at >= ?
                group by bucket
                order by bucket
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> MonthlyMetric.countOnly(
                toYearMonth(rs.getTimestamp("bucket").toInstant()), rs.getLong("cnt")), ruleCode, cutoff(monthsBack));
    }

    public List<MonthlyMetric> monthlyViolationCountsForManufacturer(UUID manufacturerId, int monthsBack) {
        String sql = """
                select date_trunc('month', v.created_at) as bucket, count(*) as cnt
                from violations v
                join inspections i on i.id = v.inspection_id
                join products p on p.id = i.product_id
                where p.manufacturer_id = ? and v.created_at >= ?
                group by bucket
                order by bucket
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> MonthlyMetric.countOnly(
                toYearMonth(rs.getTimestamp("bucket").toInstant()), rs.getLong("cnt")), manufacturerId, cutoff(monthsBack));
    }

    public List<MonthlyMetric> monthlyAverageComplianceScore(int monthsBack) {
        String sql = """
                select date_trunc('month', started_at) as bucket, avg(compliance_score) as avg_score, count(*) as cnt
                from inspections
                where started_at >= ? and compliance_score is not null
                group by bucket
                order by bucket
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new MonthlyMetric(
                toYearMonth(rs.getTimestamp("bucket").toInstant()), rs.getLong("cnt"), rs.getBigDecimal("avg_score")), cutoff(monthsBack));
    }

    public List<DimensionMetric> mostViolatedRules(int limit) {
        String sql = """
                select r.id, r.rule_code || ' — ' || r.title as label, count(*) as cnt
                from violations v join rules r on r.id = v.rule_id
                group by r.id, label
                order by cnt desc
                limit ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> DimensionMetric.of(
                (UUID) rs.getObject("id"), rs.getString("label"), rs.getLong("cnt")), limit);
    }

    public List<DimensionMetric> severityDistribution() {
        String sql = "select severity as label, count(*) as cnt from violations group by severity order by cnt desc";
        return jdbcTemplate.query(sql, (rs, rowNum) -> DimensionMetric.of(null, rs.getString("label"), rs.getLong("cnt")));
    }

    public List<DimensionMetric> violationCountsByManufacturer() {
        String sql = """
                select m.id, m.name as label, count(*) as cnt
                from violations v
                join inspections i on i.id = v.inspection_id
                join products p on p.id = i.product_id
                join manufacturers m on m.id = p.manufacturer_id
                group by m.id, m.name
                order by cnt desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> DimensionMetric.of(
                (UUID) rs.getObject("id"), rs.getString("label"), rs.getLong("cnt")));
    }

    public List<DimensionMetric> violationCountsByRegion() {
        String sql = """
                select coalesce(i.region, 'Unspecified') as label, count(*) as cnt
                from violations v join inspections i on i.id = v.inspection_id
                group by label
                order by cnt desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> DimensionMetric.of(null, rs.getString("label"), rs.getLong("cnt")));
    }

    public List<DimensionMetric> violationCountsByCategory() {
        String sql = """
                select coalesce(pc.name, 'Uncategorized') as label, count(*) as cnt
                from violations v
                join inspections i on i.id = v.inspection_id
                join products p on p.id = i.product_id
                left join product_categories pc on pc.id = p.category_id
                group by label
                order by cnt desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> DimensionMetric.of(null, rs.getString("label"), rs.getLong("cnt")));
    }

    public List<FieldCategoryCount> violationFieldCountsByCategory() {
        String sql = """
                select v.field as field, coalesce(pc.name, 'Uncategorized') as category, count(*) as cnt
                from violations v
                join inspections i on i.id = v.inspection_id
                join products p on p.id = i.product_id
                left join product_categories pc on pc.id = p.category_id
                where v.field is not null
                group by v.field, category
                order by cnt desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FieldCategoryCount(
                rs.getString("field"), rs.getString("category"), rs.getLong("cnt")));
    }

    public List<DimensionMetric> complianceScoreByManufacturer() {
        String sql = """
                select m.id, m.name as label, count(*) as cnt, avg(i.compliance_score) as avg_score
                from inspections i
                join products p on p.id = i.product_id
                join manufacturers m on m.id = p.manufacturer_id
                where i.compliance_score is not null
                group by m.id, m.name
                order by avg_score asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DimensionMetric(
                (UUID) rs.getObject("id"), rs.getString("label"), rs.getLong("cnt"), rs.getBigDecimal("avg_score")));
    }

    public List<DimensionMetric> complianceScoreByRegion() {
        String sql = """
                select coalesce(region, 'Unspecified') as label, count(*) as cnt, avg(compliance_score) as avg_score
                from inspections
                where compliance_score is not null
                group by label
                order by avg_score asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DimensionMetric(
                null, rs.getString("label"), rs.getLong("cnt"), rs.getBigDecimal("avg_score")));
    }

    public List<DimensionMetric> complianceScoreByCategory() {
        String sql = """
                select coalesce(pc.name, 'Uncategorized') as label, count(*) as cnt, avg(i.compliance_score) as avg_score
                from inspections i
                join products p on p.id = i.product_id
                left join product_categories pc on pc.id = p.category_id
                where i.compliance_score is not null
                group by label
                order by avg_score asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DimensionMetric(
                null, rs.getString("label"), rs.getLong("cnt"), rs.getBigDecimal("avg_score")));
    }

    public List<DimensionMetric> inspectorActivity() {
        String sql = """
                select u.id, u.full_name as label, count(*) as cnt, avg(i.compliance_score) as avg_score
                from inspections i join users u on u.id = i.inspector_id
                group by u.id, u.full_name
                order by cnt desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DimensionMetric(
                (UUID) rs.getObject("id"), rs.getString("label"), rs.getLong("cnt"), rs.getBigDecimal("avg_score")));
    }

    /** Average compliance score for inspections started within [start, end) — null if there's no data in the window. */
    public BigDecimal averageComplianceScoreInWindow(Instant start, Instant end) {
        String sql = """
                select avg(compliance_score) as avg_score
                from inspections
                where started_at >= ? and started_at < ? and compliance_score is not null
                """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, java.sql.Timestamp.from(start), java.sql.Timestamp.from(end));
    }

    public KpiSnapshot getKpiSnapshot() {
        long totalInspections = count("select count(*) from inspections");
        long completedInspections = count("select count(*) from inspections where status = 'COMPLETED' or status = 'CLOSED'");
        long totalViolations = count("select count(*) from violations");
        long criticalViolations = count("select count(*) from violations where severity = 'CRITICAL'");
        long activeInspectors = count("select count(distinct inspector_id) from inspections");
        long activeRules = count("select count(*) from rules where is_active = true");

        BigDecimal avgScore = jdbcTemplate.queryForObject(
                "select avg(compliance_score) from inspections where compliance_score is not null", BigDecimal.class);

        BigDecimal complianceRate = jdbcTemplate.queryForObject("""
                select case when count(*) filter (where compliance_score is not null) = 0 then null
                       else 100.0 * count(*) filter (where compliance_score >= 70) / count(*) filter (where compliance_score is not null)
                       end
                from inspections
                """, BigDecimal.class);

        return new KpiSnapshot(totalInspections, completedInspections, totalViolations, criticalViolations,
                avgScore, complianceRate, activeInspectors, activeRules);
    }

    /** Every rule version (active or not) for a rule code, for the "did quality improve after this version's effective date" insight. */
    public List<LocalDate> ruleVersionEffectiveDates(String ruleCode) {
        String sql = "select effective_date from rules where rule_code = ? order by version";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getDate("effective_date").toLocalDate(), ruleCode);
    }

    /** Rule code/title only — master data, never the rule's validation_type/expression. Used by insight generators, not evaluation. */
    public List<RuleSummary> activeRuleSummaries() {
        String sql = "select rule_code, title, version, effective_date from rules where is_active = true order by rule_code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RuleSummary(
                rs.getString("rule_code"), rs.getString("title"), rs.getInt("version"), rs.getDate("effective_date").toLocalDate()));
    }

    /** Active rules currently on version 2 or later — candidates for the rule-version-impact insight. */
    public List<RuleSummary> activeRuleSummariesWithMultipleVersions() {
        String sql = "select rule_code, title, version, effective_date from rules where is_active = true and version >= 2 order by rule_code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RuleSummary(
                rs.getString("rule_code"), rs.getString("title"), rs.getInt("version"), rs.getDate("effective_date").toLocalDate()));
    }

    public List<RegionSeverityCount> severityCountsByRegion() {
        String sql = """
                select coalesce(i.region, 'Unspecified') as region, v.severity as severity, count(*) as cnt
                from violations v join inspections i on i.id = v.inspection_id
                group by region, severity
                order by region, severity
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RegionSeverityCount(
                rs.getString("region"), rs.getString("severity"), rs.getLong("cnt")));
    }

    private static final String RECENT_WINDOW_COLUMNS = """
                       count(v.id) filter (where v.severity = 'CRITICAL' and v.created_at >= now() - interval '30 days') as recent_critical_count,
                       count(v.id) filter (where v.created_at >= now() - interval '30 days') as recent_violation_count,
                       count(v.id) filter (where v.created_at >= now() - interval '60 days' and v.created_at < now() - interval '30 days') as prior_violation_count
            """;

    public List<RiskFactors> riskFactorsByManufacturer() {
        String sql = """
                select m.id, m.name as label,
                       count(distinct i.id) as inspection_count,
                       count(v.id) as violation_count,
                       count(v.id) filter (where v.severity = 'CRITICAL') as critical_count,
                       avg(i.compliance_score) as avg_score,
                """ + RECENT_WINDOW_COLUMNS + """
                from manufacturers m
                join products p on p.manufacturer_id = m.id
                join inspections i on i.product_id = p.id
                left join violations v on v.inspection_id = i.id
                group by m.id, m.name
                """;
        return jdbcTemplate.query(sql, this::mapRiskFactors);
    }

    public List<RiskFactors> riskFactorsByCategory() {
        String sql = """
                select pc.id, pc.name as label,
                       count(distinct i.id) as inspection_count,
                       count(v.id) as violation_count,
                       count(v.id) filter (where v.severity = 'CRITICAL') as critical_count,
                       avg(i.compliance_score) as avg_score,
                """ + RECENT_WINDOW_COLUMNS + """
                from product_categories pc
                join products p on p.category_id = pc.id
                join inspections i on i.product_id = p.id
                left join violations v on v.inspection_id = i.id
                group by pc.id, pc.name
                """;
        return jdbcTemplate.query(sql, this::mapRiskFactors);
    }

    public List<RiskFactors> riskFactorsByRegion() {
        String sql = """
                select coalesce(i.region, 'Unspecified') as label,
                       count(distinct i.id) as inspection_count,
                       count(v.id) as violation_count,
                       count(v.id) filter (where v.severity = 'CRITICAL') as critical_count,
                       avg(i.compliance_score) as avg_score,
                """ + RECENT_WINDOW_COLUMNS + """
                from inspections i
                left join violations v on v.inspection_id = i.id
                group by label
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RiskFactors(null, rs.getString("label"),
                rs.getLong("inspection_count"), rs.getLong("violation_count"), rs.getLong("critical_count"), rs.getBigDecimal("avg_score"),
                rs.getLong("recent_critical_count"), rs.getLong("recent_violation_count"), rs.getLong("prior_violation_count")));
    }

    public List<RiskFactors> riskFactorsByInspector() {
        String sql = """
                select u.id, u.full_name as label,
                       count(distinct i.id) as inspection_count,
                       count(v.id) as violation_count,
                       count(v.id) filter (where v.severity = 'CRITICAL') as critical_count,
                       avg(i.compliance_score) as avg_score,
                """ + RECENT_WINDOW_COLUMNS + """
                from users u
                join inspections i on i.inspector_id = u.id
                left join violations v on v.inspection_id = i.id
                group by u.id, u.full_name
                """;
        return jdbcTemplate.query(sql, this::mapRiskFactors);
    }

    private RiskFactors mapRiskFactors(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RiskFactors((UUID) rs.getObject("id"), rs.getString("label"), rs.getLong("inspection_count"),
                rs.getLong("violation_count"), rs.getLong("critical_count"), rs.getBigDecimal("avg_score"),
                rs.getLong("recent_critical_count"), rs.getLong("recent_violation_count"), rs.getLong("prior_violation_count"));
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    private java.sql.Timestamp cutoff(int monthsBack) {
        return java.sql.Timestamp.from(Instant.now().atZone(ZoneOffset.UTC).minusMonths(monthsBack).toInstant());
    }

    private YearMonth toYearMonth(Instant instant) {
        return YearMonth.from(instant.atZone(ZoneOffset.UTC));
    }
}
