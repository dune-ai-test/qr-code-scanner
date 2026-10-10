package com.quickscan.feature.history

import com.quickscan.data.barcode.PayloadType

/**
 * Which of the ticked types the selection bar's actions currently apply to.
 *
 * Scoping is what turns the existing bulk actions into per-type ones: with
 * Links and Wi-Fi ticked, turning Wi-Fi off and deleting removes only the
 * links. Null means no scope — every ticked row is in play, which is also the
 * state a selection starts in.
 */
internal fun nextTypeScope(
    /** Types actually present in the selection. */
    present: Set<PayloadType>,
    active: Set<PayloadType>?,
    type: PayloadType,
): Set<PayloadType>? {
    if (type !in present) return active
    if (active == null) {
        // First tap narrows: everything except the type that was turned off.
        val narrowed = present - type
        return narrowed.ifEmpty { present }
    }
    val next = if (type in active) active - type else active + type
    // Turning the last type off would leave the bar with nothing to act on, so
    // that tap re-selects everything instead.
    return next.ifEmpty { present }
}

/** The ids an action applies to, given the scope the user has set. */
internal fun idsInScope(
    selection: Set<Long>,
    typeById: Map<Long, PayloadType>,
    active: Set<PayloadType>?,
): Set<Long> {
    if (active == null) return selection
    return selection.filterTo(mutableSetOf()) { typeById[it] in active }
}