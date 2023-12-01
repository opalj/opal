/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.br.{DefinedMethod, ObjectType}
import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication, VirtualFormalParameter}
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.{FPCFAnalysesManagerKey, FPCFAnalysisScheduler, PropertyStoreKey}
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.fpcf.{PropertyStore, PropertyStoreContext, SomeEPS}
import org.opalj.log.LogContext
import org.opalj.tac.cg.{AllocationSiteBasedPointsToCallGraphKey, TypeIteratorKey}
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.xta.{TypePropagationAnalysisScheduler, XTASetEntitySelector}
import org.opalj.tac.fpcf.analyses.pointsto.longToAllocationSite
import org.opalj.xl.AllocationSiteBasedTriggeredTajsConnectorScheduler
import org.opalj.xl.javaanalyses.detector.scriptengine.AllocationSiteBasedScriptEngineDetectorScheduler

import java.net.URL
import scala.collection.mutable
import scala.io.Source
import scala.util.matching.Regex

class Method(val methodId: Int, val signature: String) {
  val pcToInstances: mutable.Map[Int, Set[Instance]] = mutable.Map.empty[Int, Set[Instance]].withDefaultValue(Set.empty[Instance])
  val paramToInstances: mutable.Map[Int, Set[Instance]] = mutable.Map.empty[Int, Set[Instance]].withDefaultValue(Set.empty[Instance])
}
class Allocation(classType: String)
case class ActualAllocation(method: Method, pc: Int, classtype: String) extends Allocation(classType = classtype) {
  override def toString = s"Allocation(m ${method.methodId} pc ${pc})"
}
case class DummyAllocation(method: Method, pc: Int, classtype: String) extends Allocation(classType = classtype) {
  override def toString = s"NoAllocation( m ${method.methodId} pc ${pc})"
}
class Instance(val instanceId: Int, val objClass: String, val allocation: Allocation) {
  override def toString: String = {
  s"Instance($instanceId $objClass $allocation)"
  }
}
case class GroundTruthLogEvent(instanceClass: String, instanceId: Int, methodSignature: String, pc: Int)
case class DefsiteData(objClass: String, allocSiteId: Long, allocSitePC: Int, defsiteMethodSignature: String, defsitePC: Int)

class Dataset {
  val methods: mutable.Map[Int, Method] = mutable.Map.empty[Int, Method]
  // method Id + program counter
  val allocations = mutable.Map[(String, Int),Allocation]()
  val instanceIdToAllocation = mutable.Map[Int, Allocation]()
  def addMethod(methodId: Int, method: Method): Unit = {
    methods(methodId) = method
  }

  def addInstance(methodId: Int, pc: Int, instance: Instance): Unit = {
      val method = methods(methodId)
      method.pcToInstances(pc) = method.pcToInstances(pc) + instance

  }
  def addParamIntance(methodId: Int, param: Int, instance: Instance): Unit = {
      val method = methods(methodId)
      method.paramToInstances(param) = method.paramToInstances(param) + instance

  }
}
case class TruePositive(defsiteMethod: String, defsitePC: Int, instances: Set[Instance], allocSiteID: Long, allocsitePC: Int, allocsiteType: String) {
  override def toString: String =
    s"    TruePositive(defsitePC=$defsitePC, instances=${instances.mkString(" ")}, " +
      s"allocSiteID=$allocSiteID, allocsitePC=$allocsitePC, allocsiteType=$allocsiteType)"
}

case class FalsePositive(defsiteMethod: String, defsitePC: Int, allocSiteType: String, allocSiteID: Long, allocsitePC: Int) {
  override def toString: String =
    s"    FalsePositive(defsitePC=$defsitePC, allocSiteType=$allocSiteType, " +
      s"allocSiteID=$allocSiteID, allocsitePC=$allocsitePC)"
}

case class FalseNegative(defsiteMethod: String, defsitePC: Int, instances: Set[Instance]) {
  override def toString: String =
    s"    FalseNegative(defsitePC=$defsitePC, instances=${instances.mkString(" ")}"
}


case class MethodResult(
                         method: String,
                         precision: Double,
                         recall: Double,
                         truePositive: Int,
                         falsePositive: Int,
                         falseNegative: Int,
                         truePositiveData: Set[TruePositive],
                         falsePositiveData: Set[FalsePositive],
                         falseNegativeData: Set[FalseNegative],
                         allocSiteToInstanceIdsForMethod : mutable.Map[Long, Set[Instance]],
                         instanceIdsToAllocSiteForMethod : mutable.Map[Set[Instance], Option[Long]]
                       ) {
  override def toString: String = {

    val truePositiveDataStr = truePositiveData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")
    val falsePositiveDataStr = falsePositiveData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")
    val falseNegativeDataStr = falseNegativeData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")

    val allocSiteToInstanceIdForMethodStr = allocSiteToInstanceIdsForMethod.toList.sortBy(_._1).mkString(" ")
    val instanceIdToAllocSiteForMethodStr = instanceIdsToAllocSiteForMethod.toList.sortBy(_._2).mkString(" ")
    val p = precision * 100
    val r = recall * 100
    f"""MethodResult: $method
       |  Precision: $p%.2f%%
       |  Recall: $r%.2f%%
       |  True positives: $truePositive
       |$truePositiveDataStr
       |  False positives: $falsePositive
       |$falsePositiveDataStr
       |  False negatives: $falseNegative
       |$falseNegativeDataStr
       | Alloc site ID to instance ID: $allocSiteToInstanceIdForMethodStr
       | Instance ID to Alloc site ID: $instanceIdToAllocSiteForMethodStr
       | """.stripMargin
  }
}

object GroundTruthParser {

  def parseGroundTruth(groundTruthPath: String): Dataset = {
    val dataset = new Dataset()

    val methodPattern = new Regex("<method fullyqualified=\"(.+)\" id=\"(\\d+)\"/>")
    val instancePattern = new Regex("<(allocation|traceevent) methodId=\"(\\d+)\" (param|pc)=\"([\\d-]+)\" instanceId=\"(\\d+)\" class=\"([^\"]+)\"/>")

    for (line <- Source.fromFile(groundTruthPath).getLines()) {
      methodPattern.findFirstMatchIn(line) match {
        case Some(m) =>
          val signature = m.group(1)
          val methodId = m.group(2).toInt
          dataset.addMethod(methodId, new Method(methodId, signature))
        case _ =>
      }

      instancePattern.findFirstMatchIn(line) match {
        case Some(m) =>
          val eventtype = m.group(1)
          val methodId = m.group(2).toInt
          val pcOrParamType = m.group(3)
          val pcOrParam = m.group(4).toInt
          val instanceId = m.group(5).toInt
          val objClass = m.group(6)

          dataset.methods.get(methodId) match {
            case Some(method) => {

              if (eventtype equals "allocation") {
                val allocation = ActualAllocation(method, pcOrParam, objClass)
                dataset.allocations.getOrElseUpdate((method.signature, pcOrParam), allocation)
                dataset.instanceIdToAllocation.put(instanceId, allocation)
              }

              val allocSite = dataset.instanceIdToAllocation.getOrElseUpdate(instanceId, {
                val dummyAllocation = dataset.allocations.getOrElseUpdate((method.signature, pcOrParam), DummyAllocation(method, pcOrParam, objClass))
                dummyAllocation
              })
              val instance = new Instance(instanceId, objClass, allocSite)
              if (pcOrParamType equals "pc")
                dataset.addInstance(methodId, pcOrParam, instance)
              else
                dataset.addParamIntance(methodId, pcOrParam, instance)
            }
            case _ =>
          }

        case _ =>
      }
    }

    dataset
  }


  def normalizeType(str: String): String = {
    val equivalences = Map(
      ("jdk.nashorn.api.scripting.NashornScriptEngine", "javax.script.ScriptEngine"),
      ("org.openjdk.nashorn.api.scripting.NashornScriptEngine", "javax.script.ScriptEngine"),
      ("jdk.nashorn.api.scripting.ScriptObjectMirror", "java.lang.Object"),
      ("org.openjdk.nashorn.api.scripting.ScriptObjectMirror", "java.lang.Object"),
    )
    equivalences.getOrElse(str, str)
  }

  def ignoreTruePositive(truePositive: TruePositive): Boolean = {
    //truePositive.allocsitePC < 0 ||
      truePositive.defsiteMethod.contains("$string_concat$")
  }
  def ignoreFalseNegative(falseNegative: FalseNegative) : Boolean = {
    falseNegative.instances.exists(_.objClass.equals("java.io.PrintStream")) ||
      falseNegative.defsitePC == -2 && falseNegative.defsiteMethod.contains("main(java.lang.String[])")
    // false //falseNegative.instances.exists(_.objClass.equals("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
  }
  def ignoreFalsePositive(falsePositive: FalsePositive) : Boolean = {
    falsePositive.allocsitePC < 0 ||
      falsePositive.defsiteMethod.contains("$string_concat$")
  }
  def generateMapping(groundTruth: Set[GroundTruthLogEvent], test: Set[DefsiteData]): (Map[Long, Set[Int]], Map[Int, Option[Long]]) = {
    val allocSiteToInstanceId = mutable.Map[Long, Set[Int]]()
    val instanceIdToAllocSite = mutable.Map[Int, Option[Long]]()
    val groundTruthDict = groundTruth.map(event => ((normalizeType(event.instanceClass), event.methodSignature, event.pc), event.instanceId)).toMap
    for (defsiteData <- test) {
      val normalized = normalizeType(defsiteData.objClass)
      groundTruthDict.get((normalized, defsiteData.defsiteMethodSignature, defsiteData.defsitePC)) match {
        case Some(instanceId) => {
          allocSiteToInstanceId.put(defsiteData.allocSiteId, allocSiteToInstanceId.getOrElse(defsiteData.allocSiteId, Set.empty[Int]) + instanceId)
          instanceIdToAllocSite.put(instanceId, Option(defsiteData.allocSiteId))
        }
        case None => {}
      }
    }
    for (event <- groundTruth) {
      if (!groundTruthDict.contains((normalizeType(event.instanceClass), event.methodSignature, event.pc))) {
        instanceIdToAllocSite.put(event.instanceId, Option.empty)
      }
    }
    (allocSiteToInstanceId.toMap, instanceIdToAllocSite.toMap)

  }

  def evaluate(groundTruth: Dataset, test: Set[(DefinedMethod, Int, Option[AllocationSitePointsToSet])], project: Project[URL]) : Map[Int, MethodResult]= {
    val groupedWithoutByMethod = test.groupBy(_._1.definedMethod.fullyQualifiedSignature)
    val groundTruthData = mutable.Set[GroundTruthLogEvent]()
    val signatureToMethodId = mutable.Map[String, Int]()
    for ((methodId, method) <- groundTruth.methods) {
      signatureToMethodId.put(method.signature, methodId)
      for ((pc, instances) <- method.pcToInstances) {
        for (instance <- instances) {
          groundTruthData add GroundTruthLogEvent(instance.objClass, instance.instanceId, method.signature, pc)
        }
      }
      for ((param, instances) <- method.paramToInstances) {
        for (instance <- instances) {
          // 'this' is -1 in ground truth, -1 in test
          // param 0 is 0 in ground truth, -2 in test
          groundTruthData add GroundTruthLogEvent(instance.objClass, instance.instanceId, method.signature, -2 -param)
        }
      }

    }
    val testData = mutable.Set[DefsiteData]()
    for ((methodSignature, defSite) <- groupedWithoutByMethod) {
      implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
      val sorted = defSite.toArray.sortBy(mdsas => mdsas._2)
      for (pts <- sorted) {
        pts._3 match {
          case Some(as) => for (allocSiteId <- as.elements.iterator) {
            val (ctx, allocSitePC, typeId) = longToAllocationSite(allocSiteId)
            val objClass = ObjectType.lookup(typeId).toJava
            testData add DefsiteData(objClass, allocSiteId, allocSitePC, methodSignature, pts._2)
            println("<pointsto method=\"" + methodSignature + "\" pc=\"" + pts._2 + "\" type=\"" + objClass + "\" allocSiteId=\"" + allocSiteId + "\" allocSitePc=\"" + allocSitePC + "\" />")
          }
          case None =>
        }
      }
    }
    val (allocSiteToInstanceIds, instanceIdToAllocSite): (Map[Long, Set[Int]], Map[Int, Option[Long]]) = generateMapping(groundTruthData.toSet, testData.toSet)
    val results = mutable.Map[Int, MethodResult]()
    for ((method, testPTS) <- testData.groupBy(_.defsiteMethodSignature).filter(m => !m._1.contains("$string_concat$"))) {
      val truePositiveData = mutable.Set.empty[TruePositive]
      val falsePositiveData = mutable.Set.empty[FalsePositive]
      val falseNegativeData = mutable.Set.empty[FalseNegative]
      val allocSiteToInstanceIdsForMethod = mutable.Map[Long, Set[Instance]]()
      val instanceIdsToAllocSiteForMethod = mutable.Map[Set[Instance], Option[Long]]()
      val methodId = signatureToMethodId(method)
      val groundTruthMethod = groundTruth.methods(methodId)
      var testAllocationsAtPC = Map[Int, Set[Allocation]]()
      //val testDataCoveredInstances = mutable.Set[(Int, Int)]()
      for (defsite <- testPTS) {
        val groundTruthInstancesAtPC = groundTruthMethod.pcToInstances.getOrElse(defsite.defsitePC, Set.empty) ++ groundTruthMethod.paramToInstances.getOrElse(-defsite.defsitePC - 2, Set.empty)

        allocSiteToInstanceIds.get(defsite.allocSiteId) match {
          case Some(groundTruthInstanceIds) => {

            val groundTruthInstancesForDefSite = groundTruthInstancesAtPC.filter(instance => groundTruthInstanceIds.contains(instance.instanceId))

            if (Set(normalizeType(defsite.objClass)) equals groundTruthInstancesForDefSite.map(inst => normalizeType(inst.objClass))) {
              testAllocationsAtPC += (defsite.defsitePC -> groundTruthInstanceIds.map(groundTruth.instanceIdToAllocation))
              truePositiveData += TruePositive(method, defsite.defsitePC, groundTruthInstancesForDefSite, defsite.allocSiteId, defsite.allocSitePC, defsite.objClass)
            } else {
              falsePositiveData += FalsePositive(method, defsite.defsitePC, defsite.objClass, defsite.allocSiteId, defsite.allocSitePC)
            }
            //testDataCoveredInstances ++= groundTruthInstanceIds.map(inst => (defsite.defsitePC, inst))
          }
          case None => {
            falsePositiveData += FalsePositive(method, defsite.defsitePC, defsite.objClass, defsite.allocSiteId, defsite.allocSitePC)

          }
        }
      }

      for ((pc, instancesAtPc) <- groundTruthMethod.pcToInstances) {
        for ((allocation, instances) <- instancesAtPc.groupBy(inst => inst.allocation)) {
          if (!testAllocationsAtPC.getOrElse(pc,Set.empty).contains(allocation)) {
            falseNegativeData += FalseNegative(method, pc, instances)
          }
        }
      }
      for ((param, instancesAtParam) <- groundTruthMethod.paramToInstances) {
        for ((allocation, instances) <- instancesAtParam.groupBy(inst => inst.allocation)) {
          if (!testAllocationsAtPC.getOrElse(-2 - param, Set.empty).contains(allocation)) {
            falseNegativeData += FalseNegative(method, -2 - param, instances)
          }
        }
      }

      val filteredTruePositiveData = truePositiveData.filter(!ignoreTruePositive(_)).toSet
      val filteredFalsePositiveData = falsePositiveData.filter(!ignoreFalsePositive(_)).toSet
      val filteredFalseNegativeData = falseNegativeData.filter(!ignoreFalseNegative(_)).toSet

      val truePositive = filteredTruePositiveData.size
      val falsePositive = filteredFalsePositiveData.size
      val falseNegative = filteredFalseNegativeData.size
      val precision = if (truePositive + falsePositive > 0) truePositive.toDouble / (truePositive + falsePositive) else 0
      val recall = if (truePositive + falseNegative > 0) truePositive.toDouble / (truePositive + falseNegative) else 0

      results put (methodId, MethodResult(
        method,
        precision,
        recall,
        truePositive,
        falsePositive,
        falseNegative,
        filteredTruePositiveData.toSet,
        filteredFalsePositiveData.toSet,
        filteredFalseNegativeData.toSet,
        allocSiteToInstanceIdsForMethod,
        instanceIdsToAllocSiteForMethod
      ))
    }
    results.toMap
  }

  def printTotalPrecisionRecall(results: Map[Int, MethodResult]): (Int, Int, Int, Double, Double) = {
    val truePositive = results.values.map(_.truePositive).sum
    val falsePositive = results.values.map(_.falsePositive).sum
    val falseNegative = results.values.map(_.falseNegative).sum
    val precision = if (truePositive + falsePositive > 0) 100 * truePositive.toDouble / (truePositive + falsePositive) else 0
    val recall = if (truePositive + falseNegative > 0) 100 * truePositive.toDouble / (truePositive + falseNegative) else 0
    println(f"true positives: $truePositive false positives: $falsePositive false negatives: $falseNegative")
    println(f"total precision: $precision%.2f%% total recall: $recall%.2f%%")
    (truePositive, falsePositive, falseNegative, precision, recall)
  }
}

object ComparePTS {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty){
      throw new IllegalArgumentException("specify cp. e.g. -cp=/home/julius/IdeaProjects/opal/DEVELOPING_OPAL/validate/target/scala-2.13/test-classes/org/opalj/fpcf/fixtures/xl")
    }
      val groundTruth = GroundTruthParser.parseGroundTruth("trace.xml")
    val withoutTAJS = new PointsToAnalysisRunner()
    val withTAJS = new PointsToAnalysisRunner()
    withoutTAJS.main(args)


     val evalWithoutTAJS = GroundTruthParser.evaluate(groundTruth, withoutTAJS.pts, withoutTAJS.p)
    withTAJS.main(args ++ Iterable("RunTAJS"))
    val evalWithTAJS = GroundTruthParser.evaluate(groundTruth, withTAJS.pts, withTAJS.p)

    println(s"without TAJS: ${withoutTAJS.pts.count(_._3.exists(_.types.nonEmpty))} non-empty pts")
    println(s"<pointstotrace id=\"withoutTAJS\" />")
    println(s"with TAJS: ${withTAJS.pts.count(_._3.exists(_.types.nonEmpty))} non-empty pts")
    println(s"<pointstotrace id=\"withTAJS\" />")

    for ((id) <-  evalWithoutTAJS.keys.toList.sorted) {
        val resultWithoutTAJS = evalWithoutTAJS(id)
        val resultWithTAJS = evalWithTAJS(id)
      if (resultWithoutTAJS.falseNegative + resultWithoutTAJS.falsePositive + resultWithoutTAJS.truePositive > 0) {

        print(id)
        print(": ")

        val withoutStr = resultWithoutTAJS.toString
        val withStr = resultWithTAJS.toString
        /*if (withStr == withoutStr){
          println(withStr)
        } else */
          println("without TAJS: ")
          println(withoutStr)
          println("with TAJS:")
          println(withStr)
        if (resultWithoutTAJS.falseNegative + resultWithoutTAJS.falsePositive + resultWithoutTAJS.truePositive !=
          resultWithTAJS.falseNegative + resultWithTAJS.falsePositive + resultWithTAJS.truePositive) {
          println("unequal datapoint count!")
        }


      }
    }

    println("without TAJS")
    val evalWithout = GroundTruthParser.printTotalPrecisionRecall(evalWithoutTAJS)
    println("with TAJS")
    val evalWith = GroundTruthParser.printTotalPrecisionRecall(evalWithTAJS)

    println("""
      \begin{table}
      \center
      \begin{tabular}{lcc}
      	\toprule
      \textbf{Points-To Analysis } & \textbf{Only Java} & \textbf{Java + JS}  \\
      	\midrule""" + f"""
        True Positives  & \\tnum{${evalWithout._1}} & \\tnum{${evalWith._1}} \\\\
        False Positives & \\tnum{${evalWithout._2}} & \\tnum{${evalWith._2}} \\\\
        False Negatives & \\tnum{${evalWithout._3}} & \\tnum{${evalWith._3}} \\\\
        \\midrule
        Precision & \\tnum{${evalWithout._4}%.2f\\%%} & \\tnum{${evalWith._4}%.2f\\%%} \\\\
        Recall    & \\tnum{${evalWithout._5}%.2f\\%%} & \\tnum{${evalWith._5}%.2f\\%%} \\\\""" + """
      	\bottomrule
      \end{tabular}
      \caption{Precision And Recall of Points-To-Sets}
      \label{tab:precisionRecall}
      \end{table}
      """.stripMargin)
  }
}

class PointsToAnalysisRunner extends ProjectAnalysisApplication {
  var pts = Set[(DefinedMethod, Int, Option[AllocationSitePointsToSet])]()
  var p: Project[URL] = null

  override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
    Iterable()
  }

  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
    p = project
    val TAJSAnalyses = Iterable(
      AllocationSiteBasedScriptEngineDetectorScheduler,
      AllocationSiteBasedTriggeredTajsConnectorScheduler,
    )
    var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

    analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(project)
    analyses ::= new TypePropagationAnalysisScheduler(XTASetEntitySelector)
    val runTAJS = parameters.contains("RunTAJS")
    if (runTAJS) analyses ++= TAJSAnalyses

    PropertyStore.updateDebug(true)
    implicit val logContext: LogContext = project.logContext
    project.getOrCreateProjectInformationKeyInitializationData(
      PropertyStoreKey,
      (context: List[PropertyStoreContext[AnyRef]]) => {
        val ps = PKESequentialPropertyStore(context: _*)
        ps
      }
    )

    val (ps1, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

    val filter: SomeEPS => Boolean = _ => true
    val allEntities = ps1.entities(propertyFilter = filter).toList

    val definedMethods = allEntities.filter(_.isInstanceOf[DefinedMethod]).map(_.asInstanceOf[DefinedMethod])

    var results = ""
    for (m <- definedMethods) {
      val dm = m.definedMethod
      val defsitesInMethod = ps1.entities(propertyFilter = _.e.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite]).filter(_.method == dm).toSet
      val ptsInMethod = defsitesInMethod.toArray.map(defsite => (m, defsite.pc, ps1.properties(defsite).map(_.toFinalEP.p).find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet])))

      pts ++= ptsInMethod
      results += m.name + "\n"


      val params = ps1.entities(propertyFilter = _.e.isInstanceOf[VirtualFormalParameter]).map(_.asInstanceOf[VirtualFormalParameter]).filter(_.method == m).toList

      val ptsInMethodParams = params.toArray.map(param => (m, param.origin , ps1.properties(param).map(_.toFinalEP.p).find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet])))
      pts ++= ptsInMethodParams

    }


    BasicReport(
      results
    )
  }
}
