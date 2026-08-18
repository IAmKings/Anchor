package com.anchor.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class M0ChecklistTest {
    @Test
    fun tracksOnlyVerifiedCapabilities() {
        val initial = M0Checklist()
        assertEquals(0, initial.completed)
        assertEquals(7, initial.total)
        assertFalse(initial.isPassed(M0Check.EncryptedStorage))

        val updated = initial.markPassed(M0Check.EncryptedStorage)
        assertEquals(1, updated.completed)
        assertTrue(updated.isPassed(M0Check.EncryptedStorage))
        assertEquals(0, initial.completed)
    }
}
