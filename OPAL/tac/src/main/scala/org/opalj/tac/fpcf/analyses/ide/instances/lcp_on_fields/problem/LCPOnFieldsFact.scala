/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

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
 * Type for
 */
trait AbstractObjectFact extends LCPOnFieldsFact {
    /**
     * The name of the variable (e.g. `lv0`)
     */
    val name: String
    /**
     * Where the variable is defined (used to uniquely identify a variable/variable fact)
     */
    val definedAtIndex: Int

    def toObjectFact: ObjectFact = ObjectFact(name, definedAtIndex)
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
