package com.featuremanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for deterministic user bucketing based on userId hash.
 * Ensures the same userId always receives the same rollout decision.
 */
public class UserBucketingUtil {

    private static final int BUCKET_RANGE = 10000;

    /**
     * Get the bucket for a user based on their userId hash.
     * Returns a value between 0 and 9999 (0-100% coverage).
     *
     * @param userId the user identifier
     * @return bucket number between 0 and 9999
     */
    public static int getBucketForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            int hashValue = ((hash[0] & 0xFF) << 24) |
                           ((hash[1] & 0xFF) << 16) |
                           ((hash[2] & 0xFF) << 8) |
                           (hash[3] & 0xFF);
            return Math.abs(hashValue) % BUCKET_RANGE;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if a user should be included in a percentage rollout.
     *
     * @param userId the user identifier
     * @param percentage the rollout percentage (0-100)
     * @return true if the user is in the rollout group
     */
    public static boolean isUserInRollout(String userId, int percentage) {
        // Invalid or out-of-range percentages
        if (percentage <= 0 || percentage > 100) {
            return false;
        }
        // Null or blank userId
        if (userId == null || userId.isBlank()) {
            return false;
        }
        // 100% rollout
        if (percentage >= 100) {
            return true;
        }
        int bucket = getBucketForUser(userId);
        int threshold = (percentage * BUCKET_RANGE) / 100;
        return bucket < threshold;
    }
}
