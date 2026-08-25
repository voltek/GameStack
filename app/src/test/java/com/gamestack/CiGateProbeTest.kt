package com.gamestack

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEMPORARY CI PROBE — not a real regression test.
 * Exists only to prove the "build / test / lint" required status check
 * blocks a PR. Delete this file once that is verified; never merge it.
 */
class CiGateProbeTest {

    @Test
    fun `TEMPORARY CI PROBE this must fail to prove the gate blocks the PR`() {
        assertTrue("probe flipped green: the same PR should now unblock", true)
    }
}
