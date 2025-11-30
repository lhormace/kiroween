package com.health.chat;

import net.jqwik.api.*;

/**
 * Verification test to ensure jqwik is properly configured
 */
public class SetupVerificationTest {

    @Property
    void jqwikIsWorkingCorrectly(@ForAll int number) {
        // Simple property test to verify jqwik setup
        Assume.that(number >= 0);
        assert number >= 0;
    }
}
