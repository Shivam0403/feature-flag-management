package com.featuremanagement.service;

import com.featuremanagement.dto.FeatureFlagDtos.CreateFeatureFlagRequest;
import com.featuremanagement.dto.FeatureFlagDtos.FeatureFlagEvaluationResponse;
import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlag;
import com.featuremanagement.entity.FeatureFlagRule;
import com.featuremanagement.enums.RuleAttribute;
import com.featuremanagement.enums.RuleOperator;
import com.featuremanagement.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeatureFlagService {
    private final FeatureFlagRepository repository;
    private final Map<String, FeatureFlag> cache = new ConcurrentHashMap<>();

    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FeatureFlag createFlag(CreateFeatureFlagRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Feature flag already exists: " + request.name());
        }

        List<FeatureFlagRule> rules = request.rules() == null ? List.of() : request.rules().stream()
                .map(ruleRequest -> {
                    RuleOperator op = RuleOperator.from(ruleRequest.operator());
                    if (op == RuleOperator.PERCENTAGE_ROLLOUT) {
                        return new FeatureFlagRule(
                                RuleAttribute.USER_ID,
                                op,
                                "",
                                ruleRequest.state(),
                                ruleRequest.priority() == null ? 0 : ruleRequest.priority(),
                                ruleRequest.percentageRollout());
                    } else {
                        return new FeatureFlagRule(
                                RuleAttribute.from(ruleRequest.attribute()),
                                op,
                                String.join(",", ruleRequest.values() == null ? List.of() : ruleRequest.values()),
                                ruleRequest.state(),
                                ruleRequest.priority() == null ? 0 : ruleRequest.priority());
                    }
                })
                .toList();

        FeatureFlag flag = new FeatureFlag(request.name(), request.defaultState(), rules);
        FeatureFlag saved = repository.save(flag);
        cache.put(saved.getName(), saved);
        return saved;
    }

    public FeatureFlagEvaluationResponse evaluateFlag(String name, UserContext userContext) {
        FeatureFlag flag = cache.computeIfAbsent(name, this::loadFlagByName);
        boolean enabled = evaluate(flag, userContext);
        return new FeatureFlagEvaluationResponse(flag.getName(), enabled, enabled ? "ON" : "OFF");
    }

    private FeatureFlag loadFlagByName(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + name));
    }

    private boolean evaluate(FeatureFlag flag, UserContext userContext) {
        return flag.getRules().stream()
                .sorted(Comparator.comparingInt(FeatureFlagRule::getPriority))
                .filter(rule -> rule.matches(userContext))
                .findFirst()
                .map(FeatureFlagRule::isState)
                .orElse(flag.isDefaultState());
    }

    public List<FeatureFlag> listFlags() {
        return repository.findAll();
    }
}
