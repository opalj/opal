/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import scala.collection.immutable

/**
 * Default implementation of a mutable node of a graph.
 *
 * ==Thread Safety==
 * This is class is '''thread-safe'''.
 *
 * @author Michael Eichberg
 */
class DefaultMutableNode[I](
        theIdentifier:       I,
        identifierToString:  I => String                   = (_: Any).toString,
        theVisualProperties: immutable.Map[String, String] = immutable.Map.empty,
        theChildren:         List[DefaultMutableNode[I]]   = List.empty
) extends MutableNodeLike[I, DefaultMutableNode[I]](
    theIdentifier,
    identifierToString,
    theVisualProperties,
    theChildren
) with MutableNode[I, DefaultMutableNode[I]] {

    def this(
        identifier:         I,
        identifierToString: I => String,
        fillcolor:          Option[String]
    ) =
        this(
            identifier,
            identifierToString,
            theVisualProperties =
                fillcolor.map(c => DefaultMutableMode.BaseVirtualPropertiers + ("fillcolor" -> c)).
                    getOrElse(DefaultMutableMode.BaseVirtualPropertiers)
        )

}
object DefaultMutableMode {

    val BaseVirtualPropertiers = immutable.Map("style" -> "filled", "fillcolor" -> "white")

}
