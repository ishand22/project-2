package com.entitleflow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {
    @Test void workflowExposesExplicitSuccessfulAndFailureStates(){
        assertEquals("COMPLETED",OrderStatus.COMPLETED.name());
        assertEquals("FAILED",OrderStatus.FAILED.name());
        assertEquals(7,OrderStatus.values().length);
    }
    @Test void assetLifecycleContainsAllocatedAndRetired(){
        assertTrue(java.util.Arrays.asList(AssetStatus.values()).contains(AssetStatus.ALLOCATED));
        assertTrue(java.util.Arrays.asList(AssetStatus.values()).contains(AssetStatus.RETIRED));
    }
}
