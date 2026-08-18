package com.anchor.app.safety

import com.anchor.app.storage.CrisisRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrisisResourcesTest {
    @Test
    fun mainlandHotlineFollowsAgeBoundary() {
        assertEquals("12356", crisisResource(CrisisRegion.MainlandChina, youth = false).hotline)
        assertEquals("12355", crisisResource(CrisisRegion.MainlandChina, youth = true).hotline)
        assertEquals("110 / 120 / 119", crisisResource(CrisisRegion.MainlandChina, youth = false).emergency)
    }

    @Test
    fun macauFallsBackToEmergencyCare() {
        val resource = crisisResource(CrisisRegion.Macau, youth = false)
        assertEquals("999", resource.emergency)
        assertTrue(resource.hotline.contains("就近"))
    }

    @Test
    fun somaticPrepIsAChecklistNotADiagnosis() {
        val titles = somaticPrepItems.map { it.title }
        assertTrue(titles.contains("甲状腺功能"))
        assertTrue(titles.contains("贫血"))
        assertTrue(somaticPrepItems.none { it.note.contains("确诊") || it.note.contains("必须") })
    }

    @Test
    fun guideHotlinesKeepNamedSupportLines() {
        val lines = guideHotlines(CrisisRegion.MainlandChina, youth = false)
        assertEquals("12356", lines.first().number)
        assertTrue(lines.any { it.number == "12356" })
        assertTrue(lines.none { it.number.contains("120") })
        assertTrue(guideHotlines(CrisisRegion.MainlandChina, youth = true).any { it.number == "12355" })
    }

    @Test
    fun designedRegionsUseDedicatedHotlineCards() {
        val mainland = medicalGuideContent(CrisisRegion.MainlandChina, youth = false)
        assertEquals(listOf("120", "110"), mainland.emergency.numbers)
        assertEquals("请立即拨打 120 或 110", mainland.emergency.actionLine())
        val beijing = mainland.hotlines.single { it.name.contains("北京") }
        assertEquals("010-82951332", beijing.number)
        assertEquals("800-810-1117", beijing.detail)

        val taiwan = medicalGuideContent(CrisisRegion.Taiwan, youth = false)
        assertEquals(listOf("110", "119"), taiwan.emergency.numbers)
        assertEquals(listOf("1925", "1995", "1980"), taiwan.hotlines.map { it.number })

        val hongKong = medicalGuideContent(CrisisRegion.HongKong, youth = false)
        assertEquals(listOf("999"), hongKong.emergency.numbers)
        assertTrue(hongKong.hotlines.any { it.number == "18111" })
        assertTrue(hongKong.hotlines.any { it.number == "2382 0000" })
        assertTrue(hongKong.hotlines.none { it.number == "2382 0777" })
        assertTrue(medicalGuideContent(CrisisRegion.HongKong, youth = true).hotlines.any { it.number == "2382 0777" })

        val usa = medicalGuideContent(CrisisRegion.UnitedStates, youth = false)
        assertEquals(listOf("911"), usa.emergency.numbers)
        assertEquals("988", usa.hotlines.single().number)
    }
}
