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
import org.opalj.br._
import org.opalj.br.analyses.{AnalysisModeConfigFactory, FormalParameter, FormalParameters, Project, PropertyStoreKey, SomeProject}
import org.opalj.collection.immutable.IntSet
import org.opalj.fpcf.properties._
import org.opalj.tac.{NonVirtualMethodCall, _}
import org.opalj.util.PerformanceEvaluation.time

/**
 * A very simple (mostly) intra-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[Domain#DomainValue]

    /**
     * Determines whether the given entity on the given definition site with given uses of that
     * allocation/parameter escapes in the given code.
     */
    private def doDetermineEscape(e: Entity, defSite: Int, uses: IntSet,
                                  code: Array[Stmt[V]]): PropertyComputationResult = {
        var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
        for (use ← uses) {
            determineEscapeStmt(code(use), e, defSite) match {
                case Some(result) ⇒ return result
                case _            ⇒
            }
        }

        /**
         * Determines whether the given statement leads to an escape of the entity e with definition
         * site defSite
         */
        def determineEscapeStmt(stmt: Stmt[V], e: Entity, defSite: Int): Option[PropertyComputationResult] = {
            stmt.astID match {
                case PutStatic.ASTID ⇒
                    val PutStatic(_, _, _, _, value) = stmt
                    examineExpr(value, e, defSite)
                // we are field insensitive, so we have to consider a field (and array) write as
                // GlobalEscape
                case PutField.ASTID ⇒
                    val PutField(_, _, _, _, _, value) = stmt
                    examineExpr(value, e, defSite)
                case ArrayStore.ASTID ⇒
                    val ArrayStore(_, _, _, value) = stmt
                    examineExpr(value, e, defSite)
                case Throw.ASTID ⇒
                    val Throw(_, value) = stmt
                    examineExpr(value, e, defSite)
                // we are inter-procedural
                case ReturnValue.ASTID ⇒
                    val ReturnValue(_, value) = stmt
                    examineExpr(value, e, defSite)
                case StaticMethodCall.ASTID ⇒
                    val StaticMethodCall(_, _, _, _, _, params) = stmt
                    examineParameters(params, e, defSite)
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(_, _, _, _, _, value, params) = stmt
                    examineExpr(value, e, defSite).orElse(examineParameters(params, e, defSite))
                case NonVirtualMethodCall.ASTID ⇒
                    val NonVirtualMethodCall(_, dc, interface, name, descr, receiver, params) = stmt
                    handleNonVirtualCall(e, defSite, dc, interface, name, descr, receiver, params)
                case FailingStatement.ASTID ⇒
                    val FailingStatement(_, failingStmt) = stmt
                    determineEscapeStmt(failingStmt, e, defSite)
                case ExprStmt.ASTID ⇒
                    val ExprStmt(_, expr) = stmt
                    examineCallOrCheckCastExpr(e, defSite, expr)

                case Assignment.ASTID ⇒
                    val Assignment(_, left, right) = stmt
                    if (left.astID == Var.ASTID && right.astID == Checkcast.ASTID) {
                        val Checkcast(_, value, _) = right
                        // val DVar(_, newUses) = left
                        if (value.astID == Var.ASTID) {
                            val UVar(_, defSites) = value
                            if (defSites.contains(defSite))
                                Some(ImmediateResult(e, GlobalEscape))
                                // TODO spawn new analysis, conditionally purity
                        }
                        None
                    } else
                        examineCallOrCheckCastExpr(e, defSite, right)
                case _ ⇒ None
            }
        }

        /**
         * For a given entity with defSite, check whether the expression is a function call or a
         * CheckCast. For function call mark parameters and receiver objects that use the defSite as
         * GlobalEscape. CheckCasts with a value using the defSite should not be possible as the
         * concrete type is known. Therefore an RuntimeException is thrown.
         */
        def examineCallOrCheckCastExpr(e: Entity, defSite: Int, expr: Expr[V]): Option[PropertyComputationResult] = {
            expr.astID match {
                case NonVirtualFunctionCall.ASTID ⇒
                    val NonVirtualFunctionCall(_, dc, interface, name, descr, receiver, params) = expr
                    handleNonVirtualCall(e, defSite, dc, interface, name, descr, receiver, params)
                case VirtualFunctionCall.ASTID ⇒
                    val VirtualFunctionCall(_, _, _, _, _, receiver, params) = expr
                    examineExpr(receiver, e, defSite).orElse(examineParameters(params, e,
                        defSite))
                case StaticFunctionCall.ASTID ⇒
                    val StaticFunctionCall(_, _, _, _, _, params) = expr
                    examineParameters(params, e, defSite)
                // see Java8LambdaExpressionsRewriting
                case Invokedynamic.ASTID ⇒
                    val Invokedynamic(_, _, _, _, params) = expr
                    examineParameters(params, e, defSite)
                case _ ⇒ None
            }
        }

        /**
         * If the given expression is a [[UVar]] and is a use of the defSite, the entity e will be
         * marked
         * as GlobalEscape, otherwise [[None]] is returned
         */
        def examineExpr(expr: Expr[V], e: Entity, defSite: Int): Option[PropertyComputationResult] = {
            if (expr.astID == Var.ASTID) {
                val UVar(_, defSites) = expr
                if (defSites.contains(defSite))
                    Some(ImmediateResult(e, GlobalEscape))
                else None
            } else None
        }

        /**
         * If there exists a [[UVar]] in params that is a use of the defSite, e will be marked as
         * GlobalEscape, otherwise [[None]] is returned
         */
        def examineParameters(params: Seq[Expr[V]], e: Entity, defSite: Int): Option[PropertyComputationResult] = {
            if (params.exists { case UVar(_, defSites) ⇒ defSites.contains(defSite) })
                Some(ImmediateResult(e, GlobalEscape))
            else None
        }

        /**
         * Special handling for constructor calls, as the receiver of an constructor is always an
         * allocation site.
         * The constructor of Object does not escape the self reference by definition. For other
         * constructor, the inter procedural chain will be processed until it reaches the Object
         * constructor or escapes.
         * For non constructor calls, GlobalEscape of e will be returned whenever the receiver or a
         * parameter is a use of defSite.
         */
        def handleNonVirtualCall(e: Entity, defSite: Int, dc: ReferenceType, interface: Boolean,
                                 name: String, descr: MethodDescriptor,
                                 receiver: Expr[V], params: Seq[Expr[V]]): Option[PropertyComputationResult] = {
            // we only allow special (inter-procedural) handling for constructors
            if (name == "<init>")
                // the object constructor will not escape the this local
                if (dc != ObjectType.Object && receiver.astID == Var.ASTID) {
                    val UVar(_, defSites) = receiver
                    if (defSites.contains(defSite)) {
                        // resolve the constructor
                        val mresult = project.specialCall(dc.asObjectType, interface, "<init>", descr)
                        mresult match {
                            case Success(m) ⇒
                                val fp = propertyStore.context[FormalParameters]
                                // check if the this local escapes in the callee
                                val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                                escapeState match {
                                    case EP(_, NoEscape) ⇒ None
                                    case EP(_, GlobalEscape) ⇒
                                        Some(ImmediateResult(e, GlobalEscape))
                                    case EP(_, MethodEscape) ⇒
                                        throw new RuntimeException("not yet implemented")
                                    // result not yet finished
                                    case epk ⇒
                                        dependees += epk
                                        None

                                }
                            case /* unknown method */ _ ⇒ Some(ImmediateResult(e, GlobalEscape))
                        }
                    } else None
                } else None
            else {
                examineExpr(receiver, e, defSite).orElse(examineParameters(params, e, defSite))
            }
        }

        // Every entity that is not identified as escaping is (conditionally) not escaping
        if (dependees.isEmpty)
            return ImmediateResult(e, NoEscape);

        def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
            p match {
                case GlobalEscape | MethodEscape ⇒
                    Result(e, GlobalEscape)

                case ConditionallyNoEscape ⇒
                    val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, ConditionallyPure, dependees, c)

                case NoEscape ⇒
                    dependees = dependees.filter { _.e ne other }
                    if (dependees.isEmpty)
                        Result(e, NoEscape)
                    else
                        IntermediateResult(e, ConditionallyPure, dependees, c)

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
                val TACode(code, _, _, _) = project.get(DefaultTACAIKey)(m)

                val index = code.indexWhere(stmt ⇒ stmt.pc == pc)

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _)) ⇒
                            doDetermineEscape(as, index, uses, code)
                        case Assignment(`pc`, DVar(_, uses), NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ Result(e, NoEscape)
            case FormalParameter(m, -1) if m.name == "<init>" ⇒
                //val TACode(code, _, _, _) = project.get(DefaultTACAIKey)(m)

                Result(e, GlobalEscape)

            case fp: FormalParameter ⇒ Result(fp, GlobalEscape)
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

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        time {
            analysesManager.runWithRecommended(SimpleEscapeAnalysis)(waitOnCompletion = true)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val propertyStore = project.get(PropertyStoreKey)
        val globalEscapes = propertyStore.entities(GlobalEscape).filter(_.isInstanceOf[AllocationSite])
        val noEscape = propertyStore.entities(NoEscape).filter(_.isInstanceOf[AllocationSite])

        println(s"# of global escaping objects: ${globalEscapes.size}")
        println(s"# of local objects: ${noEscape.size}")

    }
}

