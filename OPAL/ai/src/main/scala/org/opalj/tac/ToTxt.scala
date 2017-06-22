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
 * Converts a list of three-address instructions into a text-based representation.
 *
 * @note This representation is primarily provided for debugging purposes and is not
 *       performance optimized.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object ToTxt {

    def callToTxt[V <: Var[V]](name: String, params: Seq[Expr[V]]): String = {
        params.reverse map { toTxtExpr[V] } mkString (s".$name(", ", ", ")")
    }

    @inline final def toTxtExpr[V <: Var[V]](expr: Expr[V]): String = {
        expr match {
            case v: Var[_] ⇒
                if (v.cTpe == ComputationalTypeReturnAddress)
                    v.name+"/* return address */"
                else
                    v.name
            case Param(_ /*cTpe*/ , name)      ⇒ name
            case IntConst(_, value)            ⇒ value.toString
            case LongConst(_, value)           ⇒ value.toString+"l"
            case FloatConst(_, value)          ⇒ value.toString+"f"
            case DoubleConst(_, value)         ⇒ value.toString+"d"
            case ClassConst(_, value)          ⇒ value.toJava+".class"
            case StringConst(_, value)         ⇒ s""""$value""""
            case NullExpr(_)                   ⇒ "null"

            case PrefixExpr(_, _, op, operand) ⇒ op.toString+" "+toTxtExpr[V](operand)

            case ArrayLoad(_, index, arrayRef) ⇒ s"${toTxtExpr(arrayRef)}[${toTxtExpr(index)}]"
            case ArrayLength(_, arrayRef)      ⇒ s"${toTxtExpr(arrayRef)}.length"

            case New(_, objTpe)                ⇒ s"new ${objTpe.simpleName}"

            case InstanceOf(_, value, tpe) ⇒
                s"${toTxtExpr(value)} instanceof ${tpe.asReferenceType.toJava}"

            case Checkcast(_, value, tpe) ⇒
                s"(${tpe.asReferenceType.toJava}) ${toTxtExpr(value)}"

            case Compare(_, left, op, right) ⇒
                toTxtExpr(left)+" "+op.toString+" "+toTxtExpr[V](right)

            case BinaryExpr(_, _ /*cTpe*/ , op, left, right) ⇒
                toTxtExpr[V](left)+" "+op.toString+" "+toTxtExpr[V](right)

            case PrimitiveTypecastExpr(_, baseTpe, operand) ⇒
                s"(${baseTpe.toJava}) ${toTxtExpr(operand)}"

            case NewArray(_, counts, arrayType) ⇒
                val initializedDimensions = counts.size
                val dimensions = arrayType.dimensions
                val initializer =
                    counts.map(c ⇒ s"[${toTxtExpr(c)}]").reverse.mkString("") +
                        ("[]" * (dimensions - initializedDimensions))
                s"new ${arrayType.drop(initializedDimensions).toJava}$initializer"

            case Invokedynamic(_, bootstrapMethod, name, descriptor, params) ⇒
                s"invokedynamic[${bootstrapMethod.toJava}]${callToTxt(name, params)}"

            case StaticFunctionCall(_, declClass, _, name, descriptor, params) ⇒
                declClass.toJava + callToTxt[V](name, params)

            case VirtualFunctionCall(_, declClass, _, name, descriptor, receiver, params) ⇒
                val callAsTxt = callToTxt(name, params)
                val receiverAsTxt = toTxtExpr[V](receiver)
                s"$receiverAsTxt/*${declClass.toJava}*/$callAsTxt"

            case NonVirtualFunctionCall(_, declClass, _, name, descriptor, receiver, params) ⇒
                val call = callToTxt(name, params)
                toTxtExpr[V](receiver)+"/*(non-virtual) "+declClass.toJava+"*/"+call

            case GetStatic(_, declaringClass, name, _) ⇒
                s"${declaringClass.toJava}.$name"

            case GetField(_, declaringClass, name, _, receiver) ⇒
                s"${toTxtExpr(receiver)}/*${declaringClass.toJava}*/.$name"
        }
    }

    @inline final def toTxtStmt[V <: Var[V]](stmt: Stmt[V], includePC: Boolean): String = {
        val pc = if (includePC) s"/*pc=${stmt.pc}:*/" else ""
        stmt.astID match {
            case Return.ASTID ⇒ s"$pc return"
            case ReturnValue.ASTID ⇒
                val ReturnValue(_, expr) = stmt
                s"$pc return ${toTxtExpr(expr)}"
            case Throw.ASTID ⇒
                val Throw(_, exc) = stmt
                s"$pc throw ${toTxtExpr(exc)}"

            case Nop.ASTID ⇒ s"$pc ;"

            case MonitorEnter.ASTID ⇒
                val MonitorEnter(_, objRef) = stmt
                s"$pc monitorenter ${toTxtExpr(objRef)}"
            case MonitorExit.ASTID ⇒
                val MonitorExit(_, objRef) = stmt
                s"$pc monitorexit ${toTxtExpr(objRef)}"

            case Goto.ASTID ⇒
                val Goto(_, target) = stmt
                s"$pc goto $target"

            case JumpToSubroutine.ASTID ⇒
                val JumpToSubroutine(_, target) = stmt
                s"$pc jsr $target"
            case Ret.ASTID ⇒
                val Ret(_, targets) = stmt
                targets.mkString(s"$pc ret {", ",", "}")

            case If.ASTID ⇒
                val If(_, left, cond, right, target) = stmt
                s"$pc if(${toTxtExpr(left)} $cond ${toTxtExpr(right)}) goto $target"

            case Switch.ASTID ⇒
                val Switch(_, defaultTarget, index, npairs) = stmt
                var result = "\n"
                for (x ← npairs) { result = result+"    "+x._1+": goto "+x._2+";\n" }
                result = result+"    default: goto "+defaultTarget+"\n"
                s"$pc switch(${toTxtExpr(index)}){$result}"

            case Assignment.ASTID ⇒
                val Assignment(_, variable, expr) = stmt
                s"$pc ${variable.name} = ${toTxtExpr(expr)}"

            case ExprStmt.ASTID ⇒
                val ExprStmt(_, expr) = stmt
                s"$pc expression value is ignored:*/${toTxtExpr(expr)}"

            case ArrayStore.ASTID ⇒
                val ArrayStore(_, arrayRef, index, operandVar) = stmt
                s"$pc ${toTxtExpr(arrayRef)}[${toTxtExpr(index)}] = ${toTxtExpr(operandVar)}"

            case PutStatic.ASTID ⇒
                val PutStatic(_, declaringClass, name, _, value) = stmt
                s"$pc ${declaringClass.toJava}.$name = ${toTxtExpr(value)}"

            case PutField.ASTID ⇒
                val PutField(_, declaringClass, name, _, receiver, value) = stmt
                val field = s"${toTxtExpr(receiver)}/*${declaringClass.toJava}*/.$name"
                s"$pc $field = ${toTxtExpr(value)}"

            case StaticMethodCall.ASTID ⇒
                val StaticMethodCall(_, declClass, _, name, _ /* descriptor*/ , params) = stmt
                s"$pc ${declClass.toJava}${callToTxt(name, params)}"

            case VirtualMethodCall.ASTID ⇒
                val VirtualMethodCall(_, declClass, _, name, _ /*desc.*/ , receiver, params) = stmt
                val call = callToTxt(name, params)
                s"$pc ${toTxtExpr(receiver)}/*${declClass.toJava}*/$call"

            case NonVirtualMethodCall.ASTID ⇒
                val NonVirtualMethodCall(_, declClass, _, name, _ /*desc.*/ , rec, params) = stmt
                val call = callToTxt(name, params)
                s"$pc ${toTxtExpr(rec)}/*(non-virtual) ${declClass.toJava}*/$call"

            case FailingExpression.ASTID ⇒
                val FailingExpression(_, fExpr) = stmt
                s"$pc expression evaluation will throw exception: ${toTxtExpr(fExpr)}"

            case FailingStatement.ASTID ⇒
                val FailingStatement(_, fStmt) = stmt
                s"$pc statement always throws an exception: ${toTxtStmt(fStmt, includePC)}"

        }
    }

    /**
     * Converts the statements to some human readable text.
     */
    def apply[V <: Var[V]](stmts: Array[Stmt[V]]): String = {
        apply(stmts, true, true).mkString("\n")
    }

    /**
     * Converts the statements to some human readable text.
     *
     * @param includePC If `true` the original program counter is also shown in the output.
     */
    def apply[V <: Var[V]](
        stmts:     Array[Stmt[V]],
        indented:  Boolean,
        includePC: Boolean
    ): Array[String] = {

        val max = stmts.length
        val javaLikeCode = new Array[String](max)
        var index = 0
        while (index < max) {

            def qualify(javaLikeStmt: String): String = {
                if (indented)
                    f"$index%5d:${javaLikeStmt.replace("\n", "\n       ")}"
                else
                    s"$index:$javaLikeStmt"
            }

            javaLikeCode(index) = qualify(toTxtStmt(stmts(index), includePC))

            index += 1
        }

        javaLikeCode
    }

    /**
     * @see `apply(Array,Boolean,Boolean)`
     */
    def apply[V <: Var[V]](
        stmts:     IndexedSeq[Stmt[V]],
        indented:  Boolean,
        includePC: Boolean
    ): Array[String] = {
        apply(stmts.toArray, indented, includePC)
    }

    /**
     *  Creates a text based representation of the three address code generated for the given
     *  method.
     */
    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy                                = Code.BasicClassHierarchy,
        aiResult:       Option[AIResult { val domain: RecordDefUse }] = None
    ): String = {
        aiResult map { aiResult ⇒
            ToTxt(TACAI(method, classHierarchy, aiResult)(Nil).stmts)
        } getOrElse {
            val (stmts, _, _) = TACNaive(method, classHierarchy, List(SimplePropagation), false)
            ToTxt(stmts)
        }
    }

}
