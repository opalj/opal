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
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.ObjectAllocationSite
import org.opalj.br.ArrayAllocationSite
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntSet
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
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.util.PerformanceEvaluation.time

/**
 * A very simple flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterproceduralEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    /**
     * Determines whether the given entity on the given definition site with given uses of that
     * allocation/parameter escapes in the given code.
     */
    private def doDetermineEscape(e: Entity, defSite: ValueOrigin, uses: IntSet,
                                  code: Array[Stmt[V]], m: Method): PropertyComputationResult = {
        var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]

        var worstProperty: EscapeProperty = NoEscape
        for (use ← uses) {
            determineEscapeStmt(code(use), e, defSite)
        }

        /**
         * Sets worstProperty to the minimum of its current value and the given one.
         */
        def setWorst(prop: EscapeProperty) = {
            worstProperty = worstProperty meet prop
        }

        /**
         * Determines whether the given statement leads to an escape of the entity e with definition
         * site defSite
         */
        def determineEscapeStmt(stmt: Stmt[V], e: Entity, defSite: Int): Unit = {

            stmt.astID match {
                case PutStatic.ASTID ⇒
                    val value = stmt.asPutStatic.value
                    if (usesDefSite(value)) setWorst(GlobalEscapeViaStaticFieldAssignment)
                // we are field insensitive, so we can not say what happens to that object
                case PutField.ASTID ⇒
                    val value = stmt.asPutField.value
                    if (usesDefSite(value)) setWorst(MaybeNoEscape)
                case ArrayStore.ASTID ⇒
                    val value = stmt.asArrayStore.value
                    if (usesDefSite(value)) setWorst(MaybeNoEscape)
                case Throw.ASTID ⇒
                    val value = stmt.asThrow.exception
                    // the exception could be catched, so we know nothing
                    if (usesDefSite(value)) setWorst(MaybeNoEscape)
                // we are inter-procedural
                case ReturnValue.ASTID ⇒
                    val value = stmt.asReturnValue.expr
                    if (usesDefSite(value)) setWorst(MaybeMethodEscape)
                case StaticMethodCall.ASTID ⇒
                    val StaticMethodCall(_, dc, isI, name, descr, params) = stmt
                    handleStaticCall(dc, isI, name, descr, params)
                //if (anyParameterUsesDefSite(params, e, defSite)) MaybeArgEscape else NoEscape
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(_, dc, isI, name, descr, receiver, params) = stmt
                    handleVirtualCall(dc, isI, name, descr, receiver, params)
                case NonVirtualMethodCall.ASTID ⇒
                    val NonVirtualMethodCall(_, dc, interface, name, descr, receiver, params) = stmt
                    handleNonVirtualCall(dc, interface, name, descr, receiver, params)
                case ExprStmt.ASTID ⇒
                    val expr = stmt.asExprStmt.expr
                    examineCall(e, defSite, expr)
                case Assignment.ASTID ⇒
                    val right = stmt.asAssignment.expr
                    examineCall(e, defSite, right)
                case _ ⇒
            }
        }

        /**
         * For a given entity with defSite, check whether the expression is a function call or a
         * CheckCast. For function call mark parameters and receiver objects that use the defSite as
         * GlobalEscape.
         */
        def examineCall(e: Entity, defSite: Int, expr: Expr[V]): Unit = {
            expr.astID match {
                case NonVirtualFunctionCall.ASTID ⇒
                    val NonVirtualFunctionCall(_, dc, interface, name, descr, receiver, params) = expr
                    handleNonVirtualCall(dc, interface, name, descr, receiver, params)
                case VirtualFunctionCall.ASTID ⇒
                    val VirtualFunctionCall(_, dc, isI, name, descr, receiver, params) = expr
                    handleVirtualCall(dc, isI, name, descr, receiver, params)
                case StaticFunctionCall.ASTID ⇒
                    val StaticFunctionCall(_, dc, isI, name, descr, params) = expr
                    handleStaticCall(dc, isI, name, descr, params)
                // see Java8LambdaExpressionsRewriting
                case Invokedynamic.ASTID ⇒
                    val params = expr.asInvokedynamic.params
                    if (anyParameterUsesDefSite(params)) setWorst(MaybeArgEscape)
                case _ ⇒
            }
        }

        /**
         * If the given expression is a [[UVar]] and is a use of the defSite, the entity e will be
         * marked as [[GlobalEscape]], otherwise [[None]] is returned
         */
        def usesDefSite(expr: Expr[V]): Boolean = {
            if (expr.isVar)
                if (expr.asVar.definedBy contains defSite) true
                else false
            else false
        }

        /**
         * If there exists a [[UVar]] in params that is a use of the defSite, e will be marked as
         * GlobalEscape, otherwise [[None]] is returned
         */
        def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
            if (params.exists { case UVar(_, defSites) ⇒ defSites contains defSite })
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
        def handleNonVirtualCall(dc: ReferenceType, interface: Boolean,
                                 name: String, descr: MethodDescriptor,
                                 receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
            val methodO = project.specialCall(dc.asObjectType, interface, name, descr)
            if (name == "<init>") {
                // the object constructor will not escape the this local
                if (dc ne ObjectType.Object) {
                    // this is safe as we assume a flat tac hierarchy
                    val UVar(_, defSites) = receiver
                    if (defSites.contains(defSite))
                        // resolve the constructor
                        methodO match {
                            case Success(m) ⇒
                                val fp = propertyStore.context[FormalParameters]
                                // check if the this local escapes in the callee
                                val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                                escapeState match {
                                    case EP(_, NoEscape)            ⇒
                                    case EP(_, state: GlobalEscape) ⇒ setWorst(state)
                                    case EP(_, ArgEscape)           ⇒ setWorst(ArgEscape)
                                    case EP(_, MaybeNoEscape)       ⇒ dependees += escapeState
                                    case EP(_, MaybeArgEscape)      ⇒ dependees += escapeState
                                    case EP(_, MaybeMethodEscape)   ⇒ dependees += escapeState
                                    case EP(_, x) ⇒
                                        throw new RuntimeException("not yet implemented "+x)
                                    // result not yet finished
                                    case epk ⇒ dependees += epk
                                }
                            case /* unknown method */ _ ⇒
                                setWorst(MaybeNoEscape)
                        }
                    checkParams(methodO, params)
                } else /* Object constructor does escape by def. */ NoEscape
            } else {
                checkParams(methodO, params)
                if (usesDefSite(receiver))
                    handleCallO(methodO, 0)
            }
        }

        def handleStaticCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, params: Seq[Expr[V]]) = {
            checkParams(project.staticCall(dc.asObjectType, isI, name, descr), params)
        }

        def handleVirtualCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
            if (receiver.isVar) {
                val value = receiver.asVar.value.asDomainReferenceValue
                if (value.isPrecise) {
                    value.valueType match {
                        case Some(valueType) ⇒
                            val methodO = project.instanceCall(m.classFile.thisType, valueType, name, descr)
                            checkParams(methodO, params)
                            if (usesDefSite(receiver)) handleCallO(methodO, 0)
                            return ;
                        case None ⇒ throw new NullPointerException()
                    }
                }
            }
            val packageName = m.classFile.thisType.packageName
            val methods =
                if (isI) project.interfaceCall(dc.asObjectType, name, descr)
                else project.virtualCall(packageName, dc, name, descr)
            for (method ← methods) {
                checkParams(Success(method), params)
                if (usesDefSite(receiver)) handleCall(method, 0)
            }

        }

        def checkParams(methodO: org.opalj.Result[Method], params: Seq[Expr[V]]) = {
            for (i ← params.indices) {
                if (usesDefSite(params(i)))
                    handleCallO(methodO, i + 1)
            }
        }

        def handleCallO(methodO: org.opalj.Result[Method], param: Int) = {
            methodO match {
                case Success(m) ⇒ handleCall(m, param)
                case _          ⇒ setWorst(MaybeArgEscape)
            }
        }

        def handleCall(m: Method, param: Int) = {
            val fp = propertyStore.context[FormalParameters]

            try {
                val escapeState = propertyStore(fp(m)(param), EscapeProperty.key)
                escapeState match {
                    case EP(_, NoEscape)            ⇒ setWorst(ArgEscape)
                    case EP(_, ArgEscape)           ⇒ setWorst(ArgEscape)
                    case EP(_, state: GlobalEscape) ⇒ setWorst(state)
                    case EP(_, MaybeNoEscape) ⇒
                        dependees += escapeState
                        setWorst(ArgEscape)
                    case EP(_, MaybeArgEscape) ⇒
                        dependees += escapeState
                        setWorst(ArgEscape)
                    case EP(_, MaybeMethodEscape) ⇒
                        dependees += escapeState
                        setWorst(ArgEscape)
                    case EP(_, _) ⇒
                        throw new RuntimeException("not yet implemented")
                    // result not yet finished
                    case epk ⇒
                        dependees += epk
                        setWorst(ArgEscape)
                }
            } catch {
                case _: ArrayIndexOutOfBoundsException ⇒
                    //TODO params to array
                    setWorst(MaybeArgEscape)
            }
        }

        def meetAndFilter(other: Entity, p: EscapeProperty) = {
            setWorst(p)
            dependees = dependees filter (_.e ne other)
            if (dependees.isEmpty)
                Result(e, worstProperty)
            else
                IntermediateResult(e, MaybeNoEscape meet worstProperty, dependees, c)
        }

        // Every entity that is not identified as escaping is not escaping
        if (dependees.isEmpty || worstProperty.isInstanceOf[GlobalEscape])
            return Result(e, worstProperty)

        // we depend on the result for other entities, lets construct a continuation
        def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
            other match {
                // constructor
                case FormalParameter(m, -1) if m.name == "<init>" ⇒ p match {
                    case state: GlobalEscape ⇒ Result(e, state)
                    //TODO cases
                    case MaybeNoEscape ⇒
                        u match {
                            case IntermediateUpdate ⇒
                                val newEP = EP(other, MaybeNoEscape)
                                dependees = dependees.filter(_.e ne other) + newEP
                                IntermediateResult(e, MaybeNoEscape, dependees, c)
                            case _ ⇒ meetAndFilter(other, MaybeNoEscape)
                        }
                    case MaybeArgEscape ⇒
                        u match {
                            case IntermediateUpdate ⇒
                                val newEP = EP(other, MaybeArgEscape)
                                dependees = dependees.filter(_.e ne other) + newEP
                                IntermediateResult(e, MaybeArgEscape, dependees, c)
                            case _ ⇒ meetAndFilter(other, MaybeArgEscape)
                        }
                    case MaybeMethodEscape ⇒ u match {
                        case IntermediateUpdate ⇒
                            val newEP = EP(other, MaybeMethodEscape)
                            dependees = dependees.filter(_.e ne other) + newEP
                            IntermediateResult(e, MaybeNoEscape, dependees, c)
                        case _ ⇒ meetAndFilter(other, MaybeNoEscape)
                    }
                    case NoEscape  ⇒ meetAndFilter(other, NoEscape)
                    case ArgEscape ⇒ meetAndFilter(other, ArgEscape)
                }
                case FormalParameter(_, _) ⇒ p match {
                    case state: GlobalEscape ⇒ Result(e, state)
                    case NoEscape            ⇒ meetAndFilter(other, ArgEscape)
                    case ArgEscape           ⇒ meetAndFilter(other, ArgEscape)
                    case MaybeNoEscape ⇒ u match {
                        case IntermediateUpdate ⇒
                            val newEP = EP(other, MaybeNoEscape)
                            dependees = dependees.filter(_.e ne other) + newEP
                            IntermediateResult(e, MaybeArgEscape meet worstProperty, dependees, c)
                        case _ ⇒ meetAndFilter(other, MaybeArgEscape)
                    }
                    case MaybeArgEscape ⇒ u match {
                        case IntermediateUpdate ⇒
                            val newEP = EP(other, MaybeArgEscape)
                            dependees = dependees.filter(_.e ne other) + newEP
                            IntermediateResult(e, MaybeArgEscape meet worstProperty, dependees, c)
                        case _ ⇒ meetAndFilter(other, MaybeArgEscape)
                    }
                    case MaybeMethodEscape ⇒ u match {
                        case IntermediateUpdate ⇒
                            val newEP = EP(other, MaybeMethodEscape)
                            dependees = dependees.filter(_.e ne other) + newEP
                            IntermediateResult(e, MaybeArgEscape meet worstProperty, dependees, c)
                        case _ ⇒ meetAndFilter(other, MaybeArgEscape)
                    }
                }
            }

        }

        IntermediateResult(e, MaybeNoEscape meet worstProperty, dependees, c)
    }

    /**
     * Determine whether the given entity ([[AllocationSite]] or [[FormalParameter]]) escapes
     * its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            //TODO: AllocationType
            case as @ ObjectAllocationSite(m, pc) ⇒
                val TACode(_, code, _, _, _) = project.get(DefaultTACAIKey)(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _)) ⇒
                            doDetermineEscape(as, index, uses, code, m)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ Result(e, NoEscape)
            case as @ ArrayAllocationSite(m, pc) ⇒
                val TACode(_, code, _, _, _) = project.get(DefaultTACAIKey)(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code, m)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ Result(e, NoEscape)
            case FormalParameter(m, _) if m.body.isEmpty ⇒ Result(e, MaybeNoEscape)
            case FormalParameter(m, i) ⇒
                val TACode(params, code, _, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.parameter(i)
                doDetermineEscape(e, param.origin, param.useSites, code, m)
        }
    }
}

object InterproceduralEscapeAnalysis extends FPCFAnalysisRunner {
    type V = DUVar[Domain#DomainValue]

    def entitySelector: PartialFunction[Entity, Entity] = {
        case as: AllocationSite  ⇒ as
        case fp: FormalParameter ⇒ fp
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new InterproceduralEscapeAnalysis(project)
        propertyStore.scheduleForCollected(entitySelector)(analysis.determineEscape)
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

        SimpleAIKey.domainFactory = (p, m) ⇒ new PrimitiveTACAIDomain(p, m)
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
            analysesManager.run(InterproceduralEscapeAnalysis)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val propertyStore = project.get(PropertyStoreKey)
        val staticEscapes =
            propertyStore.entities(GlobalEscapeViaStaticFieldAssignment)
        val maybeNoEscape =
            propertyStore.entities(MaybeNoEscape)
        val maybeArgEscape =
            propertyStore.entities(MaybeArgEscape)
        val maybeMethodEscape =
            propertyStore.entities(MaybeMethodEscape)
        val argEscapes = propertyStore.entities(ArgEscape)
        val noEscape = propertyStore.entities(NoEscape)

        println("ALLOCATION SITES:")
        println(s"# of global escaping objects: ${sizeAsAS(staticEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsAS(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsAS(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsAS(maybeMethodEscape)}")
        println(s"# of arg escaping objects: ${sizeAsAS(argEscapes)}")
        println(s"# of local objects: ${sizeAsAS(noEscape)}")

        println("FORMAL PARAMETERS:")
        println(s"# of global escaping objects: ${sizeAsFP(staticEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsFP(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsFP(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsFP(maybeMethodEscape)}")
        println(s"# of arg escaping objects: ${sizeAsFP(argEscapes)}")
        println(s"# of local objects: ${sizeAsFP(noEscape)}")

        def sizeAsAS(entities: Traversable[Entity]) = entities.collect { case x: AllocationSite ⇒ x }.size
        def sizeAsFP(entities: Traversable[Entity]) = entities.collect { case x: FormalParameter ⇒ x }.size
    }
}