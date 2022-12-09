/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.cg

import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.{BasicFPCFLazyAnalysisScheduler, FPCFAnalysis, FPCFAnalysisScheduler}
import org.opalj.fpcf._
import org.opalj.ll.fpcf.analyses.ifds.{JNICallUtil, LLVMFunction, NativeFunction}
import org.opalj.ll.llvm.value.{Call, Function}


// maps function a ==> Map(x -> a_call1, y -> a_call2, ...)

sealed trait SimpleCallGraphPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = SimpleNativeCallGraph
}

case class SimpleNativeCallGraph(cg: Map[NativeFunction, Set[Call]]) extends SimpleCallGraphPropertyMetaInformation with Property {
    final override def key: PropertyKey[SimpleNativeCallGraph] = SimpleNativeCallGraph.key

    def apply(function: NativeFunction): Option[Set[Call]] = cg.get(function)
}

object SimpleNativeCallGraph extends SimpleCallGraphPropertyMetaInformation {
    final val key: PropertyKey[SimpleNativeCallGraph] = PropertyKey.create(
        "SimpleNativeCallGraph",
        new SimpleNativeCallGraph(Map.empty)
    )
}

class SimpleCallGraphAnalysis(val project: SomeProject) extends FPCFAnalysis {
    type CallGraph = Map[NativeFunction, Set[Call]]
    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def lazilyAnalyze(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case entryPoints: Set[Function @unchecked] => analyze(entryPoints)
            case _ => throw new IllegalArgumentException("Simple Call Graph Analysis can only process Sets of Functions!")
        }
    }

    private def analyze(entryPoints: Set[Function]): ProperPropertyComputationResult = {
        if (entryPoints.isEmpty) Result(entryPoints, SimpleNativeCallGraph(Map.empty))
        else if (entryPoints.size == 1) Result(entryPoints, SimpleNativeCallGraph(processFunction(entryPoints.head)))
        else Result(entryPoints, SimpleNativeCallGraph(processMultipleEntries(entryPoints)))
    }

    private def processFunction(function: Function): CallGraph = {
        val cg = scala.collection.mutable.Map.empty[NativeFunction, Set[Call]]
        val entryPoints = scala.collection.mutable.Set.empty[Function]

        // process calls in this function
        function.basicBlocks.flatMap(_.instructions).foreach {
            case c: Call => c.calledValue match {
                case f: Function =>
                    entryPoints.add(f)
                    val newCalls = cg.getOrElse(LLVMFunction(f), Set.empty) ++ Set(c)
                    cg.update(LLVMFunction(f), newCalls)
                case _ if JNICallUtil.isJNICall(c) =>
                    val jniCallees = JNICallUtil.resolve(c)
                    for (jniCallee <- jniCallees) {
                        val newCalls = cg.getOrElse(jniCallee, Set.empty) ++ Set(c)
                        cg.update(jniCallee, newCalls)
                    }
                case _ =>
            }
            case _ =>
        }

        // process called functions
        val subCg = processMultipleEntries(entryPoints.toSet)

        mergeCallGraphs(cg.toMap, subCg)
    }

    private def processMultipleEntries(entryPoints: Set[Function]): CallGraph = {
        // if multiple entry points, get individual call graphs from property store and merge them
        val cgs = entryPoints
            .map(e => propertyStore(EPK(Set(e), SimpleNativeCallGraph.key)))
            .map {
                case ep: FinalEP[Set[Function], SimpleNativeCallGraph] => ep.p.cg
                case _ => throw new RuntimeException("unexpected error while computing SimpleNativeCallGraph")
            }
        cgs.fold(Map.empty)(mergeCallGraphs)
    }

    private def mergeCallGraphs(cg1: CallGraph, cg2: CallGraph): CallGraph = {
        (cg1.toSeq ++ cg2.toSeq).groupBy(_._1).map {
            case (key, value) => (key, value.flatMap(_._2).toSet)
        }
    }
}

trait SimpleCallGraphAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(SimpleNativeCallGraph)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(SimpleNativeCallGraph))
}

object LazySimpleCallGraphAnalysis extends SimpleCallGraphAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new SimpleCallGraphAnalysis(project)
        propertyStore.registerLazyPropertyComputation(SimpleNativeCallGraph.key, analysis.lazilyAnalyze)
        analysis
    }
}