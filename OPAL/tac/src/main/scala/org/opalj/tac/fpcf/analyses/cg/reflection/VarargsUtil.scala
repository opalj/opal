/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection
import org.opalj.collection.immutable.IntArraySetBuilder
import org.opalj.collection.immutable.RefArray
import org.opalj.br.FieldType
import org.opalj.br.FieldTypes
import org.opalj.br.cfg.CFG

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
        stmts: Array[Stmt[V]],
        cfg:   CFG[Stmt[V], TACStmts[V]]
    ): Option[RefArray[V]] = {
        getTFromVarArgs(expr, stmts, cfg, fillParam)
    }

    /**
     * Returns the types that a varargs argument of type Class (i.e. Class<?>...) may evaluate to.
     * Only handles the case of a simple array of class constants or primitive types' classes!
     * In case [[None]] is returned, the caller must mark the callsite as incomplete.
     */
    private[reflection] def getTypesFromVararg(
        expr:  Expr[V],
        stmts: Array[Stmt[V]],
        cfg:   CFG[Stmt[V], TACStmts[V]]
    ): Option[FieldTypes] = {
        getTFromVarArgs(expr, stmts, cfg, fillType)
    }

    private[this] def getTFromVarArgs[T](
        expr:      Expr[V],
        stmts:     Array[Stmt[V]],
        cfg:       CFG[Stmt[V], TACStmts[V]],
        fillEntry: (ArrayStore[V], Array[Stmt[V]], RefArray[T]) ⇒ Option[RefArray[T]]
    ): Option[RefArray[T]] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            None
        } else {
            val definition = stmts(definitions.head).asAssignment
            if (definition.expr.isNullExpr) {
                Some(RefArray.empty)
            } else if (definition.expr.astID != NewArray.ASTID) {
                None
            } else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                var params: RefArray[T] = RefArray.withSize(uses.size - 1)
                if (!uses.forall { useSite ⇒
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
                // }
            }
        }
    }

    // todo: merge both methods
    @inline private[this] def fillParam(use: ArrayStore[V], stmts: Array[Stmt[V]], params: RefArray[V]): Option[RefArray[V]] = {
        val indices = use.index.asVar.definedBy
        if (!indices.isSingletonSet || indices.head < 0)
            None
        else {
            val index = stmts(indices.head).asAssignment.expr
            if (!index.isIntConst) {
                None // we don't know the index in the array
            } else {
                Some(params.updated(
                    index.asIntConst.value,
                    use.asArrayStore.value.asVar
                ))
            }
        }
    }

    @inline private[this] def fillType(
        use: ArrayStore[V], stmts: Array[Stmt[V]], params: RefArray[FieldType]
    ): Option[RefArray[FieldType]] = {
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
            else if (!index.isIntConst) {
                None // we don't know the index in the array
            } else {
                val tpe =
                    if (typeDef.isClassConst) typeDef.asClassConst.value
                    else TypesUtil.getBaseType(typeDef).asBaseType
                Some(params.updated(index.asIntConst.value, tpe))
            }
        }
    }
}
