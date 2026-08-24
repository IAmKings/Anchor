package com.anchor.app.safety

import com.anchor.app.storage.CrisisRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HelpNowCopyTest {
    @Test
    fun crisisHelpCopyUsesRegionalNumbersAndAvoidsStitchHopeLine() {
        val mainland = crisisResource(CrisisRegion.MainlandChina, youth = false)
        val youth = crisisResource(CrisisRegion.MainlandChina, youth = true)
        assertEquals("你现在不是一个人", helpNowTitle)
        assertEquals("拨打心理援助热线", helpNowStep1Title)
        assertEquals("前往精神科或急诊", helpNowStep2Title)
        assertEquals("告诉一位身边可信的人", helpNowStep3Title)
        assertEquals("如果你现在处于立即危险中，请直接拨打 ${mainland.emergency}。", helpNowImmediateDanger(mainland.emergency))
        assertEquals("立即危险请拨 ${mainland.emergency}", helpNowCallEmergency(mainland.emergency))
        assertEquals("12356", mainland.hotline)
        assertEquals("12355", youth.hotline)
        listOf(
            helpNowTitle, helpNowStep1Title, helpNowStep2Body, helpNowStep3Body,
            helpNowSomaticBody, helpNowMedicalQuote, helpNowImmediateDanger(mainland.emergency),
        ).forEach { line ->
            assertFalse(line.contains("希望24"))
            assertFalse(line.contains("好起来"))
            assertFalse(line.contains("通讯录"))
            assertFalse(line.contains("附近急诊"))
        }
        assertTrue(helpNowMedicalQuote.contains("判断这个不是你的工作"))
    }
}
