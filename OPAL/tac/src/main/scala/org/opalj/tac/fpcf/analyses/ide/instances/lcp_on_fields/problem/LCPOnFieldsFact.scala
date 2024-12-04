/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import org.opalj.br.ObjectType
import org.opalj.ide.problem.IDEFact

/**
 * Type for modeling facts for linear constant propagation on fields
 */
trait LCPOnFieldsFact extends IDEFact

/**
 * Fact to use as null fact
 */
case object NullFact extends LCPOnFieldsFact

/**
 * Common type for different types of entities
 */
trait AbstractEntityFact extends LCPOnFieldsFact {
    /**
     * The name of the variable (e.g. `lv0`)
     */
    val name: String
    /**
     * Where the variable is defined (used to uniquely identify a variable/variable fact)
     */
    val definedAtIndex: Int

    def toObjectOrArrayFact: AbstractEntityFact
}

/**
 * Type for object facts
 */
trait AbstractObjectFact extends AbstractEntityFact {
    def toObjectFact: ObjectFact = ObjectFact(name, definedAtIndex)

    override def toObjectOrArrayFact: AbstractEntityFact = toObjectFact
}

/**
 * Fact representing a seen object variable
 */
case class ObjectFact(name: String, definedAtIndex: Int) extends AbstractObjectFact {
    override def toObjectFact: ObjectFact = this

    override def toString: String = s"ObjectFact($name)"
}

/**
 * Fact representing a seen object variable and modeling that it gets initialized
 */
case class NewObjectFact(name: String, definedAtIndex: Int) extends AbstractObjectFact {
    override def toString: String = s"NewObjectFact($name)"
}

/**
 * Fact representing a seen object variable and modeling that one of its fields gets written
 * @param fieldName the name of the field that gets written
 */
case class PutFieldFact(name: String, definedAtIndex: Int, fieldName: String) extends AbstractObjectFact {
    override def toString: String = s"PutFieldFact($name, $fieldName)"
}

/**
 * Type for array facts
 */
trait AbstractArrayFact extends AbstractEntityFact {
    def toArrayFact: ArrayFact = ArrayFact(name, definedAtIndex)

    override def toObjectOrArrayFact: AbstractEntityFact = toArrayFact
}

/**
 * Fact representing a seen array variable
 */
case class ArrayFact(name: String, definedAtIndex: Int) extends AbstractArrayFact {
    override def toArrayFact: ArrayFact = this

    override def toString: String = s"ArrayFact($name)"
}

/**
 * Fact representing a seen array variable and modeling that it gets initialized
 */
case class NewArrayFact(name: String, definedAtIndex: Int) extends AbstractArrayFact {
    override def toString: String = s"NewArrayFact($name)"
}

/**
 * Fact representing a seen array variable and modeling that one of its elements gets written
 */
case class PutElementFact(name: String, definedAtIndex: Int) extends AbstractArrayFact {
    override def toString: String = s"PutElementFact($name)"
}

/**
 * Type for facts for static fields
 */
trait AbstractStaticFieldFact extends LCPOnFieldsFact {
    /**
     * The object type the field belongs to
     */
    val objectType: ObjectType

    /**
     * The name of the field
     */
    val fieldName: String

    def toStaticFieldFact: AbstractStaticFieldFact = StaticFieldFact(objectType, fieldName)
}

/**
 * Fact representing a seen static field
 */
case class StaticFieldFact(objectType: ObjectType, fieldName: String) extends AbstractStaticFieldFact {
    override def toStaticFieldFact: StaticFieldFact = this

    override def toString: String = s"StaticFieldFact(${objectType.simpleName}, $fieldName)"
}

/**
 * Fact representing a seen static field and modeling that it gets written
 */
case class PutStaticFieldFact(objectType: ObjectType, fieldName: String) extends AbstractStaticFieldFact {
    override def toString: String = s"PutStaticFieldFact(${objectType.simpleName}, $fieldName)"
}
