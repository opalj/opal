/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Identifies the target of a type annotation.
 *
 * @example
 *      {{{
 *      Object o = ...;
 *      List<?> l = (Serializable & @MyTypeAnnotation Cloneable) o;
 *      }}}
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait TypeAnnotationTarget {

    def attribute_length: Int

    def tag: Int

    /** Description of the target kind. */
    def description: String

    def toXHTML(implicit cp: Constant_Pool): Node

}

//______________________________
// type_parameter_target

sealed abstract class TATTypeParameter extends TypeAnnotationTarget {

    def type_parameter_index: Int

    final override def attribute_length: Int = 1 + 1

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">
            <b>Target</b>
            <i>{ description }[0x{ tag.toHexString }]</i>
            (type parameter index:{ type_parameter_index }
            )
        </span>
    }
}

case class TATParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int
) extends TATTypeParameter {

    final override def tag: Int = 0x00

    final override def description: String = {
        "type parameter declaration of generic class or interface"
    }
}

case class TATParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int
) extends TATTypeParameter {

    final override def tag: Int = 0x01

    final override def description: String = {
        "type parameter declaration of generic method or constructor"
    }

}

//______________________________
// supertype_target
case class TATSupertype(supertype_index: Constant_Pool_Index) extends TypeAnnotationTarget {

    final override def attribute_length: Int = 1 + 2

    final override def tag: Int = 0x10

    final override def description: String = {
        "type in extends clause of class or interface declaration "+
            "(including the direct superclass of an anonymous class declaration), "+
            "or in implements clause of interface declaration"
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(supertype index: { supertype_index })</span>
    }
}

//______________________________
// type_parameter_bound_target

sealed abstract class TATTypeParameterBound extends TypeAnnotationTarget {

    def type_parameter_index: Int

    def bound_index: Int

    def description: String

    final override def attribute_length: Int = 1 + 1 + 1

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(type_parameter_index: { type_parameter_index }, bound index: { bound_index })</span>
    }
}

case class TATTypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index:          Int
) extends TATTypeParameterBound {

    final override def tag: Int = 0x11

    final override def description: String = {
        "type in bound of type parameter declaration of generic class or interface"
    }
}

case class TATTypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index:          Int
) extends TATTypeParameterBound {

    final override def tag: Int = 0x12

    final override def description: String = {
        "type in bound of type parameter declaration of generic method or constructor"
    }

}

//______________________________
// empty_target
sealed abstract class TATEmpty extends TypeAnnotationTarget {

    final override def attribute_length: Int = 1

    /** Description of the kind of target of the annotation. (See JVM.) */
    def description: String

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i></span>
    }
}

case object TATFieldDeclaration extends TATEmpty {

    final override def tag: Int = 0x13

    final override def description: String = "type in field declaration"

}
case object TATReturnType extends TATEmpty {

    final override def tag: Int = 0x14

    final override def description: String = {
        "return type of method, or type of newly constructed object"
    }

}
case object TATReceiverType extends TATEmpty {

    final override def tag: Int = 0x15

    final override def description: String = "receiver type of method or constructor"
}

//______________________________
// formal_parameter_target
case class TATFormalParameter(formal_parameter_index: Int) extends TypeAnnotationTarget {

    final override def attribute_length: Int = 1 + 1

    final override def tag: Int = 0x16

    def description: String = {
        "type in formal parameter declaration of method, constructor, or lambda expression"
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(formal parameter index: { formal_parameter_index })</span>
    }
}

//______________________________
// throws_target
case class TATThrows(throws_type_index: Int) extends TypeAnnotationTarget {

    final override def attribute_length: Int = 1 + 2

    final override def tag: Int = 0x17

    def description: String = "type in throws clause of method or throws_target constructor"

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(throws type index: { throws_type_index })</span>
    }
}

//_______________________________
// localvar_target

case class LocalvarTableEntry(start_pc: Int, length: Int, index: Int) {

    def toXHTML: Node = {
        <span>(start pc: { start_pc }, length: { length }, variable index: { index })</span>
    }

}

trait TATLocalvar extends TypeAnnotationTarget {

    /**
     * From the JVM (8) Specification:
     * '''"A table is needed to fully specify the local variable whose type
     * is annotated, because a single local variable may be represented with different local
     * variable indices over multiple live ranges. The start_pc, length, and index items in
     * each table entry specify the same information as a LocalVariableTable attribute."'''
     */
    def localvarTable: LocalvarTable

    final override def attribute_length: Int = 1 + 2 + localvarTable.size * 6

    def description: String

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(local variable occurences:{ localvarTable.map(_.toXHTML) })</span>
    }

}

case class TATLocalvarDecl(localvarTable: LocalvarTable) extends TATLocalvar {

    final override def tag: Int = 0x40

    final def description: String = "type in local variable declaration"

}

case class TATResourcevarDecl(localvarTable: LocalvarTable) extends TATLocalvar {

    final override def tag: Int = 0x41

    final def description: String = "type in resource variable declaration (try-with-resources)"

}

//______________________________
// catch_target
case class TATCatch(exception_table_index: Int) extends TypeAnnotationTarget {

    final override def attribute_length: Int = 1 + 2

    final override def tag: Int = 0x42

    def description: String = "type in exception parameter declaration"

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(exception table index: { exception_table_index })</span>
    }
}

//______________________________
// offset_target

trait TATWithOffset extends TypeAnnotationTarget {

    def offset: Int

    def description: String

    final override def attribute_length: Int = 1 + 2

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(bytecode offset: { offset })</span>
    }
}

case class TATInstanceOf(offset: Int) extends TATWithOffset {

    final override def tag: Int = 0x43

    final def description: String = "type in instanceof expression"

}
case class TATNew(offset: Int) extends TATWithOffset {

    final override def tag: Int = 0x44

    final def description: String = "type in new expression"
}
/** A `::New` expression. */
case class TATMethodReferenceExpressionNew(offset: Int) extends TATWithOffset {

    final override def tag: Int = 0x45

    final def description: String = "type in method reference expression using ::new"

}
/** A `::Identifier` expression. */
case class TATMethodReferenceExpressionIdentifier(offset: Int) extends TATWithOffset {

    final override def tag: Int = 0x46

    final def description: String = "type in method reference expression using ::Identifier"
}

//______________________________
// type_argument_target

trait TATTypeArgument extends TypeAnnotationTarget {

    def offset: Int

    def type_argument_index: Int

    /** The description of the annotated type argument as given in the JVM spec.*/
    def description: String

    final override def attribute_length: Int = 1 /*tag*/ + 2 + 1

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target"><i>{ description }[0x{ tag.toHexString }]</i>(bytecode offset: { offset }, type argument index: { type_argument_index })</span>
    }
}

case class TATCastExpression(offset: Int, type_argument_index: Int) extends TATTypeArgument {

    final override def tag: Int = 0x47

    final def description: String = "type in cast expression"

}
case class TATConstructorInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TATTypeArgument {

    final override def tag: Int = 0x48

    final def description: String = {
        "type argument for generic constructor in new expression or "+
            "explicit constructor invocation statement"
    }

}
case class TATMethodInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TATTypeArgument {

    final override def tag: Int = 0x49

    final def description: String = {
        "type argument for generic method in method invocation expression"
    }

}
case class TATConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TATTypeArgument {

    final override def tag: Int = 0x4a

    final def description: String = {
        "type argument for generic constructor in method reference expression using ::new"
    }
}
case class TATMethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TATTypeArgument {

    final override def tag: Int = 0x4b

    final def description: String = {
        "type argument for generic method in method reference expression using ::Identifier"
    }
}
