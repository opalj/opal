/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import java.net.URL

import com.typesafe.config.ConfigFactory
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.instructions.NEW
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.log.GlobalLogContext

// TODO A.B. remove later
object TestRunner {
    def main(args: Array[String]): Unit = {
        val testJar = "C:/Users/Andreas/Dropbox/Masterarbeit/sample-java-library/out/artifacts/sample_java_library_jar/sample-java-library.jar"
        implicit val project: Project[URL] = Project(new java.io.File(testJar), GlobalLogContext, ConfigFactory.load())

        val manager = project.get(FPCFAnalysesManagerKey)
        val ps = project.get(PropertyStoreKey)

        manager.runAll(List(ConstructorCallInstantiatedTypesAnalysisScheduler))

        ps.waitOnPhaseCompletion()

        val epss = ps.entities(InstantiatedTypes.key)
        for (eps ← epss) {
            println(eps.e)
            println(eps.ub.types.map(_.toJVMTypeName).mkString(", "))
        }
    }
}

/**
 * Updates InstantiatedTypes attached to a method for each constructor
 * call occurring within that method.
 *
 * This is a simple analysis which yields useful results for basic tests,
 * but it does not capture, e.g., indirect constructor calls through reflection.
 *
 * @author Andreas Bauer
 */
// TODO A.B. replace later with a more sophisticated analysis (based on the RTA one)
class ConstructorCallInstantiatedTypesAnalysis( final val project: SomeProject) extends FPCFAnalysis {

    def processMethod(definedMethod: DefinedMethod): PropertyComputationResult = {
        val code = definedMethod.definedMethod.body.get

        val instantiatedTypes = code.instructions.flatMap({
            case NEW(declType) ⇒ Some(declType)
            case _ ⇒ None
        })

        if (instantiatedTypes.isEmpty) {
            org.opalj.fpcf.NoResult
        } else {
            PartialResult(
                definedMethod,
                InstantiatedTypes.key,
                update(definedMethod, UIDSet(instantiatedTypes.toSeq: _*))
            )
        }
    }

    def update(
        method:               DefinedMethod,
        newInstantiatedTypes: UIDSet[ObjectType]
    )(
        eop: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Option[EPS[DefinedMethod, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(method, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(method, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }
}

object ConstructorCallInstantiatedTypesAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new ConstructorCallInstantiatedTypesAnalysis(p)
        val declaredMethods = p.get(DeclaredMethodsKey)
        val allMethods = p.allMethodsWithBody.map(declaredMethods.apply)
        ps.scheduleEagerComputationsForEntities(allMethods)(analysis.processMethod)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set.empty

    override def derivesEagerly: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
