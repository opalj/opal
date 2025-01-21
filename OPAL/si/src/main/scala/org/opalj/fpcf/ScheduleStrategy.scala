package org.opalj
package fpcf

sealed trait ScheduleStrategy

object ScheduleStrategy {
    case object SPS extends ScheduleStrategy // Single Phase Strategy
    case object MPS extends ScheduleStrategy // Multi Phase Strategy
    case object IPMS extends ScheduleStrategy // Independent Phase Merge Strategy
    case object OPMS extends ScheduleStrategy // Optimized Phase Merge Strategy
}
