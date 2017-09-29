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
package escape

import org.opalj.log.GlobalLogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.collection.immutable.IntArraySet

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.br.AllocationSite
import org.opalj.br.Method
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties._
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.DVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter

/**
 * A very simple flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterproceduralEscapeAnalysis private (
    final val project: SomeProject
) extends AbstractEscapeAnalysis {

    private[this] tacai = project.get(DefaultTACAIKey)

    override def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntArraySet,
        code:    Array[Stmt[V]],
        params:  Parameters[TACMethodParameter],
        m:       Method
    ): AbstractEntityEscapeAnalysis =
        new InterproceduralEntityEscapeAnalysis(e, defSite, uses, code, params, m, propertyStore, project)

    /**
     * Determine whether the given entity ([[AllocationSite]] or [[FormalParameter]]) escapes
     * its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(params, code, _, _, _) = tacai(m)

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
            case FormalParameter(m, i) ⇒
                val TACode(params, code, _, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.parameter(i)
                doDetermineEscape(e, param.origin, param.useSites, code, params, m)
        }
    }
}

object InterproceduralEscapeAnalysis extends FPCFAnalysisRunner {

    type V = DUVar[Domain#DomainValue]

    def entitySelector(propertyStore: PropertyStore): PartialFunction[Entity, Entity] = {
        case as: AllocationSite /*if !propertyStore(as, EscapeProperty.key).isPropertyFinal */  ⇒ as
        case fp: FormalParameter /*if !propertyStore(fp, EscapeProperty.key).isPropertyFinal */ ⇒ fp
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        //val analysesManager = project.get(FPCFAnalysesManagerKey)
        //analysesManager.run(SimpleEscapeAnalysis)
        val analysis = new InterproceduralEscapeAnalysis(project)
        propertyStore.scheduleForCollected(entitySelector(propertyStore))(analysis.determineEscape)
        analysis
    }

    def main(args: Array[String]): Unit = {
        val opaConfig = AnalysisModeConfigFactory.createConfig(AnalysisModes.OPA)
        val project = Project(
            org.opalj.bytecode.JRELibraryFolder,
            GlobalLogContext,
            opaConfig.withFallback(Project.GlobalConfig)
        )

        //SimpleAIKey.domainFactory = (p, m) ⇒ new PrimitiveTACAIDomain(p, m)
        time {
            val tacai = project.get(DefaultTACAIKey)
            project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
        } { t ⇒ println(s"computing the 3-address code took ${t.toSeconds}") }

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val propertyStore = project.get(PropertyStoreKey)
        //propertyStore.debug = true
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        time {
            analysesManager.run(InterproceduralEscapeAnalysis)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        val staticEscapes =
            propertyStore.entities(GlobalEscapeViaStaticFieldAssignment)
        val heapEscapes =
            propertyStore.entities(GlobalEscapeViaHeapObjectAssignment)
        val maybeNoEscape =
            propertyStore.entities(MaybeNoEscape)
        val maybeArgEscape =
            propertyStore.entities(MaybeArgEscape)
        val maybeMethodEscape =
            propertyStore.entities(MaybeMethodEscape)
        val argEscapes = propertyStore.entities(ArgEscape)
        val returnEscapes = propertyStore.entities(MethodEscapeViaReturn)
        val returnAssignmentEscapes = propertyStore.entities(MethodEscapeViaReturnAssignment)
        val parameterEscapes = propertyStore.entities(MethodEscapeViaParameterAssignment)
        val noEscape = propertyStore.entities(NoEscape)

        println("ALLOCATION SITES:")
        println(s"# of local objects: ${sizeAsAS(noEscape)}")
        println(s"# of arg escaping objects: ${sizeAsAS(argEscapes)}")
        println(s"# of method escaping objects via return: ${sizeAsAS(returnEscapes)}")
        println(s"# of method escaping objects via return assignment: ${sizeAsAS(returnAssignmentEscapes)}")
        println(s"# of method escaping objects via parameter assignment: ${sizeAsAS(parameterEscapes)}")
        println(s"# of global escaping objects: ${sizeAsAS(staticEscapes)}")
        println(s"# of indirect global escaping objects: ${sizeAsAS(heapEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsAS(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsAS(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsAS(maybeMethodEscape)}")

        println("FORMAL PARAMETERS")
        println(s"# of local objects: ${sizeAsFP(noEscape)}")
        println(s"# of arg escaping objects: ${sizeAsFP(argEscapes)}")
        println(s"# of method escaping objects via return: ${sizeAsFP(returnEscapes)}")
        println(s"# of method escaping objects via return assignment: ${sizeAsFP(returnAssignmentEscapes)}")
        println(s"# of method escaping objects via parameter assignment: ${sizeAsFP(parameterEscapes)}")
        println(s"# of global escaping objects: ${sizeAsFP(staticEscapes)}")
        println(s"# of indirect global escaping objects: ${sizeAsFP(heapEscapes)}")
        println(s"# of maybe no escaping objects: ${sizeAsFP(maybeNoEscape)}")
        println(s"# of maybe arg escaping objects: ${sizeAsFP(maybeArgEscape)}")
        println(s"# of maybe method escaping objects: ${sizeAsFP(maybeMethodEscape)}")

        def sizeAsAS(entities: Traversable[Entity]) = entities.collect { case x: AllocationSite ⇒ x }.size
        def sizeAsFP(entities: Traversable[Entity]) = entities.collect { case x: FormalParameter ⇒ x }.size
    }
}
