package com.featuremanagement.rule;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleOperator;

import java.util.Set;

public interface RuleMatcher {

    Set<RuleOperator> supportedOperators();

    boolean matches(FeatureFlagRule rule, UserContext context);
}
