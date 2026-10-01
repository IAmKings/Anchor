package com.anchor.app.safety

import com.anchor.app.storage.CrisisRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneDialTest {
    @Test
    fun splitsEmergencyNumbersAndSkipsProse() {
        assertEquals(listOf("110", "120", "119"), dialableNumbers("110 / 120 / 119"))
        assertEquals(listOf("2382 0000"), dialableNumbers("2382 0000"))
        assertEquals("tel:23820000", phoneTelUri("2382 0000"))
        assertEquals("tel:01082951332", phoneTelUri("010-82951332"))
        assertEquals("tel:+861055", phoneTelUri("+86 10 55"))
        assertEquals(emptyList(), dialableNumbers("请前往就近精神科或急诊"))
        assertEquals(emptyList(), dialableNumbers("当地紧急电话"))
        assertEquals(emptyList(), dialableNumbers("请查询当地心理援助资源"))
        assertNull(phoneTelUri("NHS 111 转 2"))
        assertEquals(listOf("116 123"), dialableNumbers("NHS 111 转 2；116 123"))
        assertFalse(isEntirelyDialable("NHS 111 转 2；116 123"))
        assertTrue(isEntirelyDialable("12356"))
        assertTrue(isEntirelyDialable("18111 / 2896 0000"))
    }

    @Test
    fun beijingAlternateNumberIsDialable() {
        val beijing = medicalGuideContent(CrisisRegion.MainlandChina, youth = false)
            .hotlines.single { it.name.contains("北京") }
        assertEquals(listOf("010-82951332", "800-810-1117"), beijing.phoneNumbers())
        assertEquals("", beijing.note())
        val national = medicalGuideContent(CrisisRegion.MainlandChina, youth = false).hotlines.first()
        assertEquals(listOf("12356"), national.phoneNumbers())
        assertEquals("国家卫健委设立", national.note())
    }

    @Test
    fun ukHotlineKeepsTheProseBesideTheDialablePiece() {
        val uk = medicalGuideContent(CrisisRegion.UnitedKingdom, youth = false).hotlines.single()
        assertEquals(listOf("116 123"), uk.phoneNumbers())
        assertEquals("", uk.note())
        assertFalse(isEntirelyDialable(uk.number))
    }
}
