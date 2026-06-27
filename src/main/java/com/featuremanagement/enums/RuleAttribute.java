package com.featuremanagement.enums;

public enum RuleAttribute {
    USER_ID,
    SUBSCRIPTION_TIER,
    REGION;

    public static RuleAttribute from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Rule attribute cannot be null");
        }
        String normalized = value.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "userid", "user_id" -> USER_ID;
            case "subscriptiontier", "subscription_tier", "tier" -> SUBSCRIPTION_TIER;
            case "region" -> REGION;
            default -> throw new IllegalArgumentException("Unsupported attribute: " + value);
        };
    }
}
