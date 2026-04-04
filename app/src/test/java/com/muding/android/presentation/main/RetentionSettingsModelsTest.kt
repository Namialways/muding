package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Test

class RetentionSettingsModelsTest {

    @Test
    fun `sheet model uses fixed pin history presets`() {
        val model = buildRecordRetentionSheetModel(
            target = RecordRetentionTarget.PIN_HISTORY,
            count = 50,
            days = 14
        )

        assertEquals(listOf(20, 50, 100, 200), model.countOptions)
        assertEquals(listOf(7, 14, 30, 90), model.dayOptions)
    }

    @Test
    fun `work record sheet model keeps fixed presets even for uncommon values`() {
        val model = buildRecordRetentionSheetModel(
            target = RecordRetentionTarget.WORK_RECORDS,
            count = 75,
            days = 45
        )

        assertEquals(listOf(10, 30, 50, 100), model.countOptions)
        assertEquals(listOf(7, 14, 30, 90), model.dayOptions)
    }

    @Test
    fun `custom editor config uses full count range with discrete slider steps`() {
        val config = buildRetentionCustomEditorConfig(
            target = RecordRetentionTarget.PIN_HISTORY,
            field = RetentionCustomField.COUNT
        )

        assertEquals(1..500, config.valueRange)
        assertEquals(498, config.sliderSteps)
        assertEquals("条", config.suffix)
    }

    @Test
    fun `snap retention slider value rounds and clamps into range`() {
        assertEquals(1, snapRetentionSliderValue(0.4f, 1..500))
        assertEquals(43, snapRetentionSliderValue(42.6f, 1..500))
        assertEquals(365, snapRetentionSliderValue(400f, 1..365))
    }
}
