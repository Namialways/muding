package com.muding.android.presentation.main

import kotlin.math.roundToInt

enum class RecordRetentionTarget(
    val title: String,
    val itemUnit: String,
    val countOptionsPreset: List<Int>,
    val dayOptionsPreset: List<Int>
) {
    PIN_HISTORY(
        title = "\u8d34\u56fe\u5386\u53f2",
        itemUnit = "\u6761",
        countOptionsPreset = listOf(20, 50, 100, 200),
        dayOptionsPreset = listOf(7, 14, 30, 90)
    ),
    WORK_RECORDS(
        title = "\u5de5\u4f5c\u8bb0\u5f55",
        itemUnit = "\u9879",
        countOptionsPreset = listOf(10, 30, 50, 100),
        dayOptionsPreset = listOf(7, 14, 30, 90)
    )
}

data class RecordRetentionSheetModel(
    val target: RecordRetentionTarget,
    val count: Int,
    val days: Int,
    val countOptions: List<Int>,
    val dayOptions: List<Int>
) {
    val summary: String
        get() = formatRecordRetentionSummary(count = count, days = days, itemUnit = target.itemUnit)
}

enum class RetentionCustomField {
    COUNT,
    DAYS
}

data class RetentionCustomEditorConfig(
    val valueRange: IntRange,
    val sliderSteps: Int,
    val suffix: String
)

fun buildRecordRetentionSheetModel(
    target: RecordRetentionTarget,
    count: Int,
    days: Int
): RecordRetentionSheetModel {
    return RecordRetentionSheetModel(
        target = target,
        count = count.coerceIn(1, 500),
        days = days.coerceIn(1, 365),
        countOptions = target.countOptionsPreset,
        dayOptions = target.dayOptionsPreset
    )
}

fun buildRetentionCustomEditorConfig(
    target: RecordRetentionTarget,
    field: RetentionCustomField
): RetentionCustomEditorConfig {
    val valueRange = when (field) {
        RetentionCustomField.COUNT -> 1..500
        RetentionCustomField.DAYS -> 1..365
    }
    val suffix = when (field) {
        RetentionCustomField.COUNT -> target.itemUnit
        RetentionCustomField.DAYS -> "\u5929"
    }
    return RetentionCustomEditorConfig(
        valueRange = valueRange,
        sliderSteps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
        suffix = suffix
    )
}

fun snapRetentionSliderValue(
    rawValue: Float,
    range: IntRange
): Int {
    return rawValue.roundToInt().coerceIn(range)
}

fun formatRecordRetentionSummary(
    count: Int,
    days: Int,
    itemUnit: String
): String {
    return "${count.coerceAtLeast(1)} $itemUnit / ${days.coerceAtLeast(1)} \u5929"
}
