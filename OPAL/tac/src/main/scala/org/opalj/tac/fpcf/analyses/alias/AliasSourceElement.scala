/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.tac.common.DefinitionSite

/**
 * Represents a source code element which can be part of an alias relation.
 *
 * Valid elements are:
 * - [[AliasFP]]: A formal parameter
 * - [[AliasDS]]: A definition site
 * - [[AliasReturnValue]]: A method return value
 * - [[AliasNull]]: The null value
 */
sealed trait AliasSourceElement {

    /**
     * The underlying element that is represented.
     */
    def element: AnyRef

    /**
     * Returns the [[Method]] this element is associated with.
     * If such a method does not exist, an exception is thrown.
     *
     * @throws UnsupportedOperationException if the element is not associated with a method
     * @return The [[Method]] this element is associated with.
     *
     * @see [[isMethodBound]]
     */
    def method: Method = throw new UnsupportedOperationException()

    /**
     * Returns the [[DeclaredMethod]] this element is associated with.
     * If such a method does not exist, an exception is thrown.
     *
     * @throws UnsupportedOperationException if the element is not associated with a method
     * @return The [[DeclaredMethod]] this element is associated with.
     *
     * @see [[isMethodBound]]
     */
    def declaredMethod: DeclaredMethod = throw new UnsupportedOperationException()

    /**
     * Returns the definition site of this element.
     * If such a definition site does not exist, an exception is thrown.
     *
     * @throws UnsupportedOperationException if the element is not associated with a definition site
     * @return The definition site of this element.
     */
    def definitionSite: Int = throw new UnsupportedOperationException()

    /**
     * Returns `true` if this element is associated with a method.
     * If this method returns `true`, [[method]] and [[declaredMethod]] can be safely called.
     *
     * @return `true` if this element is associated with a method.
     */
    def isMethodBound: Boolean
}

object AliasSourceElement {

    /**
     * Creates an [[AliasSourceElement]] that represents the given element.
     *
     * @param element The element to represent
     * @param project The project the element is part of
     * @return An [[AliasSourceElement]] that represents the given element
     */
    def apply(element: AnyRef)(implicit project: SomeProject): AliasSourceElement = {
        element match {
            case fp: VirtualFormalParameter => AliasFP(fp)
            case ds: DefinitionSite         => AliasDS(ds, project)
            case dm: Method                 => AliasReturnValue(dm, project)
            case _                          => throw new UnknownError("unhandled entity type")
        }
    }
}

/**
 * Represents a field that is part of an alias relation.
 */
case class AliasField(field: Field) extends AliasSourceElement {

    override def element: Field = field

    override def isMethodBound: Boolean = false
}

/**
 * Represents the null value that is part of an alias relation.
 */
case class AliasNull() extends AliasSourceElement {
    override def element: AnyRef = throw new UnsupportedOperationException()

    override def isMethodBound: Boolean = false
}

/**
 * Represents a method return value of a method that is part of an alias relation.
 */
case class AliasReturnValue(override val method: Method, project: SomeProject) extends AliasSourceElement {
    override def element: AnyRef = method

    override def declaredMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(method)

    override def isMethodBound: Boolean = true
}

/**
 * Represents a formal parameter of a method that is part of an alias relation.
 */
case class AliasFP(fp: VirtualFormalParameter) extends AliasSourceElement {

    override def element: VirtualFormalParameter = fp

    override def method: Method = fp.method.definedMethod

    override def definitionSite: Int = fp.origin

    override def declaredMethod: DeclaredMethod = fp.method

    override def isMethodBound: Boolean = true
}

/**
 * Represents a definition site that is part of an alias relation.
 */
case class AliasDS(ds: DefinitionSite, project: SomeProject) extends AliasSourceElement {

    override def element: DefinitionSite = ds

    override def method: Method = ds.method

    override def definitionSite: Int = ds.pc

    override def declaredMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(method)

    override def isMethodBound: Boolean = true
}
