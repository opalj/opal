/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * This domain performs all computations at the type level and does
 * not track the flow of concrete values. Given the very high level of abstraction,
 * an abstract interpretation using this domain terminates quickly.
 *
 * This domain can be used as a foundation/as an inspiration for building specialized
 * [[Domain]]s. For example, it is useful to, e.g., track which types of values
 * are actually created to calculate a more precise call graph.
 *
 * @author Michael Eichberg
 */
trait TypeLevelDomain
    extends Domain
    with DefaultSpecialDomainValuesBinding
    with DefaultReferenceValuesBinding
    with DefaultTypeLevelIntegerValues
    with DefaultTypeLevelLongValues
    with TypeLevelLongValuesShiftOperators
    with TypeLevelPrimitiveValuesConversions
    with DefaultTypeLevelFloatValues
    with DefaultTypeLevelDoubleValues
    with TypeLevelFieldAccessInstructions
    with TypeLevelInvokeInstructions
    with TypeLevelDynamicLoads {
    this: Configuration with TheCode =>
}
