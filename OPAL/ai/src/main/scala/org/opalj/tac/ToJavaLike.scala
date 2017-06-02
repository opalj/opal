/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package tac

import org.opalj.br.Method
import org.opalj.ai.AIResult
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ClassHierarchy
import org.opalj.br.Code
import org.opalj.br.ComputationalTypeReturnAddress

/**
 * Converts a list of three-address instructions into a Java like representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object ToJavaLike {

    private def callToJavaLike[V <: Var[V]](name: String, params: Seq[Expr[V]]): String = {
        params.reverse map { toJavaLikeExpr[V] } mkString (s".$name(", ", ", ")")
    }

    @inline final def toJavaLikeExpr[V <: Var[V]](expr: Expr[V]): String = {
        expr match {
            case v: Var[_] ⇒
                if (v.cTpe == ComputationalTypeReturnAddress)
                    v.name+"/* return address */"
                else
                    v.name
            case Param(_ /*cTpe*/ , name) ⇒ name
            case IntConst(_, value)       ⇒ value.toString
            case LongConst(_, value)      ⇒ value.toString+"l"
            case FloatConst(_, value)     ⇒ value.toString+"f"
            case DoubleConst(_, value)    ⇒ value.toString+"d"
            case ClassConst(_, value)     ⇒ value.toJava+".class"
            case StringConst(_, value)    ⇒ s""""$value""""
            case NullExpr(_)              ⇒ "null"

            case InstanceOf(_, value, tpe) ⇒
                s"${toJavaLikeExpr(value)} instanceof ${tpe.asReferenceType.toJava}"

            case Checkcast(_, value, tpe) ⇒
                s"(${tpe.asReferenceType.toJava}) ${toJavaLikeExpr(value)}"

            case Compare(_, left, op, right) ⇒
                toJavaLikeExpr(left)+" "+op.toString()+" "+toJavaLikeExpr[V](right)

            case BinaryExpr(_, _ /*cTpe*/ , op, left, right) ⇒
                toJavaLikeExpr[V](left)+" "+op.toString()+" "+toJavaLikeExpr[V](right)

            case PrefixExpr(_, _, op, operand) ⇒
                op.toString()+" "+toJavaLikeExpr[V](operand)

            case PrimitiveTypecastExpr(_, baseTpe, operand) ⇒
                s"(${baseTpe.toJava}) ${toJavaLikeExpr(operand)}"

            case New(_, objTpe) ⇒
                s"new ${objTpe.simpleName}"

            case NewArray(_, counts, arrayType) ⇒
                val initializedDimensions = counts.size
                val dimensions = arrayType.dimensions
                val initializer =
                    counts.map(c ⇒ s"[${toJavaLikeExpr(c)}]").reverse.mkString("") +
                        ("[]" * (dimensions - initializedDimensions))
                s"new ${arrayType.drop(initializedDimensions).toJava}$initializer"

            case ArrayLoad(_, index, arrayRef) ⇒
                s"${toJavaLikeExpr(arrayRef)}[${toJavaLikeExpr(index)}]"

            case ArrayLength(_, arrayRef) ⇒
                s"${toJavaLikeExpr(arrayRef)}.length"

            case Invokedynamic(_, bootstrapMethod, name, descriptor, params) ⇒
                s"invokedynamic[${bootstrapMethod.toJava}]${callToJavaLike(name, params)}"

            case StaticFunctionCall(_, declClass, _, name, descriptor, params) ⇒
                declClass.toJava + callToJavaLike[V](name, params)

            case VirtualFunctionCall(_, declClass, _, name, descriptor, receiver, params) ⇒
                val call = callToJavaLike(name, params)
                toJavaLikeExpr[V](receiver)+"/*"+declClass.toJava+"*/"+call

            case NonVirtualFunctionCall(_, declClass, _, name, descriptor, receiver, params) ⇒
                val call = callToJavaLike(name, params)
                toJavaLikeExpr[V](receiver)+"/* (Non-Virtual) "+declClass.toJava+"*/"+call

            case GetStatic(_, declaringClass, name) ⇒
                s"${declaringClass.toJava}.$name"

            case GetField(_, declaringClass, name, receiver) ⇒
                s"${toJavaLikeExpr(receiver)}/*${declaringClass.toJava}*/.$name"
        }
    }

    @inline final def toJavaLikeStmt[V <: Var[V]](stmt: Stmt[V]): String = {
        stmt.astID match {
            case Return.ASTID ⇒ "return;"
            case ReturnValue.ASTID ⇒
                val ReturnValue(_, expr) = stmt
                s"return ${toJavaLikeExpr(expr)};"
            case Throw.ASTID ⇒
                val Throw(_, exc) = stmt
                s"throw ${toJavaLikeExpr(exc)};"

            case Nop.ASTID ⇒ ";"

            case MonitorEnter.ASTID ⇒
                val MonitorEnter(_, objRef) = stmt
                s"monitorenter ${toJavaLikeExpr(objRef)};"
            case MonitorExit.ASTID ⇒
                val MonitorExit(_, objRef) = stmt
                s"monitorexit ${toJavaLikeExpr(objRef)};"

            case Goto.ASTID ⇒
                val Goto(_, target) = stmt
                s"goto $target;"

            case JumpToSubroutine.ASTID ⇒
                val JumpToSubroutine(_, target) = stmt
                s"jsr $target;"
            case Ret.ASTID ⇒
                val Ret(_, targets) = stmt
                targets.mkString("ret {", ",", "};")

            case If.ASTID ⇒
                val If(_, left, cond, right, target) = stmt
                s"if(${toJavaLikeExpr(left)} $cond ${toJavaLikeExpr(right)}) goto $target;"

            case Switch.ASTID ⇒
                val Switch(_, defaultTarget, index, npairs) = stmt
                var result = "\n"
                for (x ← npairs) { result = result+"    "+x._1+": goto "+x._2+";\n" }
                result = result+"    default: goto "+defaultTarget+";\n"
                s"switch(${toJavaLikeExpr(index)}){$result}"

            case Assignment.ASTID ⇒
                val Assignment(_, variable, expr) = stmt
                s"${variable.name} = ${toJavaLikeExpr(expr)};"

            case ExprStmt.ASTID ⇒
                val ExprStmt(_, expr) = stmt
                s"/* expression value is ignored */ ${toJavaLikeExpr(expr)};"

            case ArrayStore.ASTID ⇒
                val ArrayStore(_, arrayRef, index, operandVar) = stmt
                s"${toJavaLikeExpr(arrayRef)}[${toJavaLikeExpr(index)}] = ${toJavaLikeExpr(operandVar)};"

            case PutStatic.ASTID ⇒
                val PutStatic(_, declaringClass, name, value) = stmt
                s"${declaringClass.toJava}.$name = ${toJavaLikeExpr(value)}"

            case PutField.ASTID ⇒
                val PutField(_, declaringClass, name, receiver, value) = stmt
                s"${toJavaLikeExpr(receiver)}/*${declaringClass.toJava}*/.$name = ${toJavaLikeExpr(value)}"

            case StaticMethodCall.ASTID ⇒
                val StaticMethodCall(_, declClass, _, name, _ /* descriptor*/ , params) = stmt
                declClass.toJava + callToJavaLike(name, params) + ';'

            case VirtualMethodCall.ASTID ⇒
                val VirtualMethodCall(_, declClass, _, name, _ /*descriptor*/ , receiver, params) = stmt
                val call = callToJavaLike(name, params)
                toJavaLikeExpr(receiver)+"/*"+declClass.toJava+"*/"+call + ';'

            case NonVirtualMethodCall.ASTID ⇒
                val NonVirtualMethodCall(_, declClass, _, name, _ /* descriptor*/ , receiver, params) = stmt
                val call = callToJavaLike(name, params)
                toJavaLikeExpr(receiver)+"/*(Non-Virtual)"+declClass.toJava+"*/"+call + ';'

            case FailingExpression.ASTID ⇒
                val FailingExpression(_, fExpr) = stmt
                s"/*always throws an exception:*/${toJavaLikeExpr(fExpr)};"

            case FailingStatement.ASTID ⇒
                val FailingStatement(_, fStmt) = stmt
                s"/*always throws an exception:*/${toJavaLikeStmt(fStmt)};"

        }
    }

    /**
     * Converts the quadruples representation into Java-like code.
     */
    def apply[V <: Var[V]](stmts: Array[Stmt[V]]): String = {
        apply(stmts, true).mkString("\n")
    }

    /**
     * Converts each statement into a Java-like statement.
     */
    def apply[V <: Var[V]](stmts: Array[Stmt[V]], indented: Boolean): Array[String] = {

        val max = stmts.size
        val javaLikeCode = new Array[String](max)
        var index = 0;
        while (index < max) {
            def qualify(javaLikeStmt: String): String = {
                if (indented)
                    f"$index%5d: ${javaLikeStmt.replace("\n", "\n       ")}"
                else
                    s"$index: $javaLikeStmt"
            }

            javaLikeCode(index) = qualify(toJavaLikeStmt(stmts(index)))

            index += 1
        }

        javaLikeCode
    }

    /**
     * Converts each statement into a Java-like statement.
     */
    def apply[V <: Var[V]](stmts: IndexedSeq[Stmt[V]], indented: Boolean): Array[String] = {
        apply(stmts.toArray, indented)
    }

    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy                                = Code.BasicClassHierarchy,
        aiResult:       Option[AIResult { val domain: RecordDefUse }] = None
    ): String = {
        aiResult map { aiResult ⇒
            ToJavaLike(TACAI(method, classHierarchy, aiResult)(List.empty)._1)
        } getOrElse {
            ToJavaLike(TACNaive(method, classHierarchy, List(SimplePropagation), false)._1)
        }
    }

}
