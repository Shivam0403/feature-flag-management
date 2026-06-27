package com.featuremanagement.entity;

import com.featuremanagement.enums.RuleAttribute;
import com.featuremanagement.enums.RuleOperator;
import jakarta.persistence.*;

@Entity
@Table(name = "feature_flag_rules")
public class FeatureFlagRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleAttribute attribute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleOperator operator;

    @Column(name = "rule_values", nullable = false)
    private String ruleValues;

    @Column(nullable = false)
    private boolean state;

    @Column(nullable = false)
    private int priority;

    @Column(name = "percentage_rollout", nullable = true)
    private Integer percentageRollout;

    public FeatureFlagRule() {
    }

    public FeatureFlagRule(RuleAttribute attribute,
                           RuleOperator operator,
                           String values,
                           boolean state,
                           int priority) {
        this.attribute = attribute;
        this.operator = operator;
        this.ruleValues = values;
        this.state = state;
        this.priority = priority;
        this.percentageRollout = null;
    }

    public FeatureFlagRule(RuleAttribute attribute,
                           RuleOperator operator,
                           String values,
                           boolean state,
                           int priority,
                           Integer percentageRollout) {
        this.attribute = attribute;
        this.operator = operator;
        this.ruleValues = values;
        this.state = state;
        this.priority = priority;
        this.percentageRollout = percentageRollout;
    }

    public Long getId() {
        return id;
    }

    public FeatureFlag getFeatureFlag() {
        return featureFlag;
    }

    public void setFeatureFlag(FeatureFlag featureFlag) {
        this.featureFlag = featureFlag;
    }

    public RuleAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(RuleAttribute attribute) {
        this.attribute = attribute;
    }

    public RuleOperator getOperator() {
        return operator;
    }

    public void setOperator(RuleOperator operator) {
        this.operator = operator;
    }

    public String getRuleValues() {
        return ruleValues;
    }

    public void setRuleValues(String ruleValues) {
        this.ruleValues = ruleValues;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getPercentageRollout() {
        return percentageRollout;
    }

    public void setPercentageRollout(Integer percentageRollout) {
        this.percentageRollout = percentageRollout;
    }
}
