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
    extends Property with ThreadRelatedCalleesFakePropertyMetaInformation {

    override def key: PropertyKey[ThreadRelatedCalleesFakeProperty] = ThreadRelatedCalleesFakeProperty.key
}

object ThreadRelatedCalleesFinal extends ThreadRelatedCalleesFakeProperty

object ThreadRelatedCalleesNonFinal extends ThreadRelatedCalleesFakeProperty

object ThreadRelatedCalleesFakeProperty extends ThreadRelatedCalleesFakePropertyMetaInformation {
    final val key: PropertyKey[ThreadRelatedCalleesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "ThreadRelatedCalleesFakeProperty", ThreadRelatedCalleesFinal
        )
    }
}
