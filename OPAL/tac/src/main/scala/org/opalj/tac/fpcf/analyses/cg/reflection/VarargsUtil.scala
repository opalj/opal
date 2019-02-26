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

// todo merge both methods
object VarargsUtil {
    /**
     * Returns the origins of parameters that are contained in a varargs argument.
     */
    def getParamsFromVararg(
        expr:  Expr[V],
        stmts: Array[Stmt[V]],
        cfg:   CFG[Stmt[V], TACStmts[V]]
    ): Option[Seq[V]] = {
        val definitions = expr.asVar.definedBy
        if (!definitions.isSingletonSet || definitions.head < 0) {
            None
        } else {
            val definition = stmts(definitions.head).asAssignment
            if (definition.expr.isNullExpr) {
                Some(Seq.empty)
            } else if (definition.expr.astID != NewArray.ASTID) {
                None
            } else {
                val uses = IntArraySetBuilder(definition.targetVar.usedBy.toChain).result()
                if (cfg.bb(uses.head) != cfg.bb(uses.last)) {
                    // IMPROVE: Here we should also handle the case of non-constant values
                    None
                } else if (stmts(uses.last).astID != Assignment.ASTID &&
                    stmts(uses.last).astID != ExprStmt.ASTID) {
                    None
                } else {
                    var params: Seq[V] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last)
                            true
                        else {
                            val use = stmts(useSite)
                            if (use.astID != ArrayStore.ASTID)
                                false
                            else {
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!indices.isSingletonSet || indices.head < 0)
                                    false
                                else {
                                    val index = stmts(indices.head).asAssignment.expr
                                    if (!index.isIntConst) {
                                        false // we don't know the index in the array
                                    } else {
                                        params = params.updated(
                                            index.asIntConst.value,
                                            use.asArrayStore.value.asVar
                                        )
                                        true
                                    }
                                }
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
                if (cfg.bb(uses.head) != cfg.bb(uses.last)) {
                    None
                } else if (stmts(uses.last).astID != Assignment.ASTID) {
                    None
                } else {
                    var types: RefArray[FieldType] = RefArray.withSize(uses.size - 1)
                    if (!uses.forall { useSite ⇒
                        if (useSite == uses.last)
                            true
                        else {
                            val use = stmts(useSite)
                            if (use.astID != ArrayStore.ASTID)
                                false
                            else {
                                val typeDefs = use.asArrayStore.value.asVar.definedBy
                                val indices = use.asArrayStore.index.asVar.definedBy
                                if (!typeDefs.isSingletonSet || typeDefs.head < 0 ||
                                    !indices.isSingletonSet || indices.head < 0)
                                    false
                                else {
                                    val typeDef = stmts(typeDefs.head).asAssignment.expr
                                    val index = stmts(indices.head).asAssignment.expr
                                    if (!typeDef.isClassConst && !TypesUtil.isBaseTypeLoad(typeDef))
                                        false
                                    else if (!index.isIntConst) {
                                        false // we don't know the index in the array
                                    } else {
                                        val tpe =
                                            if (typeDef.isClassConst) typeDef.asClassConst.value
                                            else TypesUtil.getBaseType(typeDef).asBaseType
                                        types = types.updated(index.asIntConst.value, tpe)
                                        true
                                    }
                                }
                            }
                        }
                    } || types.contains(null)) {
                        None
                    } else {
                        Some(types)
                    }
                }
            }
        }
    }
}
