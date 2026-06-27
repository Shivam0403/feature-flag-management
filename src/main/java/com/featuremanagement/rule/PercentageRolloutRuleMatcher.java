package com.featuremanagement.rule;

import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleOperator;
import com.featuremanagement.util.UserBucketingUtil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PercentageRolloutRuleMatcher implements RuleMatcher {

    @Override
    public Set<RuleOperator> supportedOperators() {
        return Set.of(RuleOperator.PERCENTAGE_ROLLOUT);
    }

    @Override
    public boolean matches(FeatureFlagRule rule, UserContext context) {
        Integer percentage = rule.getPercentageRollout();
        if (percentage == null || percentage <= 0) {
            return false;
        }

        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            return false;
        }

        return UserBucketingUtil.isUserInRollout(userId, percentage);
    }
}
