package com.featuremanagement.rule;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleMatcherRegistry {

    private final Map<RuleOperator, RuleMatcher> matchersByOperator;

    public RuleMatcherRegistry(List<RuleMatcher> matchers) {
        Map<RuleOperator, RuleMatcher> map = new EnumMap<>(RuleOperator.class);
        for (RuleMatcher matcher : matchers) {
            for (RuleOperator operator : matcher.supportedOperators()) {
                map.put(operator, matcher);
            }
        }
        this.matchersByOperator = Map.copyOf(map);
    }

    public boolean matches(FeatureFlagRule rule, UserContext context) {
        RuleMatcher matcher = matchersByOperator.get(rule.getOperator());
        if (matcher == null) {
            throw new IllegalStateException("No matcher registered for operator: " + rule.getOperator());
        }
        return matcher.matches(rule, context);
    }
}
