/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

/**
 * Documents the string constancy that a string tree consisting of [[StringTreeNode]] has, meaning a summary whether the
 * string tree in question is invalid, has at most constant values, has at most constant values concatenated with
 * dynamic values or also contains un-concatenated dynamic values. The companion object also defines useful combination
 * functions for instances of this trait.
 *
 * @author Maximilian Rüsch
 */
sealed trait StringConstancyLevel

object StringConstancyLevel {

    /**
     * Indicates that a string has no value at a given read operation.
     */
    case object Invalid extends StringConstancyLevel

    /**
     * Indicates that a string has a constant value at a given read operation.
     */
    case object Constant extends StringConstancyLevel

    /**
     * Indicates that a string is partially constant (has a constant and a dynamic part) at some read operation.
     */
    case object PartiallyConstant extends StringConstancyLevel

    /**
     * Indicates that a string at some read operations has an unpredictable value.
     */
    case object Dynamic extends StringConstancyLevel

    /**
     * The more general StringConstancyLevel of the two given levels, i.e. the one that allows more possible
     * values at the given read operation.
     *
     * @param level1 The first level.
     * @param level2 The second level.
     * @return Returns the more general level of both given inputs.
     */
    def determineMoreGeneral(
        level1: StringConstancyLevel,
        level2: StringConstancyLevel
    ): StringConstancyLevel = {
        if (level1 == Dynamic || level2 == Dynamic) {
            Dynamic
        } else if (level1 == PartiallyConstant || level2 == PartiallyConstant) {
            PartiallyConstant
        } else if (level1 == Constant || level2 == Constant) {
            Constant
        } else {
            Invalid
        }
    }

    /**
     * Returns the StringConstancyLevel of a concatenation of two values.
     * <ul>
     *   <li>Constant + Constant = Constant</li>
     *   <li>Dynamic + Dynamic = Dynamic</li>
     *   <li>Constant + Dynamic = PartiallyConstant</li>
     *   <li>PartiallyConstant + {Dynamic, Constant} = PartiallyConstant</li>
     * </ul>
     *
     * @param level1 The first level.
     * @param level2 The second level.
     * @return Returns the level for a concatenation.
     */
    def determineForConcat(
        level1: StringConstancyLevel,
        level2: StringConstancyLevel
    ): StringConstancyLevel = {
        if (level1 == Invalid || level2 == Invalid) {
            Invalid
        } else if (level1 == PartiallyConstant || level2 == PartiallyConstant) {
            PartiallyConstant
        } else if ((level1 == Constant && level2 == Dynamic) || (level1 == Dynamic && level2 == Constant)) {
            PartiallyConstant
        } else {
            level1
        }
    }
}
