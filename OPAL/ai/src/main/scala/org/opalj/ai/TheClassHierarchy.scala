/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.ReferenceType
import org.opalj.br.ClassHierarchy

/**
 * Makes a project's class hierarchy available to a `Domain`.
 *
 * Implements a Domain's `isSubtypeOf(...)` by delegating to
 * the corresponding method defined in [[org.opalj.br.ClassHierarchy]].
 *
 * @author Michael Eichberg
 */
trait TheClassHierarchy {

    /**
     * This project's class hierarchy.
     *
     * Usually, just a redirect to the `Project`'s class hierarchy or the
     * default class hierarchy.
     */
    implicit def classHierarchy: ClassHierarchy

    /**
     * @see [[Domain.isASubtypeOf]]
     *
     * @see Delegates to [[org.opalj.br.ClassHierarchy]]'s `isSubtypeOf` method.
     */
    /*override*/ def isASubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {
        classHierarchy.isASubtypeOf(subtype, supertype)
    }

    /**
     * @see [[Domain.isSubtypeOf]]
     *
     * @see Delegates to [[org.opalj.br.ClassHierarchy]]'s `isSubtypeOf` method.
     */
    /*override*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Boolean = {
        classHierarchy.isSubtypeOf(subtype, supertype)
    }

}
