package com.wsamon.schedulephototoecal.reconcile

data class ReconciliationCounts(
    val added: Int,
    val updated: Int,
    val removed: Int,
    val unchanged: Int,
) {
    companion object {
        fun of(plan: List<ShiftReconciliationAction>): ReconciliationCounts {
            var added = 0
            var updated = 0
            var removed = 0
            var unchanged = 0
            for (action in plan) {
                when (action) {
                    is ShiftReconciliationAction.Insert -> added++
                    is ShiftReconciliationAction.Update -> updated++
                    is ShiftReconciliationAction.Delete -> removed++
                    is ShiftReconciliationAction.NoOp ->
                        if (action.reason == NoOpReason.UNCHANGED) unchanged++
                }
            }
            return ReconciliationCounts(added = added, updated = updated, removed = removed, unchanged = unchanged)
        }
    }
}
