package com.featuremanagement.dto;

import java.util.List;

public class FeatureFlagDtos {
    public record CreateFeatureFlagRequest(String name, boolean defaultState, List<FeatureFlagRuleRequest> rules) {
    }

    public record FeatureFlagRuleRequest(String attribute,
                                        String operator,
                                        List<String> values,
                                        boolean state,
                                        Integer priority,
                                        Integer percentageRollout) {
    }

    public record EvaluationRequest(String userId, String subscriptionTier, String region) {
    }

    public record FeatureFlagEvaluationResponse(String featureFlagName, boolean enabled, String result) {
    }

    public record CreateFeatureFlagResponse(String name, boolean defaultState, int ruleCount) {
    }

    public record FeatureFlagSummary(String name, boolean defaultState, int ruleCount) {
    }
}
