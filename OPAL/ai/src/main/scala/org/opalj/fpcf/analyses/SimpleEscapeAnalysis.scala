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
package analyses
import java.io.File

import org.opalj.ai.ValueOrigin
import org.opalj.ai.Domain
import org.opalj.tac.TACMethodParameter
import org.opalj.br.Method
import org.opalj.br.AllocationSite
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.Project
import org.opalj.tac.Stmt
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturnAssignment
import org.opalj.fpcf.properties.GlobalEscapeViaStaticFieldAssignment
import org.opalj.fpcf.properties.GlobalEscapeViaHeapObjectAssignment
import org.opalj.fpcf.properties.MethodEscapeViaReturn
import org.opalj.fpcf.properties.ArgEscape
import org.opalj.fpcf.properties.MethodEscapeViaParameterAssignment
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MaybeMethodEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.tac.Parameters
import org.opalj.tac.DVar
import org.opalj.tac.TACode
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.DUVar
import org.opalj.util.PerformanceEvaluation.time
class SimpleEscapeAnalysis(final val project: SomeProject) extends AbstractEscapeAnalysis {
    override def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntArraySet,
        code:    Array[Stmt[V]],
        params:  Parameters[TACMethodParameter],
        m:       Method
    ): AbstractEntityEscapeAnalysis =
        new SimpleEntityEscapeAnalysis(e, defSite, uses, code, params, m, propertyStore, project)

    override def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(params, code, _, _, _) = project.get(DefaultTACAIKey)(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code, params, m)
                        case ExprStmt(`pc`, NewArray(`pc`, _, _)) ⇒
                            ImmediateResult(e, NoEscape)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ ImmediateResult(e, NoEscape)
            case FormalParameter(m, _) if m.body.isEmpty ⇒ Result(e, MaybeNoEscape)
            case FormalParameter(m, -1) if m.name == "<init>" ⇒
                val TACode(params, code, _, _, _) = project.get(DefaultTACAIKey)(m)
                val thisParam = params.thisParameter
                doDetermineEscape(e, thisParam.origin, thisParam.useSites, code, params, m)
            case fp: FormalParameter ⇒ ImmediateResult(fp, MaybeNoEscape)
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

        //SimpleAIKey.domainFactory = (p, m) ⇒ new PrimitiveTACAIDomain(p, m)
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
            analysesManager.run(SimpleEscapeAnalysis)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val propertyStore = project.get(PropertyStoreKey)
        val staticEscapes =
            propertyStore.entities(GlobalEscapeViaStaticFieldAssignment)
        val heapEscapes = propertyStore.entities(GlobalEscapeViaHeapObjectAssignment)
        val maybeNoEscape =
            propertyStore.entities(MaybeNoEscape)
        val maybeArgEscape =
            propertyStore.entities(MaybeArgEscape)
        val maybeMethodEscape =
            propertyStore.entities(MaybeMethodEscape)
        val argEscapes = propertyStore.entities(ArgEscape)
        val noEscape = propertyStore.entities(NoEscape)
        val methodReturnEscapes = propertyStore.entities(MethodEscapeViaReturn)
        val methodParameterEscapes = propertyStore.entities(MethodEscapeViaParameterAssignment)
        val methodReturnFieldEscapes = propertyStore.entities(MethodEscapeViaReturnAssignment)

        println("ALLOCATION SITES:")
        println(s"# of maybe no escaping objects: ${sizeAsAS(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsAS(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsAS(maybeMethodEscape)}")
        println(s"# of local objects: ${sizeAsAS(noEscape)}")
        println(s"# of arg escaping objects: ${sizeAsAS(argEscapes)}")
        println(s"# of method escaping objects via return : ${sizeAsAS(methodReturnEscapes)}")
        println(s"# of method escaping objects via parameter: ${sizeAsAS(methodParameterEscapes)}")
        println(s"# of method escaping objects via return assignment: ${sizeAsAS(methodReturnFieldEscapes)}")
        println(s"# of direct global escaping objects: ${sizeAsAS(staticEscapes)}")
        println(s"# of indirect global escaping objects: ${sizeAsAS(heapEscapes)}")

        println("FORMAL PARAMETERS:")
        println(s"# of maybe no escaping objects: ${sizeAsFP(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsFP(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsFP(maybeMethodEscape)}")
        println(s"# of local objects: ${sizeAsFP(noEscape)}")
        println(s"# of arg escaping objects: ${sizeAsFP(argEscapes)}")
        println(s"# of method escaping objects via return : ${sizeAsFP(methodReturnEscapes)}")
        println(s"# of method escaping objects via parameter: ${sizeAsFP(methodParameterEscapes)}")
        println(s"# of method escaping objects via return assignment: ${sizeAsFP(methodReturnFieldEscapes)}")
        println(s"# of direct global escaping objects: ${sizeAsFP(staticEscapes)}")
        println(s"# of indirect global escaping objects: ${sizeAsFP(heapEscapes)}")

        def sizeAsAS(entities: Traversable[Entity]) = entities.collect { case x: AllocationSite ⇒ x }.size
        def sizeAsFP(entities: Traversable[Entity]) = entities.collect { case x: FormalParameter ⇒ x }.size
    }
}
