/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.tac.fpcf.properties.TACAI

object AllocationsUtil {

    /**
     * Processes a single allocation site in a given context method.
     */
    def handleAllocation[ContextType <: Context](
        allocationContext: ContextType,
        allocationPC:      Int,
        data:              AnyRef,
        failure:           () => Unit
    )(process: (ContextType, Int, Array[Stmt[V]]) => Unit)(
        implicit
        state: TypeProviderState,
        ps:    PropertyStore
    ): Unit = {
        if (allocationContext eq NoContext) {
            failure()
        } else {
            val allocationMethod = allocationContext.method
            if (allocationPC >= 0 &&
                allocationMethod.hasSingleDefinedMethod &&
                allocationMethod.definedMethod.body.isDefined) {
                val epk = EPK(allocationMethod.definedMethod, TACAI.key)
                val tacEOptP = if (state.hasDependee(epk)) state.getProperty(epk) else ps(epk)

                if (tacEOptP.isRefinable) {
                    val depender = (allocationContext, allocationPC, data)
                    state.addDependency(depender, tacEOptP)
                }

                if (tacEOptP.isEPS)
                    handleAllocation(
                        allocationContext, allocationPC, tacEOptP.asEPS, failure
                    )(process)
            } else {
                failure()
            }
        }
    }

    /**
     * Processes a single allocation site in a method for which the TACAI is given.
     */
    private def handleAllocation[ContextType <: Context](
        allocationContext: ContextType,
        allocationPC:      Int,
        tacEOptP:          EPS[Method, TACAI],
        failure:           () => Unit
    )(process: (ContextType, Int, Array[Stmt[V]]) => Unit): Unit = {
        val tacO = tacEOptP.ub.tac
        if (tacO.isDefined) {
            val tac = tacO.get
            process(allocationContext, tac.properStmtIndexForPC(allocationPC), tac.stmts)
        } else {
            failure()
        }
    }

    /**
     * Processes allocation sites for a given local variable.
     * Clients MUST handle dependencies where the depender is the given one and the dependee
     * provides further allocation sites.
     */
    def handleAllocations[ContextType <: Context](
        value:      V,
        context:    ContextType,
        depender:   Entity,
        stmts:      Array[Stmt[V]],
        typeFilter: ReferenceType => Boolean,
        failure:    () => Unit
    )(process: (ContextType, Int, Array[Stmt[V]]) => Unit)(
        implicit
        typeProvider: TypeProvider,
        state:        TypeProviderState,
        ps:           PropertyStore
    ): Unit = {
        val allocations = typeProvider.typesProperty(
            value, context.asInstanceOf[typeProvider.ContextType], depender, stmts
        )
        typeProvider.foreachAllocation(value, context, stmts, allocations) {
            (tpe, allocationContext, pc) =>
                if (typeFilter(tpe)) {
                    handleAllocation(
                        context,
                        value,
                        stmts,
                        allocationContext.asInstanceOf[ContextType],
                        pc,
                        depender,
                        failure
                    )(process)
                }
        }
    }

    private def handleAllocation[ContextType <: Context](
        context:           ContextType,
        value:             V,
        stmts:             Array[Stmt[V]],
        allocationContext: ContextType,
        allocationPC:      Int,
        data:              AnyRef,
        failure:           () => Unit
    )(process: (ContextType, Int, Array[Stmt[V]]) => Unit)(
        implicit
        state: TypeProviderState,
        ps:    PropertyStore
    ): Unit = {
        if (allocationContext eq NoContext) {
            failure()
            value.definedBy.foreach { index =>
                if (index >= 0) {
                    process(context, index, stmts)
                } else {
                    failure()
                }
            }
        } else {
            handleAllocation(allocationContext, allocationPC, data, failure)(process)
        }
    }

    /**
     * Provides an easy way to handle updates to allocation sites dependees registered by the
     * methods in this class.
     * This method handles dependencies where the depender satisfies the `dataType` predicate
     * (beware of type erasure!).
     * It is of utmost importance to handle all possible dependencies to ensure termination!
     */
    def continuationForAllocation[DataType, ContextType <: Context](
        eps:      SomeEPS,
        context:  ContextType,
        value:    DataType => (V, Array[Stmt[V]]),
        dataType: Entity => Boolean,
        failure:  DataType => Unit
    )(process: (DataType, ContextType, Int, Array[Stmt[V]]) => Unit)(
        implicit
        typeProvider: TypeProvider,
        state:        TypeProviderState,
        ps:           PropertyStore
    ): Unit = {
        val epk = eps.toEPK

        if (state.hasDependee(epk)) {
            val deps = state.dependersOf(epk)

            eps.ub match {
                case _: TACAI =>
                    deps.foreach {
                        case (allocationContext, allocationPC: Int, data: Entity) if dataType(data) =>
                            handleAllocation(
                                allocationContext.asInstanceOf[ContextType],
                                allocationPC,
                                eps.asInstanceOf[EPS[Method, TACAI]],
                                () => failure(data.asInstanceOf[DataType])
                            ) { (_allocationContext, allocationIndex, _stmts) =>
                                    process(
                                        data.asInstanceOf[DataType],
                                        _allocationContext.asInstanceOf[ContextType],
                                        allocationIndex,
                                        _stmts
                                    )
                                }

                        case _ =>
                    }
                case _ =>
                    deps.foreach {
                        case data: Entity if dataType(data) =>
                            val (expr, stmts) = value(data.asInstanceOf[DataType])
                            typeProvider.continuationForAllocations(
                                expr, eps.asInstanceOf[EPS[Entity, typeProvider.PropertyType]]
                            ) { (_, allocationContext, allocationPC) =>
                                handleAllocation(
                                    context, expr, stmts,
                                    allocationContext, allocationPC, data,
                                    () => failure(data.asInstanceOf[DataType])
                                ) { (_allocationContext, allocationIndex, _stmts) =>
                                        process(
                                            data.asInstanceOf[DataType],
                                            _allocationContext.asInstanceOf[ContextType],
                                            allocationIndex,
                                            _stmts
                                        )
                                    }
                            }
                        case _ =>
                    }
            }
        }
    }
}
