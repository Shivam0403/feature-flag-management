package com.featuremanagement.entity;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.enums.RuleAttribute;
import com.featuremanagement.enums.RuleOperator;
import com.featuremanagement.util.UserBucketingUtil;
import jakarta.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public boolean matches(UserContext context) {
        if (operator == RuleOperator.PERCENTAGE_ROLLOUT) {
            return matchesPercentageRollout(context);
        }

        String contextValue = switch (attribute) {
            case USER_ID -> context.getUserId();
            case SUBSCRIPTION_TIER -> context.getSubscriptionTier();
            case REGION -> context.getRegion();
        };

        List<String> matchedValues = parseValues(ruleValues);
        return operator.matches(contextValue, matchedValues);
    }

    private boolean matchesPercentageRollout(UserContext context) {
        if (percentageRollout == null || percentageRollout <= 0) {
            return false;
        }
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return UserBucketingUtil.isUserInRollout(userId, percentageRollout);
    }

    private List<String> parseValues(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
