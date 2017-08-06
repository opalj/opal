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
package fpcf
package analysis

import java.io.File

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.AllocationSite
import org.opalj.br.ExceptionHandlers
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntSet
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties._
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.DVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.FailingExpr
import org.opalj.tac.Invokedynamic
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.Throw
import org.opalj.tac.UVar
import org.opalj.tac.Var
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.util.PerformanceEvaluation.time

/**
 * A very simple flow-sensitive (mostly) intra-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    /**
     * Determines whether the given entity on the given definition site with given uses of that
     * allocation/parameter escapes in the given code.
     */
    private def doDetermineEscape(e: Entity, defSite: ValueOrigin, uses: IntSet,
                                  code: Array[Stmt[V]], handlers: ExceptionHandlers): PropertyComputationResult = {
        var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
        var worstProperty: EscapeProperty = NoEscape
        for (use ← uses) {
            val result = determineEscapeStmt(code(use), e, defSite)

            worstProperty = worstProperty meet result
        }

        /**
         * Determines whether the given statement leads to an escape of the entity e with definition
         * site defSite
         */
        def determineEscapeStmt(stmt: Stmt[V], e: Entity, defSite: Int): EscapeProperty = {
            stmt.astID match {
                case PutStatic.ASTID ⇒
                    val PutStatic(_, _, _, _, value) = stmt
                    if (usesDefSite(value, e, defSite))
                        GlobalEscapeViaStaticFieldAssignment
                    else NoEscape
                // we are field insensitive, so we have to consider a field (and array) write as
                // GlobalEscape
                case PutField.ASTID ⇒
                    val PutField(_, _, _, _, _, value) = stmt
                    if (usesDefSite(value, e, defSite)) MaybeNoEscape else NoEscape
                case ArrayStore.ASTID ⇒
                    val ArrayStore(_, _, _, value) = stmt
                    if (usesDefSite(value, e, defSite)) MaybeNoEscape else NoEscape
                case Throw.ASTID ⇒
                    val Throw(_, value) = stmt
                    // the exception could be catched, so we know nothing
                    if (usesDefSite(value, e, defSite)) MaybeNoEscape else NoEscape
                // we are inter-procedural
                case ReturnValue.ASTID ⇒
                    val ReturnValue(_, value) = stmt
                    if (usesDefSite(value, e, defSite)) MaybeMethodEscape else NoEscape
                case StaticMethodCall.ASTID ⇒
                    val StaticMethodCall(_, _, _, _, _, params) = stmt
                    if (anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(_, _, _, _, _, value, params) = stmt
                    if (usesDefSite(value, e, defSite) ||
                        anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                case NonVirtualMethodCall.ASTID ⇒
                    val NonVirtualMethodCall(_, dc, interface, name, descr, receiver, params) = stmt
                    handleNonVirtualCall(e, defSite, dc, interface, name, descr, receiver, params)
                case FailingExpr.ASTID ⇒
                    //val FailingExpr(_, failingStmt) = stmt
                    println(stmt)
                    NoEscape
                //determineEscapeStmt(failingStmt, e, defSite)
                case ExprStmt.ASTID ⇒
                    val ExprStmt(_, expr) = stmt
                    examineCall(e, defSite, expr)

                case Assignment.ASTID ⇒
                    val Assignment(_, _, right) = stmt
                    examineCall(e, defSite, right)
                case _ ⇒ NoEscape
            }
        }

        /**
         * For a given entity with defSite, check whether the expression is a function call or a
         * CheckCast. For function call mark parameters and receiver objects that use the defSite as
         * GlobalEscape.
         */
        def examineCall(e: Entity, defSite: Int, expr: Expr[V]): EscapeProperty = {
            expr.astID match {
                case NonVirtualFunctionCall.ASTID ⇒
                    val NonVirtualFunctionCall(_, dc, interface, name, descr, receiver, params) = expr
                    handleNonVirtualCall(e, defSite, dc, interface, name, descr, receiver, params)
                case VirtualFunctionCall.ASTID ⇒
                    val VirtualFunctionCall(_, _, _, _, _, receiver, params) = expr
                    if (usesDefSite(receiver, e, defSite) ||
                        anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                case StaticFunctionCall.ASTID ⇒
                    val StaticFunctionCall(_, _, _, _, _, params) = expr
                    if (anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                // see Java8LambdaExpressionsRewriting
                case Invokedynamic.ASTID ⇒
                    val Invokedynamic(_, _, _, _, params) = expr
                    if (anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                case _ ⇒ NoEscape
            }
        }

        /**
         * If the given expression is a [[UVar]] and is a use of the defSite, the entity e will be
         * marked as [[GlobalEscape]], otherwise [[None]] is returned
         */
        def usesDefSite(expr: Expr[V], e: Entity, defSite: Int): Boolean = {
            if (expr.astID == Var.ASTID) {
                val UVar(_, defSites) = expr
                if (defSites.contains(defSite))
                    true
                else false
            } else false
        }

        /**
         * If there exists a [[UVar]] in params that is a use of the defSite, e will be marked as
         * GlobalEscape, otherwise [[None]] is returned
         */
        def anyParameterUsesDefSite(params: Seq[Expr[V]], e: Entity, defSite: Int): Boolean = {
            if (params.exists { case UVar(_, defSites) ⇒ defSites.contains(defSite) })
                true
            else false
        }

        /**
         * Special handling for constructor calls, as the receiver of an constructor is always an
         * allocation site.
         * The constructor of Object does not escape the self reference by definition. For other
         * constructor, the inter procedural chain will be processed until it reaches the Object
         * constructor or escapes.
         * For non constructor calls, [[GlobalEscape]] of e will be returned whenever the receiver
         * or a parameter is a use of defSite.
         */
        def handleNonVirtualCall(e: Entity, defSite: Int, dc: ReferenceType, interface: Boolean,
                                 name: String, descr: MethodDescriptor,
                                 receiver: Expr[V], params: Seq[Expr[V]]): EscapeProperty = {
            // we only allow special (inter-procedural) handling for constructors
            if (name == "<init>") {
                // the object constructor will not escape the this local
                if (dc != ObjectType.Object) {
                    // this is safe as we assume a flat tac hierarchy
                    val UVar(_, defSites) = receiver
                    if (defSites.contains(defSite))
                        // resolve the constructor
                        project.specialCall(dc.asObjectType, interface, "<init>", descr) match {
                            case Success(m) ⇒
                                val fp = propertyStore.context[FormalParameters]
                                // check if the this local escapes in the callee
                                val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                                val tmpResult = escapeState match {
                                    case EP(_, NoEscape)            ⇒ NoEscape
                                    case EP(_, state: GlobalEscape) ⇒ state
                                    case EP(_, MaybeNoEscape) ⇒
                                        MaybeNoEscape
                                    case EP(_, MaybeArgEscape) ⇒
                                        MaybeArgEscape
                                    case EP(_, MaybeMethodEscape) ⇒
                                        MaybeMethodEscape
                                    case EP(_, ConditionallyNoEscape) ⇒
                                        dependees += escapeState
                                        NoEscape
                                    case EP(_, ConditionallyArgEscape) ⇒
                                        dependees += escapeState
                                        NoEscape
                                    case EP(_, ConditionallyMethodEscape) ⇒
                                        dependees += escapeState
                                        NoEscape
                                    case EP(_, _) ⇒
                                        throw new RuntimeException("not yet implemented")
                                    // result not yet finished
                                    case epk ⇒
                                        dependees += epk
                                        NoEscape

                                }
                                worstProperty = tmpResult meet worstProperty
                            case /* unknown method */ _ ⇒
                                worstProperty = MaybeNoEscape meet worstProperty
                        }
                    if (anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                } else /* Object constructor does escape by def. */ NoEscape
            } else {
                if (usesDefSite(receiver, e, defSite) ||
                    anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
            }
        }

        // Every entity that is not identified as escaping is not escaping
        if (dependees.isEmpty)
            return Result(e, worstProperty)

        // we depend on the result for other entities, lets construct a continuation
        def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
            p match {
                case state: GlobalEscape ⇒
                    Result(e, state)
                case MaybeNoEscape     ⇒ Result(e, MaybeNoEscape)
                case MaybeArgEscape    ⇒ Result(e, MaybeArgEscape)
                case MaybeMethodEscape ⇒ Result(e, MaybeMethodEscape)

                case ConditionallyNoEscape ⇒
                    val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, ConditionallyNoEscape, dependees, c)

                case NoEscape ⇒
                    dependees = dependees.filter {
                        _.e ne other
                    }
                    if (dependees.isEmpty)
                        Result(e, worstProperty)
                    else
                        IntermediateResult(e, ConditionallyNoEscape, dependees, c)

            }
        }

        IntermediateResult(e, ConditionallyNoEscape, dependees, c)
    }

    /**
     * Determine whether the given entity ([[AllocationSite]] or [[FormalParameter]]) escapes
     * its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as @ AllocationSite(m, pc) ⇒
                val TACode(_, code, _, handlers, _) = project.get(DefaultTACAIKey)(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _)) ⇒
                            doDetermineEscape(as, index, uses, code, handlers)
                        case Assignment(`pc`, DVar(_, uses), NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code, handlers)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ Result(e, NoEscape)
            case FormalParameter(m, _) if m.body.isEmpty ⇒ Result(e, MaybeNoEscape)
            case FormalParameter(m, -1) if m.name == "<init>" ⇒
                val TACode(params, code, _, handlers, _) = project.get(DefaultTACAIKey)(m)
                val thisParam = params.thisParameter
                doDetermineEscape(e, thisParam.origin, thisParam.useSites, code, handlers)
            case fp: FormalParameter ⇒ Result(fp, MaybeNoEscape)
        }

    }
}

object SimpleEscapeAnalysis extends FPCFAnalysisRunner {
    type V = DUVar[Domain#DomainValue]

    def entitySelector: PartialFunction[Entity, Entity] = {
        case as: AllocationSite  ⇒ as
        case fp: FormalParameter ⇒ fp
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore <||< (entitySelector, analysis.determineEscape)
        analysis
    }

    def main(args: Array[String]): Unit = {
        val rt = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/rt.jar")
        val charsets = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/charset.jar")
        val deploy = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/deploy.jar")
        val javaws = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/javaws.jar")
        val jce = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/jce.jar")
        val jfr = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/jfr.jar")
        val jfxswt = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/jfxswt.jar")
        val jsse = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/jsse.jar")
        val managementagent = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/management-agent.jar")
        val plugin = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/plugin.jar")
        val resources = new File("/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/resources.jar")
        val project = Project(Array(rt, charsets, deploy, javaws, jce, jfr, jfxswt, jsse,
            managementagent, plugin, resources), Array.empty[File])

        val testConfig = AnalysisModeConfigFactory.createConfig(AnalysisModes.OPA)
        Project.recreate(project, testConfig)

        SimpleAIKey.domainFactory = (p, cf, m) ⇒ new PrimitiveTACAIDomain(p.classHierarchy, cf, m)
        time {
            val tacai = project.get(DefaultTACAIKey)
            for {
                m ← project.allMethodsWithBody.par
            } {
                tacai(m)
            }
        } { t ⇒ println(s"tac took ${t.toSeconds}") }

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        time {
            analysesManager.runWithRecommended(SimpleEscapeAnalysis)(waitOnCompletion = true)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val propertyStore = project.get(PropertyStoreKey)
        val staticEscapes =
            propertyStore.entities(GlobalEscapeViaStaticFieldAssignment).collect { case as: AllocationSite ⇒ as }
        val maybeNoEscape =
            propertyStore.entities(MaybeNoEscape).collect { case as: AllocationSite ⇒ as }
        val maybeArgEscape =
            propertyStore.entities(MaybeArgEscape).collect { case as: AllocationSite ⇒ as }
        val maybeMethodEscape =
            propertyStore.entities(MaybeMethodEscape).filter(_.isInstanceOf[AllocationSite])
        val noEscape = propertyStore.entities(NoEscape).collect { case as: AllocationSite ⇒ as }
        for {
            as ← noEscape
        } {
            println(as.pc+":"+as.method.toJava(project.classFile(as.method)))
        }

        println(s"# of global escaping objects: ${staticEscapes.size}")
        println(s"# of maybe no escaping objects: ${maybeNoEscape.size}")
        println(s"# of maybe arg escaping objects: ${maybeArgEscape.size}")
        println(s"# of maybe method escaping objects: ${maybeMethodEscape.size}")
        println(s"# of local objects: ${noEscape.size}")
    }
}

