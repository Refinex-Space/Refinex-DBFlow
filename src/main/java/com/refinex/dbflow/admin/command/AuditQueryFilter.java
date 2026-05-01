package com.refinex.dbflow.admin.command;

import com.refinex.dbflow.audit.dto.AuditQueryCriteria;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * 管理端审计查询筛选表单，承接 Spring MVC 查询参数绑定。
 *
 * @author refinex
 */
public class AuditQueryFilter {

    /**
     * 创建时间起点，ISO-8601 格式。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant from;

    /**
     * 创建时间终点，ISO-8601 格式。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant to;

    /**
     * 用户主键。
     */
    private Long userId;

    /**
     * 项目标识。
     */
    private String project;

    /**
     * 环境标识。
     */
    private String env;

    /**
     * 风险级别。
     */
    private String risk;

    /**
     * 审计决策。
     */
    private String decision;

    /**
     * SQL hash。
     */
    private String sqlHash;

    /**
     * 工具名称。
     */
    private String tool;

    /**
     * 页码。
     */
    private Integer page;

    /**
     * 每页条数。
     */
    private Integer size;

    /**
     * 排序字段。
     */
    private String sort;

    /**
     * 排序方向。
     */
    private String direction;

    /**
     * 转换为服务层审计查询条件。
     *
     * @return 审计查询条件
     */
    public AuditQueryCriteria toCriteria() {
        return AuditQueryCriteria.builder()
                .from(from)
                .to(to)
                .userId(userId)
                .projectKey(project)
                .environmentKey(env)
                .riskLevel(risk)
                .decision(decision)
                .sqlHash(sqlHash)
                .tool(tool)
                .page(page)
                .size(size)
                .sort(sort)
                .direction(direction)
                .build();
    }

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getSqlHash() {
        return sqlHash;
    }

    public void setSqlHash(String sqlHash) {
        this.sqlHash = sqlHash;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
