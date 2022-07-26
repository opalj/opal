/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.collection.immutable.UShortPair
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType

import scala.collection.immutable.ArraySeq

/**
 * Builder for a list of [[org.opalj.br.MethodTemplate]]s.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
case class METHODS[T](methods: ArraySeq[METHOD[T]]) {

    /**
     * Returns the build [[org.opalj.br.MethodTemplate]]s and their code annotations.
     */
    def result(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType
    )(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): ArraySeq[(br.MethodTemplate, Option[T])] = {
        methods.map[(br.MethodTemplate, Option[T])](m =>
            m.result(classFileVersion, declaringClassType))
    }

}

object METHODS {

    def apply[T](methods: METHOD[T]*): METHODS[T] = {
        new METHODS(ArraySeq.unsafeWrapArray(methods.toArray))
    }

}
