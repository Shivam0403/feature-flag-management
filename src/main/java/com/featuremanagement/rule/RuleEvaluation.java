package com.featuremanagement.rule;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlag;
import com.featuremanagement.entity.FeatureFlagRule;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Evaluates feature flag rules in priority order.
 *
 * <p>To add a new rule type, create a {@link RuleMatcher} implementation
 * annotated with {@code @Component}. Spring registers it automatically.
 */
@Component
public class RuleEvaluation {

    private final RuleMatcherRegistry matcherRegistry;

    public RuleEvaluation(RuleMatcherRegistry matcherRegistry) {
        this.matcherRegistry = matcherRegistry;
    }

    public boolean evaluateFlag(FeatureFlag flag, UserContext context) {
        return flag.getRules().stream()
                .sorted(Comparator.comparingInt(FeatureFlagRule::getPriority))
                .filter(rule -> matcherRegistry.matches(rule, context))
                .findFirst()
                .map(FeatureFlagRule::isState)
                .orElse(flag.isDefaultState());
    }
}
