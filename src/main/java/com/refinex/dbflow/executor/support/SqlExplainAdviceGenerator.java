package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.executor.dto.SqlExplainAdvice;
import com.refinex.dbflow.executor.dto.SqlExplainPlanRow;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL EXPLAIN 执行计划建议生成器。
 *
 * @author refinex
 */
public final class SqlExplainAdviceGenerator {

    /**
     * 工具类不允许实例化。
     */
    private SqlExplainAdviceGenerator() {
    }

    /**
     * 根据执行计划行生成建议。
     *
     * @param planRows 执行计划行
     * @return 建议列表
     */
    public static List<SqlExplainAdvice> generate(List<SqlExplainPlanRow> planRows) {
        List<SqlExplainAdvice> advice = new ArrayList<>();
        for (SqlExplainPlanRow row : planRows) {
            String table = row.table();
            if (StringUtils.hasText(row.key())) {
                advice.add(new SqlExplainAdvice("INDEX_USED", "INFO", table, "执行计划使用索引 " + row.key()));
            }
            if ("ALL".equalsIgnoreCase(row.type()) || !StringUtils.hasText(row.key())) {
                advice.add(new SqlExplainAdvice("FULL_SCAN", "WARNING", table, "执行计划可能存在全表扫描或未命中索引"));
            }
            if (!StringUtils.hasText(row.possibleKeys()) && StringUtils.hasText(table)) {
                advice.add(new SqlExplainAdvice("NO_POSSIBLE_KEYS", "INFO", table, "优化器未发现可用候选索引"));
            }
        }
        return List.copyOf(advice);
    }
}
