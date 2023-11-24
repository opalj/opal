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
  val pcToInstances: mutable.Map[Int, List[Instance]] = mutable.Map.empty[Int, List[Instance]].withDefaultValue(List.empty[Instance])
}

class Instance(val instanceId: Int, val objClass: String)

class Dataset {
  val methods: mutable.Map[Int, Method] = mutable.Map.empty[Int, Method]

  def addMethod(methodId: Int, method: Method): Unit = {
    methods(methodId) = method
  }

  def addInstance(methodId: Int, pc: Int, instance: Instance): Unit = {
    if (methods.contains(methodId)) {
      val method = methods(methodId)
      method.pcToInstances(pc) = method.pcToInstances(pc) :+ instance
    }
  }
}
case class TruePositive(defsiteMethod: String, defsitePC: Int, instanceType: String, instanceID: Int, allocSiteID: Long, allocsitePC: Int, allocsiteType: String) {
  override def toString: String =
    s"TruePositive(defsitePC=$defsitePC, instanceType=$instanceType, " +
      s"instanceID=$instanceID, allocSiteID=$allocSiteID, allocsitePC=$allocsitePC, allocsiteType=$allocsiteType)"
}

case class FalsePositive(defsiteMethod: String, defsitePC: Int, allocSiteType: String, allocSiteID: Long, allocsitePC: Int) {
  override def toString: String =
    s"FalsePositive(defsitePC=$defsitePC, allocSiteType=$allocSiteType, " +
      s"allocSiteID=$allocSiteID, allocsitePC=$allocsitePC)"
}

case class FalseNegative(defsiteMethod: String, defsitePC: Int, instanceType: String, instanceID: Int) {
  override def toString: String =
    s"FalseNegative(defsitePC=$defsitePC, instanceType=$instanceType, " +
      s"instanceID=$instanceID)"
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
                         falseNegativeData: Set[FalseNegative]
                       ) {
  override def toString: String = {

    val truePositiveDataStr = truePositiveData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")
    val falsePositiveDataStr = falsePositiveData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")
    val falseNegativeDataStr = falseNegativeData.toList.sortBy(_.defsitePC).map(_.toString).mkString("\n")

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
       |""".stripMargin
  }
}

object GroundTruthParser {

  def parseGroundTruth(groundTruthPath: String): Dataset = {
    val dataset = new Dataset()

    val methodPattern = new Regex("<method fullyqualified=\"(.+)\" id=\"(\\d+)\"/>")
    val instancePattern = new Regex("<methodId=\"(\\d+)\" pc=\"(\\d+)\" instanceId=\"(\\d+)\" class=\"([^\"]+)\"/>")

    for (line <- Source.fromFile(groundTruthPath).getLines()) {
      methodPattern.findFirstMatchIn(line) match {
        case Some(m) if !m.group(1).contains("$string_concat$") =>
          val signature = m.group(1)
          val methodId = m.group(2).toInt
          dataset.addMethod(methodId, new Method(methodId, signature))
        case _ =>
      }

      instancePattern.findFirstMatchIn(line) match {
        case Some(m) =>
          val methodId = m.group(1).toInt
          val pc = m.group(2).toInt
          val instanceId = m.group(3).toInt
          val objClass = m.group(4)
          val instance = new Instance(instanceId, objClass)
          dataset.addInstance(methodId, pc, instance)
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
  def ignoreFalseNegative(falseNegative: (String, Int, String, Int)) : Boolean = {
    false //falseNegative._3 == "org.openjdk.nashorn.api.scripting.NashornScriptEngine"
  }
  def generateMapping(groundTruth: Set[(String, Int, String, Int)], test: Set[(String, Long, String, Int)]): (Map[Long, Int], Map[Int, Option[Long]]) = {
    val allocSiteToInstanceId = mutable.Map[Long, Int]()
    val instanceIdToAllocSite = mutable.Map[Int, Option[Long]]()
    val groundTruthDict = groundTruth.map(typeInstanceMethodPc => ((normalizeType(typeInstanceMethodPc._1), typeInstanceMethodPc._3, typeInstanceMethodPc._4), typeInstanceMethodPc._2)).toMap
    for ((typeName, allocsite, method, pc) <- test) {
      val normalized = normalizeType(typeName)
      groundTruthDict.get((normalized, method, pc)) match {
        case Some(instanceId) => {
          allocSiteToInstanceId.put(allocsite, instanceId)
          instanceIdToAllocSite.put(instanceId, Option(allocsite))
        }
        case None => {}
      }
    }
    for ((typeName, instanceId, method, pc) <- groundTruth) {
      if (!groundTruthDict.contains((normalizeType(typeName), method, pc))) {
        instanceIdToAllocSite.put(instanceId, Option.empty)
      }
    }
    (allocSiteToInstanceId.toMap, instanceIdToAllocSite.toMap)

  }

  def evaluate(groundTruth: Dataset, testPts: Set[(DefinedMethod, DefinitionSite, Option[AllocationSitePointsToSet])], project: Project[URL]) : Map[Int, MethodResult]= {
    val groupedWithoutByMethod = testPts.groupBy(_._1.definedMethod.fullyQualifiedSignature)
    val groundTruthData = mutable.Set[(String, Int, String, Int)]()

    for ((_, method) <- groundTruth.methods) {
      for ((pc, instances) <- method.pcToInstances) {
        for (instance <- instances) {
          groundTruthData add (instance.objClass, instance.instanceId, method.signature, pc)
        }
      }
    }
    val testData = mutable.Set[(String, Long, String, Int)]()
    for ((methodSignature, defSite) <- groupedWithoutByMethod) {
      implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
      val sorted = defSite.toArray.sortBy(mdsas => mdsas._2.pc)
      for (pts <- sorted) {
        pts._3 match {
          case Some(as) => for (allocSiteId <- as.elements.iterator) {
            val (ctx, pc, typeId) = longToAllocationSite(allocSiteId)
            val objClass = ObjectType.lookup(typeId).toJava
            testData add ((objClass, allocSiteId, methodSignature, pts._2.pc))
            println("<pointsto method=\"" + methodSignature + "\" pc=\"" + pts._2.pc + "\" type=\"" + objClass + "\" allocSiteId=\"" + allocSiteId + "\" allocSitePc=\"" + pc + "\" />")
          }
          case None =>
        }
      }
    }
    val (allocSiteToInstanceId, instanceIdToAllocSite): (Map[Long, Int], Map[Int, Option[Long]]) = generateMapping(groundTruthData.toSet, testData.toSet)
    implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)

    groundTruth.methods.foldLeft(Map.empty[Int, MethodResult]) { case (results, (methodId, method)) =>

      val truePositiveData = mutable.Set.empty[TruePositive]
      val falsePositiveData = mutable.Set.empty[FalsePositive]
      val falseNegativeData = mutable.Set.empty[FalseNegative]
      val testPTSforMethod = groupedWithoutByMethod.get(method.signature)
      for ((pc, instances) <- method.pcToInstances) {
        val groundTruthInstanceIds = instances.map(instance => instance.instanceId -> normalizeType(instance.objClass)).toMap
        val testPTSforPC = testPTSforMethod.map(pt => pt.filter(_._2.pc == pc))
        val testInstanceIdsForPC = mutable.Set[(Int, String)]()
        testPTSforPC match {
          case Some(defSite) => {
            for (pts <- defSite) {
              pts._3 match {
                case Some(as) => for (allocSiteId <- as.elements.iterator) {
                  val (ctx, pc, typeId) = longToAllocationSite(allocSiteId)
                  val allocSiteType = normalizeType(ObjectType.lookup(typeId).toJava)
                  allocSiteToInstanceId.get(allocSiteId) match {
                    case Some(groundTruthId) if groundTruthInstanceIds.contains(groundTruthId) =>
                      val groundTruthType = groundTruthInstanceIds(groundTruthId)
                      val groundTruthTypeNormalized = normalizeType(groundTruthType)
                      testInstanceIdsForPC add (groundTruthId , groundTruthTypeNormalized)
                      if (normalizeType(allocSiteType) == groundTruthTypeNormalized) {
                        truePositiveData += TruePositive(method.signature, pts._2.pc, groundTruthType, groundTruthId, allocSiteId, pc, allocSiteType)
                      } else {
                        falsePositiveData += FalsePositive(method.signature, pts._2.pc, allocSiteType, allocSiteId, pc)
                      }
                    case _ => falsePositiveData += FalsePositive(method.signature, pts._2.pc, allocSiteType, allocSiteId, pc)
                  }
                }
                case None =>
              }
            }
          }
          case None => {
            // no PTS
          }
        }
        for (instance <- instances) {
          if (!testInstanceIdsForPC.contains((instance.instanceId, instance.objClass))) {
            val falseNegative = (method.signature, pc, instance.objClass, instance.instanceId)
            if (!ignoreFalseNegative(falseNegative)) {
              falseNegativeData += FalseNegative(method.signature, pc, instance.objClass, instance.instanceId)
            }
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
        falsePositiveData.toSet,
        falseNegativeData.toSet
      ))

    }

  }
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
