package com.anchor.app.safety

internal const val helpNowTitle = "你现在不是一个人"
internal const val helpNowStep1Label = "第一步"
internal const val helpNowStep1Title = "拨打心理援助热线"
internal const val helpNowStep2Label = "第二步"
internal const val helpNowStep2Title = "前往精神科或急诊"
internal const val helpNowStep2Body = "去最近的医院急诊，或直接预约精神科。"
internal const val helpNowStep3Label = "第三步"
internal const val helpNowStep3Title = "告诉一位身边可信的人"
internal const val helpNowStep3Body = "找一个你信得过的朋友或家人，告诉他们你现在很难受。"
internal const val helpNowSomaticTitle = "躯体因素也请一起查"
internal const val helpNowSomaticBody =
    "甲状腺异常、贫血、维生素缺乏、慢性疼痛或药物副作用等身体因素也可能表现为情绪症状。"
internal const val helpNowChecklistAction = "查看就医准备清单"
internal const val helpNowGuideAction = "查看就医指南"
internal const val helpNowMedicalQuote = "判断这个不是你的工作，是医生的工作。去做一次评估，是为了终结自我审判。"

internal fun helpNowImmediateDanger(emergency: String): String =
    "如果你现在处于立即危险中，请直接拨打 $emergency。"

internal fun helpNowCallEmergency(emergency: String): String = "立即危险请拨 $emergency"

internal const val helpNowEnterWaiting = "先进入就医等待期"
