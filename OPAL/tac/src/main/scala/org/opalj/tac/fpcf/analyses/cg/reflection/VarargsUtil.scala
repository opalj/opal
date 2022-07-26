/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection
import org.opalj.collection.immutable.IntArraySetBuilder
import org.opalj.br.FieldType
import org.opalj.br.FieldTypes

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Utility class to retrieve types or expressions for varargs.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
object VarargsUtil {

    /**
     * Returns the origins of parameters that are contained in a varargs argument.
     */
    def getParamsFromVararg(
        expr:  Expr[V],
        stmts: Array[Stmt[V]]
    ): Option[ArraySeq[V]] = {
        getTFromVarArgs(expr, stmts, fillParam)
    }

    /**
     * Returns the types that a varargs argument of type Class (i.e. Class<?>...) may evaluate to.
     * Only handles the case of a simple array of class constants or primitive types' classes!
     * In case [[None]] is returned, the caller must mark the callsite as incomplete.
     */
    private[reflection] def getTypesFromVararg(
        expr:  Expr[V],
        stmts: Array[Stmt[V]]
    ): Option[FieldTypes] = {
        getTFromVarArgs(expr, stmts, fillType)
    }

    private[this] def getTFromVarArgs[T >: Null](
        expr:      Expr[V],
        stmts:     Array[Stmt[V]],
        fillEntry: (ArrayStore[V], Array[Stmt[V]], ArraySeq[T]) => Option[ArraySeq[T]]
    )(implicit ct: ClassTag[T]): Option[ArraySeq[T]] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            None
        } else {
            val definition = stmts(definitions.head).asAssignment
            if (definition.expr.isNullExpr) {
                Some(ArraySeq.empty)
            } else if (definition.expr.astID != NewArray.ASTID) {
                None
            } else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toList).result()
                var params: ArraySeq[T] = ArraySeq.unsafeWrapArray(new Array[T](uses.size - 1))
                if (!uses.forall { useSite =>
                    val use = stmts(useSite)
                    if (useSite == uses.last)
                        // todo: should we just check for invocations?
                        use.astID == Assignment.ASTID || use.astID == ExprStmt.ASTID
                    else {
                        if (use.astID != ArrayStore.ASTID)
                            false
                        else {
                            val update = fillEntry(use.asArrayStore, stmts, params)
                            if (update.isDefined) params = update.get
                            update.isDefined
                        }
                    }
                } || params.contains(null)) {
                    None
                } else {
                    Some(params)
                }
            }
        }
    }

    // todo: merge both methods
    @inline private[this] def fillParam(
        use: ArrayStore[V], stmts: Array[Stmt[V]], params: ArraySeq[V]
    ): Option[ArraySeq[V]] = {
        val indices = use.index.asVar.definedBy
        if (!indices.isSingletonSet || indices.head < 0)
            None
        else {
            val index = stmts(indices.head).asAssignment.expr
            if (!index.isIntConst || index.asIntConst.value >= params.size) {
                None // we don't know the index in the array or it is larger than expected
            } else {
                Some(params.updated(
                    index.asIntConst.value,
                    use.asArrayStore.value.asVar
                ))
            }
        }
    }

    @inline private[this] def fillType(
        use: ArrayStore[V], stmts: Array[Stmt[V]], params: ArraySeq[FieldType]
    ): Option[ArraySeq[FieldType]] = {
        val typeDefs = use.asArrayStore.value.asVar.definedBy
        val indices = use.asArrayStore.index.asVar.definedBy
        if (!typeDefs.isSingletonSet || typeDefs.head < 0 ||
            !indices.isSingletonSet || indices.head < 0)
            None
        else {
            val typeDef = stmts(typeDefs.head).asAssignment.expr
            val index = stmts(indices.head).asAssignment.expr
            if (!typeDef.isClassConst && !TypesUtil.isBaseTypeLoad(typeDef))
                None
            else if (!index.isIntConst || index.asIntConst.value >= params.size) {
                None // we don't know the index in the array or it is larger than expected
            } else {
                val tpe =
                    if (typeDef.isClassConst) typeDef.asClassConst.value
                    else TypesUtil.getBaseType(typeDef).asBaseType
                Some(params.updated(index.asIntConst.value, tpe))
            }
        }
    }
}
