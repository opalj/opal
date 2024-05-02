/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package alias

import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.Context
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.IsNullValue
import org.opalj.value.ValueInformation

/**
 * Represents a source code element which can be part of an alias relation.
 *
 * Valid elements are:
 *
 * - [[AliasFormalParameter]]: A formal parameter.
 *
 * - [[AliasReturnValue]]: A method return value.
 *
 * - [[AliasField]]: A non-static field, represented by a [[FieldReference]].
 *
 * - [[AliasStaticField]]: A static field.
 *
 * - [[AliasUVar]]: A UVar, represented by a [[PersistentUVar]] and a [[Method]].
 */
sealed trait AliasSourceElement {

    /**
     * The underlying element that is represented.
     */
    def element: AnyRef

    /**
     * Returns the [[Method]] this element is associated with.
     * If such a method does not exist, an [[UnsupportedOperationException]] is thrown.
     *
     * @throws UnsupportedOperationException if the element is not associated with a method
     * @return The [[Method]] this element is associated with.
     *
     * @see [[isMethodBound]]
     */
    def method: Method = throw new UnsupportedOperationException()

    /**
     * Returns the [[DeclaredMethod]] this element is associated with.
     * If such a method does not exist, an [[UnsupportedOperationException]] is thrown.
     *
     * @throws UnsupportedOperationException if the element is not associated with a method
     * @return The [[DeclaredMethod]] this element is associated with.
     *
     * @see [[isMethodBound]]
     */
    def declaredMethod: DeclaredMethod = throw new UnsupportedOperationException()

    /**
     * Returns `true` if this element is associated with a method.
     * If this method returns `true`, [[method]] and [[declaredMethod]] can be safely called.
     *
     * @return `true` if this element is associated with a method.
     */
    def isMethodBound: Boolean

    /**
     * Returns `true` if the type of the represented element is a reference type.
     * Otherwise, if it is a primitive type, it returns false.
     *
     * @return `true` if the type of the represented element is a reference type.
     */
    def isReferenceType: Boolean

    /**
     * Returns `true` if the value of the element is the null value.
     *
     * @return `true` if the value of the element is the null value.
     */
    def isNullValue: Boolean = false

    /**
     * Returns the [[ReferenceType]] of the represented element.
     * If the type of the represented element is not reference type, a [[ClassCastException]] is thrown.
     *
     * @throws ClassCastException if the type of the represented element is not a reference type
     * @return the reference type of the represented element.
     *
     * @see [[isReferenceType]]
     */
    def referenceType: ReferenceType

    // conversion methods

    def isAliasField: Boolean = false

    def asAliasField: AliasField = throw new UnsupportedOperationException()

    def isAliasStaticField: Boolean = false

    def asAliasStaticField: AliasStaticField = throw new UnsupportedOperationException()

    def isAliasReturnValue: Boolean = false

    def asAliasReturnValue: AliasReturnValue = throw new UnsupportedOperationException()

    def isAliasFP: Boolean = false

    def asAliasFP: AliasFormalParameter = throw new UnsupportedOperationException()

    def isAliasUVar: Boolean = false

    def asAliasUVar: AliasUVar = throw new UnsupportedOperationException()

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
            case fp: VirtualFormalParameter => AliasFormalParameter(fp)
            case dm: Method                 => AliasReturnValue(dm, project)
            case f: FieldReference =>
                if (!f.field.isStatic) AliasField(f)
                else throw new IllegalArgumentException("Static fields must be represented by a normal Field")
            case f: Field =>
                if (f.isStatic) AliasStaticField(f)
                else throw new IllegalArgumentException("Non-static fields must be represented by a FieldReference")
            case (uVar: PersistentUVar, m: Method) => AliasUVar(uVar, m, project)
            case _                                 => throw new UnknownError("unhandled entity type")
        }
    }
}

/**
 * Encapsulates a reference to a specific field. it contains the possible definition sites of the object used to access
 * the field and the context in which the field is referenced.
 *
 * @param field The field that is referenced
 * @param defSites The possible definition sites of the field
 */
case class FieldReference(field: Field, context: Context, defSites: IntTrieSet)

/**
 * Represents a non-static field that is part of an alias relation.
 */
case class AliasField(fieldReference: FieldReference) extends AliasSourceElement {

    override def element: FieldReference = fieldReference

    override def isMethodBound: Boolean = true

    override def declaredMethod: DeclaredMethod = fieldReference.context.method

    override def method: Method = fieldReference.context.method.definedMethod

    override def isReferenceType: Boolean = fieldReference.field.fieldType.isReferenceType

    override def referenceType: ReferenceType = fieldReference.field.fieldType.asReferenceType

    override def isAliasField: Boolean = true

    override def asAliasField: AliasField = this
}

/**
 * Represents a static field that is part of an alias relation.
 */
case class AliasStaticField(field: Field) extends AliasSourceElement {

    override def element: Field = field

    override def isMethodBound: Boolean = false

    override def isReferenceType: Boolean = field.fieldType.isReferenceType

    override def referenceType: ReferenceType = field.fieldType.asReferenceType

    override def isAliasStaticField: Boolean = true

    override def asAliasStaticField: AliasStaticField = this

}

/**
 * Represents a method return value of a method that is part of an alias relation.
 */
case class AliasReturnValue(override val method: Method, project: SomeProject) extends AliasSourceElement {

    private[this] val dm = project.get(DeclaredMethodsKey)(method)

    override def element: AnyRef = method

    override def declaredMethod: DeclaredMethod = dm

    override def isMethodBound: Boolean = true

    override def isReferenceType: Boolean = method.returnType.isReferenceType

    override def referenceType: ReferenceType = method.returnType.asReferenceType

    override def isAliasReturnValue: Boolean = true

    override def asAliasReturnValue: AliasReturnValue = this
}

/**
 * Represents a formal parameter of a method that is part of an alias relation.
 */
case class AliasFormalParameter(fp: VirtualFormalParameter) extends AliasSourceElement {

    override def element: VirtualFormalParameter = fp

    override def method: Method = fp.method.definedMethod

    override def declaredMethod: DeclaredMethod = fp.method

    override def isMethodBound: Boolean = true

    override def isReferenceType: Boolean = {
        if (fp.parameterIndex == -1) fp.method.declaringClassType.isObjectType
        else fp.method.definedMethod.parameterTypes(fp.parameterIndex).isReferenceType
    }

    override def referenceType: ReferenceType = {
        if (fp.parameterIndex == -1) fp.method.declaringClassType.asReferenceType
        else fp.method.definedMethod.parameterTypes(fp.parameterIndex).asReferenceType
    }

    override def isAliasFP: Boolean = true

    override def asAliasFP: AliasFormalParameter = this
}

/**
 * A persistent representation (using pcs instead of TAC value origins) for a UVar.
 *
 * @see [[org.opalj.tac.fpcf.analyses.cg.persistentUVar]]
 */
case class PersistentUVar(valueInformation: ValueInformation, defSites: IntTrieSet)

/**
 * Represents a UVar that is part of an alias relation.
 */
case class AliasUVar(
    persistentUVar:      PersistentUVar,
    override val method: Method,
    project:             SomeProject
) extends AliasSourceElement {

    private[this] val dm = project.get(DeclaredMethodsKey)(method)

    override def element: (PersistentUVar, Method) = (persistentUVar, method)

    override def isMethodBound: Boolean = true

    override def declaredMethod: DeclaredMethod = dm

    override def isReferenceType: Boolean = persistentUVar.valueInformation.isReferenceValue

    override def isNullValue: Boolean = persistentUVar.valueInformation.isInstanceOf[IsNullValue]

    override def referenceType: ReferenceType = persistentUVar.valueInformation.asReferenceValue.asReferenceType

    override def isAliasUVar: Boolean = true

    override def asAliasUVar: AliasUVar = this

}
