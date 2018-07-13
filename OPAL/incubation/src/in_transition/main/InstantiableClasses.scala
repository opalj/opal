/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

/**
 * Stores the information about those class that are not instantiable. The set of
 * classes that are not instantiable is usually only a small fraction of all classes
 * and hence, more efficient to store/query than those which are instantiable.
 *
 * A class is considered instantiable if it is possible that at some point in time an
 * instance of the respective class is created (via a direct constructor call, a factory method
 * call, an indirect instance creation by means of creating an instance of a subtype).
 *
 * An example of a class which is not instaniable is a class which defines a private constructor
 * which is not called by other (factory) methods and which is also not serializable. A class
 * which defines no constructor at all (not possible using Java, but still valid bytecode) is
 * also not instantiable.
 *
 * @author Michael Eichberg
 */
// RENAME => MayHaveInstances
class InstantiableClasses(
        val project:         SomeProject,
        val notInstantiable: Set[ObjectType]
) {

    def isNotInstantiable(classType: ObjectType): Boolean = notInstantiable.contains(classType)

    def statistics: Map[String, Int] = Map(
        "# of not instantiable classes in the project" → notInstantiable.size
    )

    override def toString: String = notInstantiable.mkString("Not instantiable: ", ", ", ".")

}
