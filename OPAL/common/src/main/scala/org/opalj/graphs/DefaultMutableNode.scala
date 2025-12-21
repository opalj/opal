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
    theVisualProperties: immutable.Map[String, String] = immutable.Map.empty[String, String],
    theChildren:         List[DefaultMutableNode[I]]   = List.empty[DefaultMutableNode[I]]
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
                fillcolor.map(c => DefaultMutableMode.BaseVirtualProperties + ("fillcolor" -> c))
                    .getOrElse(DefaultMutableMode.BaseVirtualProperties)
        )

}

object DefaultMutableMode {

    val BaseVirtualProperties = immutable.Map("style" -> "filled", "fillcolor" -> "white")

}
