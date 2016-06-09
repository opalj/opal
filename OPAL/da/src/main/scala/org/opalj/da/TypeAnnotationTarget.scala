/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische 
*    Universität Darmstadt nor the names of its contributors may be used to 
*    endorse or promote products derived from this software without specific 
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node

    def tag: Int
}

//______________________________
// type_parameter_target

trait Type_Parameter_Target extends TypeAnnotationTarget {
    def type_parameter_index: Constant_Pool_Index
}

case class ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Constant_Pool_Index
) extends Type_Parameter_Target {

    final override def tag: Int = 0x00

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp) }</span>
    }
}

case class ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Constant_Pool_Index
) extends Type_Parameter_Target {

    final override def tag: Int = 0x01

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp) }</span>
    }
}

//______________________________
// supertype_target
case class Supertype_Target(supertype_index: Constant_Pool_Index) extends TypeAnnotationTarget {

    final override def tag: Int = 0x10

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(supertype_index).toString(cp) }</span>
    }
}

//______________________________
// type_parameter_bound_target

trait Type_Parameter_Bound_Target extends TypeAnnotationTarget {
    def type_parameter_index: Constant_Pool_Index
    def bound_index: Constant_Pool_Index
}
case class TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Constant_Pool_Index,
        bound_index:          Constant_Pool_Index
) extends Type_Parameter_Bound_Target {

    final override def tag: Int = 0x11

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp)+"-"+cp(bound_index).toString(cp) }</span>
    }
}
case class TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Constant_Pool_Index,
        bound_index:          Constant_Pool_Index
) extends Type_Parameter_Bound_Target {

    final override def tag: Int = 0x12

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp)+"-"+cp(bound_index).toString(cp) }</span>
    }
}

//______________________________
// empty_target
case object FieldDeclaration extends TypeAnnotationTarget {

    final override def tag: Int = 0x13

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Field Decleration</span>
    }
}
case object ReturnType extends TypeAnnotationTarget {

    final override def tag: Int = 0x14

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Return Type</span>
    }
}
case object ReceiverType extends TypeAnnotationTarget {

    final override def tag: Int = 0x15

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Receiver Type</span>
    }
}

//______________________________
// formal_parameter_target
case class Formal_Parameter_Target(formal_parameter_index: Constant_Pool_Index) extends TypeAnnotationTarget {

    final override def tag: Int = 0x16

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(formal_parameter_index).toString(cp) }</span>
    }
}

//______________________________
// throws_target
case class Throws_Target(throws_type_index: Constant_Pool_Index) extends TypeAnnotationTarget {

    final override def tag: Int = 0x17

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(throws_type_index).toString(cp) }</span>
    }
}

//_______________________________
// localvar_target

trait Localvar_Target extends TypeAnnotationTarget {
    def localvarTable: IndexedSeq[LocalvarTableEntry]
}

case class LocalvarTableEntry(
        start_pc: Int,
        length:   Int,
        index:    Int
) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[pc: { start_pc }, local variable table:{ cp(index).toString(cp) }]</span>
    }
}

case class LocalvarDecl(localvarTable: IndexedSeq[LocalvarTableEntry]) extends Localvar_Target {

    type LocalvarTable = IndexedSeq[LocalvarTableEntry]

    final override def tag: Int = 0x40

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[LocalvarDecl:{ localvarTable.map(_.toXHTML(cp)) }]</span>
    }
}
case class ResourcevarDecl(localvarTable: IndexedSeq[LocalvarTableEntry]) extends Localvar_Target {

    type LocalvarTable = IndexedSeq[LocalvarTableEntry]

    final override def tag: Int = 0x41

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[ResourcevarDecl:{ localvarTable.map(_.toXHTML(cp)) }]</span>
    }
}

//______________________________
// catch_target
case class Catch_Target(exception_table_index: Constant_Pool_Index) extends TypeAnnotationTarget {

    final override def tag: Int = 0x42

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(exception_table_index).toString(cp) }</span>
    }
}

//______________________________
// offset_target

trait Offset_Target extends TypeAnnotationTarget {
    def offset: Int
}

case class InstanceOf(offset: Int) extends Offset_Target {

    final override def tag: Int = 0x43

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class New(offset: Int) extends Offset_Target {

    final override def tag: Int = 0x44

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class MethodReferenceExpressionNew /*::New*/ (offset: Int) extends Offset_Target {

    final override def tag: Int = 0x45

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class MethodReferenceExpressionIdentifier /*::Identifier*/ (offset: Int) extends Offset_Target {

    final override def tag: Int = 0x46

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}

//______________________________
// type_argument_target

trait Type_Argument_Target extends TypeAnnotationTarget {
    def offset: Int
    def type_argument_index: Constant_Pool_Index
}

case class CastExpression(
        offset:              Int,
        type_argument_index: Constant_Pool_Index
) extends Type_Argument_Target {

    final override def tag: Int = 0x47

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class ConstructorInvocation(
        offset:              Int,
        type_argument_index: Constant_Pool_Index
) extends Type_Argument_Target {

    final override def tag: Int = 0x48

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class MethodInvocation(
        offset:              Int,
        type_argument_index: Constant_Pool_Index
) extends Type_Argument_Target {

    final override def tag: Int = 0x49

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class ConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Constant_Pool_Index
) extends Type_Argument_Target {

    final override def tag: Int = 0x4a

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class MethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Constant_Pool_Index
) extends Type_Argument_Target {

    final override def tag: Int = 0x4b

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
