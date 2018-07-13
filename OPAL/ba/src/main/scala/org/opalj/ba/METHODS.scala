/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.collection.immutable.UShortPair
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType

/**
 * Builder for a list of [[org.opalj.br.MethodTemplate]]s.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
class METHODS[T](private[this] var methods: Seq[METHOD[T]]) {

    /**
     * Returns the build [[org.opalj.br.MethodTemplate]] and its code annotations.
     */
    def result(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType
    )(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): IndexedSeq[(br.MethodTemplate, Option[T])] = {
        IndexedSeq.empty ++ methods.iterator.map(m ⇒ m.result(classFileVersion, declaringClassType))
    }

}

object METHODS {

    def apply[T](methods: METHOD[T]*): METHODS[T] = new METHODS(methods)

}
