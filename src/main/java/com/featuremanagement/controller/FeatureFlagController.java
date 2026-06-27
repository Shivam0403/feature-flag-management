package com.featuremanagement.controller;

import com.featuremanagement.dto.FeatureFlagDtos.CreateFeatureFlagRequest;
import com.featuremanagement.dto.FeatureFlagDtos.CreateFeatureFlagResponse;
import com.featuremanagement.dto.FeatureFlagDtos.EvaluationRequest;
import com.featuremanagement.dto.FeatureFlagDtos.FeatureFlagEvaluationResponse;
import com.featuremanagement.dto.FeatureFlagDtos.FeatureFlagSummary;
import com.featuremanagement.dto.UserContext;
import com.featuremanagement.entity.FeatureFlag;
import com.featuremanagement.service.FeatureFlagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flags")
public class FeatureFlagController {
    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateFeatureFlagResponse> createFeatureFlag(@RequestBody CreateFeatureFlagRequest request) {
        FeatureFlag created = service.createFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateFeatureFlagResponse(created.getName(), created.isDefaultState(), created.getRules().size()));
    }

    @PostMapping("/{name}/evaluate")
    public ResponseEntity<FeatureFlagEvaluationResponse> evaluateFeatureFlag(
            @PathVariable String name,
            @RequestBody EvaluationRequest request) {
        UserContext context = new UserContext();
        context.setUserId(request.userId());
        context.setSubscriptionTier(request.subscriptionTier());
        context.setRegion(request.region());

        return ResponseEntity.ok(service.evaluateFlag(name, context));
    }

    @GetMapping
    public List<FeatureFlagSummary> listFlags() {
        return service.listFlags().stream()
                .map(flag -> new FeatureFlagSummary(flag.getName(), flag.isDefaultState(), flag.getRules().size()))
                .toList();
    }
}
