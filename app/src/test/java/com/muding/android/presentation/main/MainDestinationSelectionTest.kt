package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainDestinationSelectionTest {

    @Test
    fun `entering records from another destination requests records refresh`() {
        val result = selectMainDestination(
            currentDestination = MainDestination.HOME,
            targetDestination = MainDestination.RECORDS
        )

        assertEquals(MainDestination.RECORDS, result.destination)
        assertTrue(result.refreshRecords)
    }

    @Test
    fun `selecting records while already on records does not request duplicate refresh`() {
        val result = selectMainDestination(
            currentDestination = MainDestination.RECORDS,
            targetDestination = MainDestination.RECORDS
        )

        assertEquals(MainDestination.RECORDS, result.destination)
        assertFalse(result.refreshRecords)
    }

    @Test
    fun `selecting other destinations does not request records refresh`() {
        val result = selectMainDestination(
            currentDestination = MainDestination.RECORDS,
            targetDestination = MainDestination.SETTINGS
        )

        assertEquals(MainDestination.SETTINGS, result.destination)
        assertFalse(result.refreshRecords)
    }
}
