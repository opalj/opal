/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Specifies which parts of a class file should be compared with another one.
 *
 * @author Timothy Earley
 */
abstract class SimilarityTestConfiguration {

    /**
     * Selects those fields which should be compared. By default all fields are selected.
     */
    def compareFields(
        leftContext: ClassFile,
        left:        Iterable[JVMField],
        right:       Iterable[JVMField]
    ): (Iterable[JVMField], Iterable[JVMField])

    /**
     * Selects those methods which should be compared. By default all methods are selected.
     *
     * If, e.g., the `left` methods belong to the class which is derived from the `right` one
     * and should contain all methods except of the default constructor, then the default
     * constructor should be filtered from the right set of methods.
     */
    def compareMethods(
        leftContext: ClassFile,
        left:        Iterable[JVMMethod],
        right:       Iterable[JVMMethod]
    ): (Iterable[JVMMethod], Iterable[JVMMethod])

    /**
     * Selects the attributes which should be compared.
     */
    def compareAttributes(
        leftContext: CommonAttributes,
        left:        Attributes,
        right:       Attributes
    ): (Attributes, Attributes)

    def compareCode(
        leftContext: JVMMethod,
        left:        Option[Code],
        right:       Option[Code]
    ): (Option[Code], Option[Code])

}

class CompareAllConfiguration extends SimilarityTestConfiguration {

    override def compareFields(
        leftContext: ClassFile,
        left:        Iterable[JVMField],
        right:       Iterable[JVMField]
    ): (Iterable[JVMField], Iterable[JVMField]) = {
        (left, right)
    }

    override def compareMethods(
        leftContext: ClassFile,
        left:        Iterable[JVMMethod],
        right:       Iterable[JVMMethod]
    ): (Iterable[JVMMethod], Iterable[JVMMethod]) = {
        (left, right)
    }

    /**
     * Selects the attributes which should be compared. By default all attributes except
     * of unknown ones are selected.
     */
    override def compareAttributes(
        leftContext: CommonAttributes,
        left:        Attributes,
        right:       Attributes
    ): (Attributes, Attributes) = {
        val newLeft = left.filterNot(a => a.isInstanceOf[UnknownAttribute])
        val newRight = right.filterNot(a => a.isInstanceOf[UnknownAttribute])
        (newLeft, newRight)
    }

    override def compareCode(
        leftContext: JVMMethod,
        left:        Option[Code],
        right:       Option[Code]
    ): (Option[Code], Option[Code]) = {
        (left, right)
    }
}
object CompareAllConfiguration extends CompareAllConfiguration
