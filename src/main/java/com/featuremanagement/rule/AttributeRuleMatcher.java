package com.featuremanagement.rule;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleAttribute;
import com.featuremanagement.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AttributeRuleMatcher implements RuleMatcher {

    private static final Set<RuleOperator> SUPPORTED = Set.of(
            RuleOperator.EQUALS,
            RuleOperator.NOT_EQUALS,
            RuleOperator.IN,
            RuleOperator.NOT_IN
    );

    @Override
    public Set<RuleOperator> supportedOperators() {
        return SUPPORTED;
    }

    @Override
    public boolean matches(FeatureFlagRule rule, UserContext context) {
        String contextValue = getAttributeValue(rule.getAttribute(), context);
        List<String> values = parseValues(rule.getRuleValues());
        return rule.getOperator().matches(contextValue, values);
    }

    private String getAttributeValue(RuleAttribute attribute, UserContext context) {
        return switch (attribute) {
            case USER_ID -> context.getUserId();
            case SUBSCRIPTION_TIER -> context.getSubscriptionTier();
            case REGION -> context.getRegion();
        };
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
