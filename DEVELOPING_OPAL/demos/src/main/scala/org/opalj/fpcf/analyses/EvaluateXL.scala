/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.br.{DefinedMethod, ObjectType}
import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication}
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
}
case class Allocation(method: Method, pc: Int, classtype: String)
class Instance(val instanceId: Int, val objClass: String, val allocation: Option[Allocation]) {
  override def toString: String = {
  s"Instance($instanceId $objClass ${allocation.map(a => s"m ${a.method.methodId} pc ${a.pc}").getOrElse("noallocsite")})"
  }
}
case class GroundTruthLogEvent(instanceClass: String, instanceId: Int, methodSignature: String, pc: Int)
case class DefsiteData(objClass: String, allocSiteId: Long, allocSitePC: Int, defsiteMethodSignature: String, defsitePC: Int)

class Dataset {
  val methods: mutable.Map[Int, Method] = mutable.Map.empty[Int, Method]
  // method Id + program counter
  val allocations = mutable.Map[(String, Int), Allocation]()
  val instanceIdToAllocation = mutable.Map[Int, Allocation]()
  def addMethod(methodId: Int, method: Method): Unit = {
    methods(methodId) = method
  }

  def addInstance(methodId: Int, pc: Int, instance: Instance): Unit = {
    if (methods.contains(methodId)) {
      val method = methods(methodId)
      method.pcToInstances(pc) = method.pcToInstances(pc) + instance
    }
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
    val instancePattern = new Regex("<(allocation|traceevent) methodId=\"(\\d+)\" pc=\"(\\d+)\" instanceId=\"(\\d+)\" class=\"([^\"]+)\"/>")

    for (line <- Source.fromFile(groundTruthPath).getLines()) {
      methodPattern.findFirstMatchIn(line) match {
        case Some(m) /* if !m.group(1).contains("$string_concat$") */ =>
          val signature = m.group(1)
          val methodId = m.group(2).toInt
          dataset.addMethod(methodId, new Method(methodId, signature))
        case _ =>
      }

      instancePattern.findFirstMatchIn(line) match {
        case Some(m) =>
          val eventtype = m.group(1)
          val methodId = m.group(2).toInt
          val pc = m.group(3).toInt
          val instanceId = m.group(4).toInt
          val objClass = m.group(5)

          dataset.methods.get(methodId) match {
            case Some(method) => {

              if (eventtype equals "allocation") {
                val allocation = Allocation(method, pc, objClass)
                dataset.allocations.getOrElseUpdate((method.signature, pc), allocation)
                dataset.instanceIdToAllocation.put(instanceId, allocation)
              }
              val instance = new Instance(instanceId, objClass, dataset.instanceIdToAllocation.get(instanceId))
              dataset.addInstance(methodId, pc, instance)
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
      truePositive.defsiteMethod.contains("$string_concat$")
  }
  def ignoreFalseNegative(falseNegative: FalseNegative) : Boolean = {
    falseNegative.instances.exists(_.objClass.equals("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
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
    // expected: allocsite 175924209254439 <=> inst 44563007 (Vain.main, PC 35)
    (allocSiteToInstanceId.toMap, instanceIdToAllocSite.toMap)

  }

  def evaluate(groundTruth: Dataset, testPts: Set[(DefinedMethod, DefinitionSite, Option[AllocationSitePointsToSet])], project: Project[URL]) : Map[Int, MethodResult]= {
    val groupedWithoutByMethod = testPts.groupBy(_._1.definedMethod.fullyQualifiedSignature)
    val groundTruthData = mutable.Set[GroundTruthLogEvent]()
    val signatureToMethodId = mutable.Map[String, Int]()
    for ((methodId, method) <- groundTruth.methods) {
      signatureToMethodId.put(method.signature, methodId)
      for ((pc, instances) <- method.pcToInstances) {
        for (instance <- instances) {
          groundTruthData add GroundTruthLogEvent(instance.objClass, instance.instanceId, method.signature, pc)
        }
      }
    }
    val testData = mutable.Set[DefsiteData]()
    for ((methodSignature, defSite) <- groupedWithoutByMethod) {
      implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
      val sorted = defSite.toArray.sortBy(mdsas => mdsas._2.pc)
      for (pts <- sorted) {
        pts._3 match {
          case Some(as) => for (allocSiteId <- as.elements.iterator) {
            val (ctx, allocSitePC, typeId) = longToAllocationSite(allocSiteId)
            val objClass = ObjectType.lookup(typeId).toJava
            testData add DefsiteData(objClass, allocSiteId, allocSitePC, methodSignature, pts._2.pc)
            println("<pointsto method=\"" + methodSignature + "\" pc=\"" + pts._2.pc + "\" type=\"" + objClass + "\" allocSiteId=\"" + allocSiteId + "\" allocSitePc=\"" + allocSitePC + "\" />")
          }
          case None =>
        }
      }
    }
    val (allocSiteToInstanceIds, instanceIdToAllocSite): (Map[Long, Set[Int]], Map[Int, Option[Long]]) = generateMapping(groundTruthData.toSet, testData.toSet)
    //implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
    val results = mutable.Map[Int, MethodResult]()
    for ((method, testPTS) <- testData.groupBy(_.defsiteMethodSignature).filter(m => !m._1.contains("$string_concat$"))) {
      val truePositiveData = mutable.Set.empty[TruePositive]
      val falsePositiveData = mutable.Set.empty[FalsePositive]
      val falseNegativeData = mutable.Set.empty[FalseNegative]
      val allocSiteToInstanceIdsForMethod = mutable.Map[Long, Set[Instance]]()
      val instanceIdsToAllocSiteForMethod = mutable.Map[Set[Instance], Option[Long]]()
      val methodId = signatureToMethodId(method)
      val groundTruthMethod = groundTruth.methods(methodId)
      //val instancesAndPossibleAllocSites = instances.map(instance => (instanceIdToAllocSite.getOrElse(instance.instanceId, Option.empty), instance.instanceId, normalizeType(instance.objClass)))
      ///val possibleAllocSitesForInstanceIds = instancesAndPossibleAllocSites.groupBy(allocSiteInstIdType => (allocSiteInstIdType._1, allocSiteInstIdType._3)).view.mapValues(instances => instances.map(inst => new Instance(inst._2, inst._3))).toMap

      val testDataCoveredInstances = mutable.Set[(Int, Int)]()
      for (defsite <- testPTS) {
        val groundTruthInstancesAtPC = groundTruthMethod.pcToInstances.getOrElse(defsite.defsitePC, Set.empty)

        allocSiteToInstanceIds.get(defsite.allocSiteId) match {
          case Some(groundTruthInstanceIds) => {
            val groundTruthInstancesForDefSite = groundTruthInstancesAtPC.filter(instance => groundTruthInstanceIds.contains(instance.instanceId))

            if (Set(normalizeType(defsite.objClass)) equals groundTruthInstancesForDefSite.map(inst => normalizeType(inst.objClass))) {
              truePositiveData += TruePositive(method, defsite.defsitePC, groundTruthInstancesForDefSite, defsite.allocSiteId, defsite.allocSitePC, defsite.objClass)
            } else {
              falsePositiveData += FalsePositive(method, defsite.defsitePC, defsite.objClass, defsite.allocSiteId, defsite.allocSitePC)
            }
            testDataCoveredInstances ++= groundTruthInstanceIds.map(inst => (defsite.defsitePC, inst))
          }
          case None => {
            falsePositiveData += FalsePositive(method, defsite.defsitePC, defsite.objClass, defsite.allocSiteId, defsite.allocSitePC)

          }
        }
      }

      for ((pc, instancesAtPc) <- groundTruthMethod.pcToInstances) {

        for ((allocation, instances) <- instancesAtPc.groupBy(inst => inst.allocation)) {
          if (! instances.map(inst => (pc, inst.instanceId)).subsetOf(testDataCoveredInstances)) {
            falseNegativeData += FalseNegative(method, pc, instances)
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
    /*
      all.get((Option(allocSiteId), normalizeType(allocSiteType))) match {
        case Some(groundTruthInstances) =>
          allocSiteToInstanceIdsForMethod.put(allocSiteId, groundTruthInstances)
          //val groundTruthType = groundTruthInstanceIds(groundTruthIds.find(_ => true).get)
          //val groundTruthTypeNormalized = normalizeType(groundTruthType)
          testAllocsitesForPC add(allocSiteId, allocSiteType)
          //if (normalizeType(allocSiteType) == groundTruthTypeNormalized) {
          truePositiveData += TruePositive(method.signature, pc, groundTruthInstances, allocSiteId, pc, allocSiteType)
        //} else {
        //  falsePositiveData += FalsePositive(method.signature, defsitePC, allocSiteType, allocSiteId, pc)
        //}
        case _ => {
          allocSiteToInstanceIdsForMethod.put(allocSiteId, Set.empty)
          falsePositiveData += FalsePositive(method.signature, pc, allocSiteType, allocSiteId, pc)
        }
      }
    }
      /*
    groundTruth.methods.foldLeft(Map.empty[Int, MethodResult]) { case (results, (methodId, method)) =>
      val truePositiveData = mutable.Set.empty[TruePositive]
      val falsePositiveData = mutable.Set.empty[FalsePositive]
      val falseNegativeData = mutable.Set.empty[FalseNegative]
      val testPTSforMethod = groupedWithoutByMethod.get(method.signature)
      val allocSiteToInstanceIdsForMethod = mutable.Map[Long, Set[Instance]]()
      val instanceIdsToAllocSiteForMethod = mutable.Map[Set[Instance], Option[Long]]()

      for ((pc, instances) <- method.pcToInstances) {
        //val groundTruthInstanceIds = instances.map(instance => instance.instanceId -> normalizeType(instance.objClass)).toMap
        val instancesAndPossibleAllocSites = instances.map(instance => (instanceIdToAllocSite.getOrElse(instance.instanceId, Option.empty), instance.instanceId, normalizeType(instance.objClass)))
        val possibleAllocSitesForInstanceIds = instancesAndPossibleAllocSites.groupBy(allocSiteInstIdType => (allocSiteInstIdType._1, allocSiteInstIdType._3)).view.mapValues(instances =>  instances.map(inst => new Instance(inst._2, inst._3))).toMap
        val testPTSforPC = testPTSforMethod.map(pt => pt.filter(_._2.pc == pc))
        val testAllocsitesForPC = mutable.Set[(Long, String)]()
        val allocSites = testPTSforPC.getOrElse(Set.empty).flatMap(as => as._3)
        for (as <- allocSites) {
           for (allocSiteId <- as.elements.iterator) {
              val (ctx, allocSitePC, typeId) = longToAllocationSite(allocSiteId)
              val allocSiteType = normalizeType(ObjectType.lookup(typeId).toJava)
              possibleAllocSitesForInstanceIds.get((Option(allocSiteId), normalizeType(allocSiteType))) match {
                case Some(groundTruthInstances) =>
                  allocSiteToInstanceIdsForMethod.put(allocSiteId, groundTruthInstances)
                  //val groundTruthType = groundTruthInstanceIds(groundTruthIds.find(_ => true).get)
                  //val groundTruthTypeNormalized = normalizeType(groundTruthType)
                  testAllocsitesForPC add (allocSiteId , allocSiteType)
                  //if (normalizeType(allocSiteType) == groundTruthTypeNormalized) {
                    truePositiveData += TruePositive(method.signature, pc, groundTruthInstances, allocSiteId, pc, allocSiteType)
                  //} else {
                  //  falsePositiveData += FalsePositive(method.signature, defsitePC, allocSiteType, allocSiteId, pc)
                  //}
                case _ => {
                  allocSiteToInstanceIdsForMethod.put(allocSiteId, Set.empty)
                  falsePositiveData += FalsePositive(method.signature, pc, allocSiteType, allocSiteId, pc)
                }
              }
            }
        }
        instances.map(inst => ())

        for ((possibleAllocSite, groundTruthInstances) <- possibleAllocSitesForInstanceIds) {
          instanceIdsToAllocSiteForMethod.put(groundTruthInstances, possibleAllocSite._1)
          if ( possibleAllocSite._1.isEmpty || !testAllocsitesForPC.contains((possibleAllocSite._1.get, possibleAllocSite._2))) {
            falseNegativeData += FalseNegative(method.signature, pc, groundTruthInstances)
          } else {
            print("")
          }
        }



      }
      val truePositive = truePositiveData.size
      val falsePositive = falsePositiveData.size
      val falseNegative = falseNegativeData.size
      val precision = if (truePositive + falsePositive > 0) truePositive.toDouble / (truePositive + falsePositive) else 0
      val recall = if (truePositive + falseNegative > 0) truePositive.toDouble / (truePositive + falseNegative) else 0

      results + (methodId -> MethodResult(
        method.signature,
        precision,
        recall,
        truePositive,
        falsePositive,
        falseNegative,
        truePositiveData.toSet,
        falsePositiveData.filter(!ignoreFalsePositive(_)).toSet,
        falseNegativeData.filter(!ignoreFalseNegative(_)).toSet,
        allocSiteToInstanceIdsForMethod,
        instanceIdsToAllocSiteForMethod
      )) */

    }
  */

  def printTotalPrecisionRecall(results: Map[Int, MethodResult]): Unit = {
    val truePositive = results.values.map(_.truePositive).sum
    val falsePositive = results.values.map(_.falsePositive).sum
    val falseNegative = results.values.map(_.falseNegative).sum
    val precision = if (truePositive + falsePositive > 0) 100 * truePositive.toDouble / (truePositive + falsePositive) else 0
    val recall = if (truePositive + falseNegative > 0) 100 * truePositive.toDouble / (truePositive + falseNegative) else 0
    println(f"true positives: $truePositive false positives: $falsePositive false negatives: $falseNegative")
    println(f"total precision: $precision%.2f%% total recall: $recall%.2f%%")
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

    //val groupedWithoutByMethod = withoutTAJS.pts.groupBy(_._1.toString)
    /*for (elem <- groupedWithoutByMethod) {
      println(elem._1)
      val sorted = elem._2.toArray.sortBy(mdsas => mdsas._2.pc)
      for (pts <- sorted) {
        println("  " + pts._2.pc + "  " + pts._3.toString)

      }
    }*/

     val evalWithoutTAJS = GroundTruthParser.evaluate(groundTruth, withoutTAJS.pts, withoutTAJS.p)
    withTAJS.main(args ++ Iterable("RunTAJS"))
    val evalWithTAJS = GroundTruthParser.evaluate(groundTruth, withTAJS.pts, withTAJS.p)

    //val groupedWithout = withoutTAJS.pts.groupBy(_._2.method.signature.toString)
    //val groupedWith = withTAJS.pts.groupBy(_._2.method.signature.toString)
    //val combinedMap = (groupedWithout.keySet ++ groupedWith.keySet).map(ds => (ds, groupedWithout.get(ds).map(_.map(_._3)), groupedWith.get(ds).map(_.map(_._3))))
    //val differences = combinedMap.filter(comparison => comparison._2.toString != comparison._3.toString)
    println(s"without TAJS: ${withoutTAJS.pts.count(_._3.exists(_.types.nonEmpty))} non-empty pts")
    println(s"<pointstotrace id=\"withoutTAJS\" />")
    //withoutTAJS.printPointsToSet()
    println(s"with TAJS: ${withTAJS.pts.count(_._3.exists(_.types.nonEmpty))} non-empty pts")
    println(s"<pointstotrace id=\"withTAJS\" />")
    //withTAJS.printPointsToSet()

    for ((id) <-  evalWithoutTAJS.keys.toList.sorted) {
        val resultWithoutTAJS = evalWithoutTAJS(id)
        val resultWithTAJS = evalWithTAJS(id)
      if (resultWithoutTAJS.falseNegative + resultWithTAJS.falsePositive + resultWithoutTAJS.truePositive > 0) {
        print(id)
        print(": ")
        val withoutStr = resultWithoutTAJS.toString
        val withStr = resultWithTAJS.toString
        if (withStr == withoutStr){
          println(withStr)
        } else {
          println("without TAJS: ")
          println(withoutStr)
          println("with TAJS:")
          println(withStr)


        }
      }
    }




    println("without TAJS")
    GroundTruthParser.printTotalPrecisionRecall(evalWithoutTAJS)
    println("with TAJS")
    GroundTruthParser.printTotalPrecisionRecall(evalWithTAJS)

  }
}

class PointsToAnalysisRunner extends ProjectAnalysisApplication {
  var pts = Set[(DefinedMethod, DefinitionSite, Option[AllocationSitePointsToSet])]()
  var p: Project[URL] = null

  override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
    Iterable()
  }

  def printPointsToSet() = {

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

    //val defSites = allEntities.filter(_.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite])
    val definedMethods = allEntities.filter(_.isInstanceOf[DefinedMethod]).map(_.asInstanceOf[DefinedMethod])

    var results = ""
    for (m <- definedMethods) {
      val dm = m.definedMethod
      val defsitesInMethod = ps1.entities(propertyFilter = _.e.isInstanceOf[DefinitionSite]).map(_.asInstanceOf[DefinitionSite]).filter(_.method == dm).toSet
      val ptsInMethod = defsitesInMethod.toArray.map(defsite => (m, defsite, ps1.properties(defsite).map(_.toFinalEP.p).find(_.isInstanceOf[AllocationSitePointsToSet]).map(_.asInstanceOf[AllocationSitePointsToSet])))
      pts ++= ptsInMethod
      results += m.name + "\n"
      // results += "points to sets: " + pts
    }

    BasicReport(
      results
    )
  }
}
