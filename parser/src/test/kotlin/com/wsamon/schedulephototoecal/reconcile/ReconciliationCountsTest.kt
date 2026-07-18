package com.wsamon.schedulephototoecal.reconcile

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.ParseConfidence
import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Test

class ReconciliationCountsTest {

    private val date = LocalDate.of(2026, 7, 12)

    private fun shift(included: Boolean = true) = ParsedShift(
        date = date,
        notScheduled = false,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        position = "Grocery Clerk",
        storeNumber = "1309",
        rawOcrText = "",
        confidence = ParseConfidence.HIGH,
        included = included,
    )

    private val content = ShiftEventContentBuilder.build(shift(), ZoneOffset.UTC)!!

    @Test
    fun `counts a mixed plan into the right buckets`() {
        val plan = listOf(
            ShiftReconciliationAction.Insert(shift(), content),
            ShiftReconciliationAction.Insert(shift(), content),
            ShiftReconciliationAction.Update(shift(), 1L, content),
            ShiftReconciliationAction.Delete(shift(), 2L),
            ShiftReconciliationAction.NoOp(shift(), NoOpReason.UNCHANGED),
            ShiftReconciliationAction.NoOp(shift(), NoOpReason.UNCHANGED),
            ShiftReconciliationAction.NoOp(shift(included = false), NoOpReason.EXCLUDED),
            ShiftReconciliationAction.NoOp(shift(), NoOpReason.NOT_SCHEDULED_NO_EXISTING_EVENT),
        )

        val counts = ReconciliationCounts.of(plan)

        assertThat(counts.added).isEqualTo(2)
        assertThat(counts.updated).isEqualTo(1)
        assertThat(counts.removed).isEqualTo(1)
        assertThat(counts.unchanged).isEqualTo(2)
    }

    @Test
    fun `empty plan yields all zero counts`() {
        val counts = ReconciliationCounts.of(emptyList())
        assertThat(counts).isEqualTo(ReconciliationCounts(added = 0, updated = 0, removed = 0, unchanged = 0))
    }
}
