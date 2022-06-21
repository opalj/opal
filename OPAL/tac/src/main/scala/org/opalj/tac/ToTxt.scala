/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.Method
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.ExitNode
import org.opalj.br.ClassHierarchy
import org.opalj.br.Type
import org.opalj.br.ComputationalTypeReturnAddress

/**
 * Converts a list of three-address instructions into a text-based representation for comprehension
 * purposes only.
 *
 * @note This representation is primarily provided for debugging purposes and is not
 *       performance optimized.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
object ToTxt {

    def callToTxt[V <: Var[V]](name: String, params: Seq[Expr[V]]): String = {
        params map { toTxtExpr[V] } mkString (s".$name(", ", ", ")")
    }

    @inline final def toTxtExpr[V <: Var[V]](expr: Expr[V]): String = {
        expr match {
            case v: Var[_] =>
                if (v.cTpe == ComputationalTypeReturnAddress)
                    v.name+"/* return address */"
                else
                    v.name
            case Param(_ /*cTpe*/ , name)    => name
            case IntConst(_, value)          => value.toString
            case LongConst(_, value)         => value.toString+"l"
            case FloatConst(_, value)        => value.toString+"f"
            case DoubleConst(_, value)       => value.toString+"d"
            case ClassConst(_, value)        => value.toJava+".class"
            case StringConst(_, value)       => s""""${value.replace("\\", "\\\\")}""""
            case MethodHandleConst(_, value) => s"""MethodHandle("${value.toJava}")"""
            case MethodTypeConst(_, value)   => s"""MethodType("${value.toJava}")"""
            case DynamicConst(_, bootstrapMethod, name, _) =>
                s"DynamicConstant[${bootstrapMethod.toJava}]($name)"
            case NullExpr(_)                   => "null"

            case PrefixExpr(_, _, op, operand) => op.toString+" "+toTxtExpr[V](operand)

            case ArrayLoad(_, index, arrayRef) => s"${toTxtExpr(arrayRef)}[${toTxtExpr(index)}]"
            case ArrayLength(_, arrayRef)      => s"${toTxtExpr(arrayRef)}.length"

            case New(_, objTpe)                => s"new ${objTpe.toJava}"

            case InstanceOf(_, value, tpe) =>
                s"${toTxtExpr(value)} instanceof ${tpe.asReferenceType.toJava}"

            case Compare(_, left, op, right) =>
                toTxtExpr(left)+" "+op.toString+" "+toTxtExpr[V](right)

            case BinaryExpr(_, _ /*cTpe*/ , op, left, right) =>
                toTxtExpr[V](left)+" "+op.toString+" "+toTxtExpr[V](right)

            case PrimitiveTypecastExpr(_, baseTpe, operand) =>
                s"(${baseTpe.toJava}) ${toTxtExpr(operand)}"

            case NewArray(_, counts, arrayType) =>
                val initializedDimensions = counts.size
                val dimensions = arrayType.dimensions
                val initializer =
                    counts.map(c => s"[${toTxtExpr(c)}]").reverse.mkString("") +
                        ("[]" * (dimensions - initializedDimensions))
                s"new ${arrayType.drop(initializedDimensions).toJava}$initializer"

            case InvokedynamicFunctionCall(_, bootstrapMethod, name, _ /*descriptor*/ , params) =>
                s"invokedynamic[${bootstrapMethod.toJava}]${callToTxt(name, params)}"

            case StaticFunctionCall(_, declClass, _, name, _ /*descriptor*/ , params) =>
                declClass.toJava + callToTxt[V](name, params)

            case VirtualFunctionCall(_, declClass, _, name, _ /*descriptor*/ , receiver, params) =>
                val callAsTxt = callToTxt(name, params)
                val receiverAsTxt = toTxtExpr[V](receiver)
                s"$receiverAsTxt/*${declClass.toJava}*/$callAsTxt"

            case NonVirtualFunctionCall(_, declClass, _, name, _ /*descriptor*/ , receiver, params) =>
                val call = callToTxt(name, params)
                toTxtExpr[V](receiver)+"/*(non-virtual) "+declClass.toJava+"*/"+call

            case GetStatic(_, declaringClass, name, _) =>
                s"${declaringClass.toJava}.$name"

            case GetField(_, declaringClass, name, _, receiver) =>
                s"${toTxtExpr(receiver)}/*${declaringClass.toJava}*/.$name"

        }
    }

    @inline final def toTxtStmt[V <: Var[V]](stmt: Stmt[V], includePC: Boolean): String = {
        val pc = if (includePC) s"/*pc=${stmt.pc}:*/" else ""
        stmt.astID match {
            case Return.ASTID => s"$pc return"
            case ReturnValue.ASTID =>
                val ReturnValue(_, expr) = stmt
                s"$pc return ${toTxtExpr(expr)}"
            case Throw.ASTID =>
                val Throw(_, exc) = stmt
                s"$pc throw ${toTxtExpr(exc)}"

            case Nop.ASTID => s"$pc ;"

            case MonitorEnter.ASTID =>
                val MonitorEnter(_, objRef) = stmt
                s"$pc monitorenter ${toTxtExpr(objRef)}"
            case MonitorExit.ASTID =>
                val MonitorExit(_, objRef) = stmt
                s"$pc monitorexit ${toTxtExpr(objRef)}"

            case Goto.ASTID =>
                val Goto(_, target) = stmt
                s"$pc goto $target"

            case JSR.ASTID =>
                val JSR(_, target) = stmt
                s"$pc jsr $target"
            case Ret.ASTID =>
                val Ret(_, targets) = stmt
                targets.mkString(s"$pc ret {", ",", "}")

            case If.ASTID =>
                val If(_, left, cond, right, target) = stmt
                s"$pc if(${toTxtExpr(left)} $cond ${toTxtExpr(right)}) goto $target"

            case Switch.ASTID =>
                val Switch(_, defaultTarget, index, npairs) = stmt
                var result = "\n"
                for (x <- npairs) { result = result+"    "+x._1+": goto "+x._2+";\n" }
                result = result+"    default: goto "+defaultTarget+"\n"
                s"$pc switch(${toTxtExpr(index)}){$result}"

            case Assignment.ASTID =>
                val Assignment(_, variable, expr) = stmt
                s"$pc ${variable.name} = ${toTxtExpr(expr)}"

            case ArrayStore.ASTID =>
                val ArrayStore(_, arrayRef, index, operandVar) = stmt
                s"$pc ${toTxtExpr(arrayRef)}[${toTxtExpr(index)}] = ${toTxtExpr(operandVar)}"

            case PutStatic.ASTID =>
                val PutStatic(_, declaringClass, name, _, value) = stmt
                s"$pc ${declaringClass.toJava}.$name = ${toTxtExpr(value)}"

            case PutField.ASTID =>
                val PutField(_, declaringClass, name, _, receiver, value) = stmt
                val field = s"${toTxtExpr(receiver)}/*${declaringClass.toJava}*/.$name"
                s"$pc $field = ${toTxtExpr(value)}"

            case StaticMethodCall.ASTID =>
                val StaticMethodCall(_, declClass, _, name, _ /* descriptor*/ , params) = stmt
                s"$pc ${declClass.toJava}${callToTxt(name, params)}"

            case VirtualMethodCall.ASTID =>
                val VirtualMethodCall(_, declClass, _, name, _ /*desc.*/ , receiver, params) = stmt
                val call = callToTxt(name, params)
                s"$pc ${toTxtExpr(receiver)}/*${declClass.toJava}*/$call"

            case NonVirtualMethodCall.ASTID =>
                val NonVirtualMethodCall(_, declClass, _, name, _ /*desc.*/ , rec, params) = stmt
                val call = callToTxt(name, params)
                s"$pc ${toTxtExpr(rec)}/*(non-virtual) ${declClass.toJava}*/$call"

            case InvokedynamicMethodCall.ASTID =>
                val InvokedynamicMethodCall(_, bootstrapMethod, name, _ /*desc.*/ , params) = stmt
                s"$pc invokedynamic[${bootstrapMethod.toJava}]${callToTxt(name, params)}"

            case Checkcast.ASTID =>
                val Checkcast(_, value, tpe) = stmt
                s"$pc (${tpe.asReferenceType.toJava}) ${toTxtExpr(value)}"

            case CaughtException.ASTID =>
                val e = stmt.asCaughtException
                val t = { e.exceptionType.map(_.toJava).getOrElse("<ANY>") }
                s"$pc caught $t /* <= ${e.exceptionLocations.mkString("{", ",", "}")}*/"

            case ExprStmt.ASTID =>
                val ExprStmt(_, expr) = stmt
                s"$pc /*expression value is ignored:*/${toTxtExpr(expr)}"

        }
    }

    private def qualify(index: Int, javaLikeStmt: String, indented: Boolean): String = {
        if (indented)
            f"$index%5d:${javaLikeStmt.replace("\n", "\n       ")}"
        else
            s"$index:$javaLikeStmt"
    }

    final def stmtsToTxtStmt[V <: Var[V]](stmts: Array[Stmt[V]], includePC: Boolean): String = {
        stmts.iterator.zipWithIndex.map { stmtWithIndex =>
            val (stmt, index) = stmtWithIndex
            qualify(index, toTxtStmt(stmt, includePC), indented = false)
        }.mkString("\n")
    }

    def apply[P <: AnyRef, V <: Var[V]](tac: TACode[P, V]): scala.collection.Seq[String] = {
        apply(
            tac.params, tac.stmts, tac.cfg,
            skipParams = false, indented = true, includePC = false
        )
    }

    /**
     * Converts the statements to some human readable text.
     *
     * @param includePC If `true` the original program counter is also shown in the output.
     */
    def apply[P <: AnyRef, V <: Var[V]](
        params:     Parameters[P],
        stmts:      Array[Stmt[V]],
        cfg:        CFG[Stmt[V], TACStmts[V]],
        skipParams: Boolean,
        indented:   Boolean,
        includePC:  Boolean
    ): scala.collection.Seq[String] = {
        val indention = " " * (if (indented) 6 else 0)
        val max = stmts.length
        val javaLikeCode = new scala.collection.mutable.ArrayBuffer[String](stmts.length * 3)

        if (!skipParams) {
            if (params.parameters.nonEmpty) {
                javaLikeCode += indention+"/* PARAMETERS:"
                params.parameters.zipWithIndex foreach { paramWithIndex =>
                    val (param, index) = paramWithIndex
                    if (param ne null) {
                        val paramTxt = indention+"   param"+index.toHexString+": "+param.toString()
                        javaLikeCode += (param match {
                            case v: DVar[_] =>
                                v.useSites.mkString(s"$paramTxt // use sites={", ", ", "}")
                            case _ =>
                                paramTxt
                        })
                    }
                }
                javaLikeCode += indention+"*/"
            } else {
                javaLikeCode += "/* NO PARAMETERS */"
            }
        }

        var index = 0
        while (index < max) {

            def catchTypeToString(t: Option[Type]): String = t.map(_.toJava).getOrElse("<FINALLY>")

            val bb = cfg.bb(index)
            assert(
                bb ne null,
                s"index: $index; max: $max; catchNodes:${cfg.catchNodes.mkString("{", ", ", "}")}"
            )
            if (bb.startPC == index) {
                // we are at the beginning of a basic block
                if (index > 0) javaLikeCode += "" // an empty line
                val predecessors = bb.predecessors
                if (predecessors.nonEmpty) {
                    javaLikeCode +=
                        predecessors.map {
                            case bb: BasicBlock => bb.endPC.toString
                            case cn: CatchNode  => catchTypeToString(cn.catchType)
                        }.toList.sorted.mkString(
                            indention+"// "+(if (index == 0) "<start>, " else ""),
                            ", ",
                            " ->"
                        )
                }
            }

            javaLikeCode += qualify(index, toTxtStmt(stmts(index), includePC), indented)

            if (bb.endPC == index && bb.successors.exists(!_.isBasicBlock)) {
                val successors =
                    bb.successors.toSeq.sorted.collect {
                        case cn: CatchNode =>
                            s"⚡️ ${catchTypeToString(cn.catchType)} -> ${cn.handlerPC}"
                        case ExitNode(false) =>
                            "⚡️ <uncaught exception => abnormal return>"
                    }

                var head = (" " * (if (indented) 6 else 0))+"// "
                if (stmts(index).astID != Throw.ASTID && bb.successors.forall(successor => {
                    !successor.isBasicBlock && !successor.isNormalReturnExitNode
                }))
                    head += "⚠️ ALWAYS THROWS EXCEPTION – "
                if (successors.nonEmpty)
                    javaLikeCode +=
                        successors.mkString(head, ", ", "")
            }

            index += 1
        }

        javaLikeCode
    }

    /**
     *  Creates a text based representation of the three address code generated for the given
     *  method.
     */
    def apply(
        method:         Method,
        classHierarchy: ClassHierarchy                                            = ClassHierarchy.PreInitializedClassHierarchy,
        aiResult:       Option[AIResult { val domain: Domain with RecordDefUse }] = None
    ): String = {
        aiResult.map { aiResult =>
            val taCode = TACAI(method, classHierarchy, aiResult, propagateConstants = true)(Nil)
            ToTxt(taCode.params, taCode.stmts, taCode.cfg, skipParams = false, true, true)
        }.getOrElse {
            val taCode = TACNaive(method, classHierarchy, List(SimplePropagation))
            ToTxt(taCode.params, taCode.stmts, taCode.cfg, skipParams = true, true, true)
        }.mkString("\n")
    }

}
