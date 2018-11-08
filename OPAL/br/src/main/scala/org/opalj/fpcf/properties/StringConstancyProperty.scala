/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties

import org.opalj.br.Field
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.properties.StringConstancyLevel.CONSTANT
import org.opalj.fpcf.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.properties.StringConstancyLevel.PARTIALLY_CONSTANT

import scala.collection.mutable.ArrayBuffer

sealed trait StringConstancyPropertyMetaInformation extends PropertyMetaInformation {
    type Self = StringConstancyProperty
}

/**
 * Values in this enumeration represent the granularity of used strings.
 *
 * @author Patrick Mell
 */
object StringConstancyLevel extends Enumeration {

    type StringConstancyLevel = StringConstancyLevel.Value

    /**
     * This level indicates that a string has a constant value at a given read operation.
     */
    final val CONSTANT = Value("constant")

    /**
     * This level indicates that a string is partially constant (constant + dynamic part) at some
     * read operation, that is, the initial value of a string variable needs to be preserved. For
     * instance, it is fine if a string variable is modified after its initialization by
     * appending another string, s2. Later, s2 might be removed partially or entirely without
     * violating the constraints of this level.
     */
    final val PARTIALLY_CONSTANT = Value("partially_constant")

    /**
     * This level indicates that a string at some read operations has an unpredictable value.
     */
    final val DYNAMIC = Value("dynamic")

}

class StringConstancyProperty(
        val constancyLevel:  StringConstancyLevel.Value,
        val possibleStrings: ArrayBuffer[String]
) extends Property with StringConstancyPropertyMetaInformation {

    final def key = StringConstancyProperty.key

    override def toString: String = {
        val ps = possibleStrings.mkString("[", ", ", "]")
        s"StringConstancyProperty { Constancy Level: $constancyLevel; Possible Strings: $ps }"
    }

}

object StringConstancyProperty extends StringConstancyPropertyMetaInformation {

    final val PropertyKeyName = "StringConstancy"

    final val key: PropertyKey[StringConstancyProperty] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, _: FallbackReason, e: Entity) ⇒ {
                // TODO: Using simple heuristics, return a better value for some easy cases
                StringConstancyProperty(DYNAMIC, ArrayBuffer("*"))
            },
            (_, eps: EPS[Field, StringConstancyProperty]) ⇒ eps.ub,
            (_: PropertyStore, _: Entity) ⇒ None
        )
    }

    def apply(
        constancyLevel:  StringConstancyLevel.Value,
        possibleStrings: ArrayBuffer[String]
    ): StringConstancyProperty = new StringConstancyProperty(constancyLevel, possibleStrings)

    /**
     * This function takes an array of [[StringConstancyProperty]] and reduces it. This means that
     * the most-general [[StringConstancyLevel]] encountered in the given properties is returned
     * ([[StringConstancyLevel.CONSTANT]] is the most static >
     * [[StringConstancyLevel.PARTIALLY_CONSTANT]] > [[StringConstancyLevel.DYNAMIC]] is the most-
     * general) along with the union of all possible strings. Note that this union contains every
     * possible string only once (also the "*" marker)! "*" might be contained as well as (semi)
     * constant strings to convey all possibilities.
     *
     * @param properties The properties to reduce.
     * @return Returns a single [[StringConstancyProperty]] with values as described above. In case
     *         the given `properties` array is empty, `None` will be returned.
     */
    def reduce(properties: Array[StringConstancyProperty]): Option[StringConstancyProperty] = {
        if (properties.isEmpty) {
            return None
        }

        val possibleValues = ArrayBuffer[String]()
        var level = CONSTANT
        properties.foreach { next ⇒
            if ((level == CONSTANT) ||
                (level == PARTIALLY_CONSTANT && next.constancyLevel != CONSTANT)) {
                level = next.constancyLevel
            }
            possibleValues.appendAll(next.possibleStrings)
        }

        Some(StringConstancyProperty(level, possibleValues.distinct))
    }

}
