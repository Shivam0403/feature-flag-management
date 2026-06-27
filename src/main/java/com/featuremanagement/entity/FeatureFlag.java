package com.featuremanagement.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean defaultState;

    @OneToMany(mappedBy = "featureFlag", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FeatureFlagRule> rules = new ArrayList<>();

    public FeatureFlag() {}

    public FeatureFlag(String name, boolean defaultState, List<FeatureFlagRule> rules) {
        this.name = name;
        this.defaultState = defaultState;
        setRules(rules);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefaultState() {
        return defaultState;
    }

    public void setDefaultState(boolean defaultState) {
        this.defaultState = defaultState;
    }

    public List<FeatureFlagRule> getRules() {
        return rules;
    }

    public void setRules(List<FeatureFlagRule> rules) {
        this.rules.clear();
        if (rules != null) {
            rules.forEach(this::addRule);
        }
    }

    public void addRule(FeatureFlagRule rule) {
        rule.setFeatureFlag(this);
        this.rules.add(rule);
    }
}
