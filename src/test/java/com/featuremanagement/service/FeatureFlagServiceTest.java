package com.featuremanagement.service;

import com.featuremanagement.dto.FeatureFlagDtos.CreateFeatureFlagRequest;
import com.featuremanagement.dto.FeatureFlagDtos.FeatureFlagEvaluationResponse;
import com.featuremanagement.dto.FeatureFlagDtos.FeatureFlagRuleRequest;
import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlag;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleAttribute;
import com.featuremanagement.enums.RuleOperator;
import com.featuremanagement.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("ci")
class FeatureFlagServiceTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @SpyBean
    private FeatureFlagRepository featureFlagRepository;

    @Test
    void createFlag_persistsFlagAndRules() {
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(
                "beta-ui-" + System.nanoTime(),
                false,
                List.of(new FeatureFlagRuleRequest("subscriptionTier", "IN", List.of("pro"), true, 1, null))
        );

        FeatureFlag created = featureFlagService.createFlag(request);

        assertEquals(request.name(), created.getName());
        assertFalse(created.isDefaultState());
        assertEquals(1, created.getRules().size());
        

        FeatureFlag persisted = featureFlagRepository.findByName(request.name()).orElseThrow();
        assertEquals(request.name(), persisted.getName());
        assertTrue(persisted.getRules().get(0).isState());
    }

    @Test
    void evaluateFlag_returnsRuleBasedStateAndDefaultsWhenNoRuleMatches() {
        FeatureFlag flag = new FeatureFlag("checkout-redesign-" + System.nanoTime(), false, List.of(
                new FeatureFlagRule(RuleAttribute.REGION, RuleOperator.IN, "us,ca", true, 0)
        ));
        featureFlagRepository.save(flag);

        UserContext matchingContext = new UserContext();
        matchingContext.setRegion("us");

        UserContext nonMatchingContext = new UserContext();
        nonMatchingContext.setRegion("eu");

        FeatureFlagEvaluationResponse enabled = featureFlagService.evaluateFlag(flag.getName(), matchingContext);
        FeatureFlagEvaluationResponse disabled = featureFlagService.evaluateFlag(flag.getName(), nonMatchingContext);

        assertTrue(enabled.enabled());
        assertEquals("ON", enabled.result());
        assertFalse(disabled.enabled());
        assertEquals("OFF", disabled.result());
    }

    @Test
    void evaluateFlag_usesCacheAfterFirstLookup() {
        FeatureFlag flag = new FeatureFlag("search-boost-" + System.nanoTime(), false, List.of(
                new FeatureFlagRule(RuleAttribute.SUBSCRIPTION_TIER, RuleOperator.IN, "pro", true, 0)
        ));
        featureFlagRepository.save(flag);

        UserContext context = new UserContext();
        context.setSubscriptionTier("pro");

        featureFlagService.evaluateFlag(flag.getName(), context);
        featureFlagService.evaluateFlag(flag.getName(), context);

        verify(featureFlagRepository, times(1)).findByName(flag.getName());
    }

    @Test
    void evaluateFlag_percentageRollout_deterministicForSameUser() {
        String flagName = "feature-rollout-" + System.nanoTime();
        FeatureFlag flag = new FeatureFlag(flagName, false, List.of(
                new FeatureFlagRule(RuleAttribute.USER_ID, RuleOperator.PERCENTAGE_ROLLOUT, "", true, 0, 30)
        ));
        featureFlagRepository.save(flag);

        UserContext context = new UserContext();
        context.setUserId("user-123");

        FeatureFlagEvaluationResponse result1 = featureFlagService.evaluateFlag(flagName, context);
        FeatureFlagEvaluationResponse result2 = featureFlagService.evaluateFlag(flagName, context);

        assertEquals(result1.enabled(), result2.enabled(), "Same user should get same rollout decision");
    }

    @Test
    void evaluateFlag_percentageRollout_100PercentIncludesAllUsers() {
        String flagName = "feature-100-pct-" + System.nanoTime();
        FeatureFlag flag = new FeatureFlag(flagName, false, List.of(
                new FeatureFlagRule(RuleAttribute.USER_ID, RuleOperator.PERCENTAGE_ROLLOUT, "", true, 0, 100)
        ));
        featureFlagRepository.save(flag);

        for (int i = 0; i < 10; i++) {
            UserContext context = new UserContext();
            context.setUserId("user-" + i);
            FeatureFlagEvaluationResponse result = featureFlagService.evaluateFlag(flagName, context);
            assertTrue(result.enabled(), "100% rollout should include all users");
        }
    }

    @Test
    void evaluateFlag_percentageRollout_0PercentIncludesNoUsers() {
        String flagName = "feature-0-pct-" + System.nanoTime();
        FeatureFlag flag = new FeatureFlag(flagName, false, List.of(
                new FeatureFlagRule(RuleAttribute.USER_ID, RuleOperator.PERCENTAGE_ROLLOUT, "", true, 0, 0)
        ));
        featureFlagRepository.save(flag);

        for (int i = 0; i < 10; i++) {
            UserContext context = new UserContext();
            context.setUserId("user-" + i);
            FeatureFlagEvaluationResponse result = featureFlagService.evaluateFlag(flagName, context);
            assertFalse(result.enabled(), "0% rollout should exclude all users");
        }
    }

    @Test
    void evaluateFlag_percentageRollout_approximatePercentageDistribution() {
        String flagName = "feature-dist-" + System.nanoTime();
        int percentage = 40;
        FeatureFlag flag = new FeatureFlag(flagName, false, List.of(
                new FeatureFlagRule(RuleAttribute.USER_ID, RuleOperator.PERCENTAGE_ROLLOUT, "", true, 0, percentage)
        ));
        featureFlagRepository.save(flag);

        int totalUsers = 500;
        int enabledCount = 0;

        for (int i = 0; i < totalUsers; i++) {
            UserContext context = new UserContext();
            context.setUserId("user-" + i);
            FeatureFlagEvaluationResponse result = featureFlagService.evaluateFlag(flagName, context);
            if (result.enabled()) {
                enabledCount++;
            }
        }

        double actualPercentage = (enabledCount * 100.0) / totalUsers;
        double tolerance = 5;
        assertTrue(actualPercentage >= percentage - tolerance && actualPercentage <= percentage + tolerance,
                String.format("Expected ~%d%%, got %.2f%%", percentage, actualPercentage));
    }

    @Test
    void evaluateFlag_percentageRolloutWithDefaultState() {
        String flagName = "feature-with-default-" + System.nanoTime();
        FeatureFlag flag = new FeatureFlag(flagName, true, List.of(
                new FeatureFlagRule(RuleAttribute.USER_ID, RuleOperator.PERCENTAGE_ROLLOUT, "", false, 0, 20)
        ));
        featureFlagRepository.save(flag);

        UserContext context = new UserContext();
        context.setUserId("user-not-in-rollout");

        FeatureFlagEvaluationResponse result = featureFlagService.evaluateFlag(flagName, context);
        assertTrue(result.enabled(), "Should use default state when rule does not match");
    }
}
