package com.featuremanagement.enums;

import java.util.List;

public enum RuleOperator {
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    PERCENTAGE_ROLLOUT;

    public static RuleOperator from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Rule operator cannot be null");
        }
        String normalized = value.trim().toLowerCase().replace("-", "_");
        return switch (normalized) {
            case "equals", "==", "=", "eq" -> EQUALS;
            case "not_equals", "not-equals", "!=", "neq", "ne" -> NOT_EQUALS;
            case "in" -> IN;
            case "not_in", "not-in", "nin" -> NOT_IN;
            case "percentage_rollout", "percentage-rollout", "rollout", "percentage" -> PERCENTAGE_ROLLOUT;
            default -> throw new IllegalArgumentException("Unsupported operator: " + value);
        };
    }

    public boolean matches(String contextValue, List<String> values) {
        String normalized = contextValue == null ? "" : contextValue.trim().toLowerCase();
        return switch (this) {
            case EQUALS -> !values.isEmpty() && values.get(0).equals(normalized);
            case NOT_EQUALS -> values.isEmpty() || !values.get(0).equals(normalized);
            case IN -> values.contains(normalized);
            case NOT_IN -> !values.contains(normalized);
            case PERCENTAGE_ROLLOUT -> throw new UnsupportedOperationException(
                    "Percentage rollout is handled by RuleEvaluation");
        };
    }
}
