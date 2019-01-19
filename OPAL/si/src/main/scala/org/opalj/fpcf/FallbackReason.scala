/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Specifies the reason why a fallback is used.
 */
sealed trait FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean
}
/**
 * The fallback is used, because the property was queried, but was not explicitly computed in the
 * past, is not computed now and will also not be computed in the future.
 */
case object PropertyIsNotComputedByAnyAnalysis extends FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean = true
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean = false
}

/**
 * The fallback is used, because the property was queried/is required, but the property was
 * not computed for the specific entity though an analysis is scheduled/executed.
 *
 * @note This may happen for properties associated with dead code/code that is no used by the
 *       current project. E.g., the callers property of an unused library method is most
 *       likely not computed. If it is queried, then this is the Property that should be returned.
 */
case object PropertyIsNotDerivedByPreviouslyExecutedAnalysis extends FallbackReason {
    def propertyIsNotComputedByAnyAnalysis: Boolean = false
    def propertyIsNotDerivedByPreviouslyExecutedAnalysis: Boolean = true
}
