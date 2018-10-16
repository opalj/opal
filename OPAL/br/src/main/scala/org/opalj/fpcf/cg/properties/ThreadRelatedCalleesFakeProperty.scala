/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

sealed trait ThreadRelatedCalleesFakePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ThreadRelatedCalleesFakeProperty
}

/**
 * A fake property used internally to allow dependencies to the tac property in the thread related
 * calls analyses.
 *
 * @author Florian Kuebler
 */
sealed abstract class ThreadRelatedCalleesFakeProperty
    extends Property with OrderedProperty with ThreadRelatedCalleesFakePropertyMetaInformation {

    override def key: PropertyKey[ThreadRelatedCalleesFakeProperty] = ThreadRelatedCalleesFakeProperty.key
}

object ThreadRelatedCalleesFinal extends ThreadRelatedCalleesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: ThreadRelatedCalleesFakeProperty
    ): Unit = {}
}
object ThreadRelatedCalleesNonFinal extends ThreadRelatedCalleesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: ThreadRelatedCalleesFakeProperty
    ): Unit = {
        if (other eq ThreadRelatedCalleesFinal) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }
}

object ThreadRelatedCalleesFakeProperty extends ThreadRelatedCalleesFakePropertyMetaInformation {
    final val key: PropertyKey[ThreadRelatedCalleesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "ThreadRelatedCalleesFakeProperty", ThreadRelatedCalleesFinal
        )
    }
}
