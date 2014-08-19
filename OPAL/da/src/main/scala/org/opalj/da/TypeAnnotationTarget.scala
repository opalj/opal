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
}

//______________________________
// type_parameter_target
case class ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp) }</span>
    }
}

case class ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp) }</span>
    }
}

//______________________________
// supertype_target
case class SupertypeTarget(
        supertype_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(supertype_index).toString(cp) }</span>
    }
}

//______________________________
// type_parameter_bound_target
case class TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp)+"-"+cp(bound_index).toString(cp) }</span>
    }
}
case class TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(type_parameter_index).toString(cp)+"-"+cp(bound_index).toString(cp) }</span>
    }
}

//______________________________
// empty_target
case class FieldDeclaration() extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Field Decleration</span>
    }
}
case class ReturnType() extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Return Type</span>
    }
}
case class ReceiverType() extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">Receiver Type</span>
    }
}

//______________________________
// formal_parameter_target
case class FormalParameter(formal_parameter_index: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(formal_parameter_index).toString(cp) }</span>
    }
}

//______________________________
// throws_target
case class Throws(throws_type_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(throws_type_index).toString(cp) }</span>
    }
}

//______________________________
// catch_target
case class Catch(exception_table_index: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="type_annotation_target">{ cp(exception_table_index).toString(cp) }</span>
    }
}

case class LocalvarTableEntry(
        start_pc: Int,
        length: Int,
        local_variable_table_index: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[pc: { start_pc }, local vraible table:{ cp(local_variable_table_index).toString(cp) }]</span>
    }
}

case class LocalvarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]) extends TypeAnnotationTarget {
    type LocalvarTable = IndexedSeq[LocalvarTableEntry]
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[LocalvarDecl:{ for (localVar ← localVarTable) yield localVar.toXHTML(cp) }]</span>
    }
}
case class ResourcevarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]) extends TypeAnnotationTarget {
    type LocalvarTable = IndexedSeq[LocalvarTableEntry]

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[ResourcevarDecl:{ for (localVar ← localVarTable) yield localVar.toXHTML(cp) }]</span>
    }
}

//______________________________
// offset_target
case class InstanceOf(offset: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class New(offset: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class MethodReferenceExpressionNew /*::New*/ (
        offset: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}
case class MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int) extends TypeAnnotationTarget {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }]</span>
    }
}

//______________________________
// type_arguement_target
case class CastExpression(
        offset: Int,
        type_argument_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class ConstructorInvocation(
        offset: Int,
        type_argument_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class MethodInvocation(
        offset: Int,
        type_argument_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class ConstructorInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
case class MethodInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int) extends TypeAnnotationTarget {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>[offset:{ offset }, { cp(type_argument_index).toString(cp) }]</span>
    }
}
