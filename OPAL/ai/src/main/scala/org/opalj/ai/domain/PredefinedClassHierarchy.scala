/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ClassHierarchy

/**
 * This class uses OPAL's `PreInitializedClassHierarchy` (see `ClassHierarchy` for details)
 * for class hierarchy related queries.
 *
 * '''Use this trait ONLY if you just want to do some testing.'''
 *
 * @author Michael Eichberg
 */
trait PredefinedClassHierarchy {

    /**
     * Returns the predefined class hierarchy.
     * OPAL's built-in default class hierarchy only reflects the type-hierarchy between the
     * most basic types – in particular between the exceptions potentially thrown
     * by JVM instructions.
     */
    final def classHierarchy: ClassHierarchy = PredefinedClassHierarchy.classHierarchy

}

object PredefinedClassHierarchy {

    final val classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy

}
