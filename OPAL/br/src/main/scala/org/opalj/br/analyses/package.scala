/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.collection.Map
import scala.collection.immutable.ArraySeq

/**
 * Defines commonly useful type aliases.
 *
 * @author Michael Eichberg
 */
package object analyses {

    /**
     * Type alias for Projects with arbitrary sources.
     */
    type SomeProject = Project[_]

    type ProjectInformationKeys = si.ProjectInformationKeys

    type StringConstantsInformation = Map[String, ArraySeq[PCInMethod]]

    implicit object MethodDeclarationContextOrdering extends Ordering[MethodDeclarationContext] {
        def compare(x: MethodDeclarationContext, y: MethodDeclarationContext): Int = x compare y
    }

}
