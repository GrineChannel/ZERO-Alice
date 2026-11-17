package com.zeroproject.alice.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AliceTaskExecutionPolicyTest {
    @Test fun expectedPendingStepCanAdvance() {
        assertTrue(AliceTaskExecutionPolicy.canAdvance("planned", 0, 0, 2))
        assertTrue(AliceTaskExecutionPolicy.canAdvance("in_progress", 1, 1, 2))
    }

    @Test fun staleOrFinishedStepsCannotAdvance() {
        assertFalse(AliceTaskExecutionPolicy.canAdvance("cancelled", 0, 0, 2))
        assertFalse(AliceTaskExecutionPolicy.canAdvance("completed", 2, 2, 2))
        assertFalse(AliceTaskExecutionPolicy.canAdvance("in_progress", 1, 0, 2))
        assertFalse(AliceTaskExecutionPolicy.canAdvance("planned", 0, -1, 2))
        assertFalse(AliceTaskExecutionPolicy.canAdvance("planned", 0, 2, 2))
    }

    @Test fun destructiveAndPreviewStepsNeedExplicitExecution() {
        assertTrue(AliceTaskExecutionPolicy.requiresSingleStep("предпросмотр замены элис test.txt :: hi"))
        assertTrue(AliceTaskExecutionPolicy.requiresSingleStep("замени файл элис test.txt :: hi"))
        assertTrue(AliceTaskExecutionPolicy.requiresSingleStep("откати файл элис test.txt"))
        assertTrue(AliceTaskExecutionPolicy.requiresSingleStep("разработай minecraft элис abc :: мод"))
        assertFalse(AliceTaskExecutionPolicy.requiresSingleStep("создай папку элис demo"))
        assertFalse(AliceTaskExecutionPolicy.requiresSingleStep("создай файл элис demo.txt :: hi"))
    }
}
