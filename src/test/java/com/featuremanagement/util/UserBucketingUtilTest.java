package com.featuremanagement.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserBucketingUtilTest {

    @Test
    void getBucketForUser_returnsDeterministicValue() {
        int bucket1 = UserBucketingUtil.getBucketForUser("user-123");
        int bucket2 = UserBucketingUtil.getBucketForUser("user-123");
        assertEquals(bucket1, bucket2, "Same userId should always return same bucket");
    }

    @Test
    void getBucketForUser_differentUsersGetDifferentBuckets() {
        int bucket1 = UserBucketingUtil.getBucketForUser("user-1");
        int bucket2 = UserBucketingUtil.getBucketForUser("user-2");
        assertNotEquals(bucket1, bucket2, "Different userIds should typically get different buckets");
    }

    @Test
    void getBucketForUser_bucketIsWithinValidRange() {
        for (int i = 0; i < 100; i++) {
            int bucket = UserBucketingUtil.getBucketForUser("user-" + i);
            assertTrue(bucket >= 0 && bucket < 10000, "Bucket should be between 0 and 9999");
        }
    }

    @Test
    void isUserInRollout_0PercentReturnsFalse() {
        assertFalse(UserBucketingUtil.isUserInRollout("user-123", 0));
        assertFalse(UserBucketingUtil.isUserInRollout("user-456", 0));
    }

    @Test
    void isUserInRollout_100PercentReturnsTrue() {
        assertTrue(UserBucketingUtil.isUserInRollout("user-123", 100));
        assertTrue(UserBucketingUtil.isUserInRollout("user-456", 100));
    }

    @Test
    void isUserInRollout_deterministicForSameUser() {
        boolean result1 = UserBucketingUtil.isUserInRollout("user-123", 50);
        boolean result2 = UserBucketingUtil.isUserInRollout("user-123", 50);
        assertEquals(result1, result2, "Same user and percentage should always produce same result");
    }

    @Test
    void isUserInRollout_approximatePercentageDistribution() {
        int percentage = 30;
        int totalUsers = 1000;
        int includedCount = 0;

        for (int i = 0; i < totalUsers; i++) {
            if (UserBucketingUtil.isUserInRollout("user-" + i, percentage)) {
                includedCount++;
            }
        }

        double actualPercentage = (includedCount * 100.0) / totalUsers;
        double lowerBound = percentage - 5;
        double upperBound = percentage + 5;

        assertTrue(actualPercentage >= lowerBound && actualPercentage <= upperBound,
                String.format("Expected ~%d%%, got %.2f%%", percentage, actualPercentage));
    }

    @Test
    void isUserInRollout_handlesNullUserId() {
        assertFalse(UserBucketingUtil.isUserInRollout(null, 50));
    }

    @Test
    void isUserInRollout_handlesBlankUserId() {
        assertFalse(UserBucketingUtil.isUserInRollout("", 50));
        assertFalse(UserBucketingUtil.isUserInRollout("   ", 50));
    }

    @Test
    void isUserInRollout_percentageEdgeCases() {
        // 0% should never include anyone
        assertFalse(UserBucketingUtil.isUserInRollout("user-1", 0));
        // Negative or over 100% should return false
        assertFalse(UserBucketingUtil.isUserInRollout("user-1", -1));
        assertFalse(UserBucketingUtil.isUserInRollout("user-1", 101));
        // 100% should always include everyone
        assertTrue(UserBucketingUtil.isUserInRollout("user-1", 100));
        // 50% should include roughly half the users
        int count50pct = 0;
        for (int i = 0; i < 100; i++) {
            if (UserBucketingUtil.isUserInRollout("user-" + i, 50)) {
                count50pct++;
            }
        }
        assertTrue(count50pct > 30 && count50pct < 70, "50% rollout should include roughly half the users");
    }
}
