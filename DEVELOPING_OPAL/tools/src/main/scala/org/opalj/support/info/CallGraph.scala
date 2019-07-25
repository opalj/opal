/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.net.URL

import com.typesafe.config.ConfigValueFactory

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.Field
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.pointsto.ArrayEntity
import org.opalj.tac.fpcf.analyses.pointsto.TamiFlexKey

/**
 * Computes a call graph and reports its size.
 *
 * You can specify the call-graph algorithm:
 *  -algorithm=CHA for an CHA-based call graph
 *  -algorithm=RTA for an RTA-based call graph
 *  -algorithm=PointsTo for a points-to based call graph
 * The default algorithm is RTA.
 *
 * Please also specify whether the target (-cp=) is an application or a library using "-projectConf=".
 * Predefined configurations `ApplicationProject.conf` or `LibraryProject.conf` can be used here.
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * @author Florian Kuebler
 */
object CallGraph extends ProjectAnalysisApplication {

    override def title: String = "Call Graph Analysis"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-algorithm=CHA|RTA|PointsTo]"+"[-callers=method]"+"[-callees=method]"+"[-writeCG=file]"+"[-writeTimings=file]"+"[-writePointsToSets=file]"+"[-main=package.MainClass]"+"[-tamiflex-log=logfile]"
    }

    private val algorithmRegex = "-algorithm=(CHA|RTA|PointsTo)".r

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.matches(algorithmRegex.regex) &&
                    !p.startsWith("-callers=") &&
                    !p.startsWith("-callees=") &&
                    !p.startsWith("-writeCG=") &&
                    !p.startsWith("-writeTimings=") &&
                    !p.startsWith("-writePointsToSets=") &&
                    !p.startsWith("-main=") &&
                    !p.startsWith("-tamiflex-log=")
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    private[this] def performAnalysis(
        project:      Project[URL],
        calleesSigs:  List[String],
        callersSigs:  List[String],
        cgAlgorithm:  String,
        cgFile:       Option[String],
        timingsFile:  Option[String],
        pointsToFile: Option[String]
    ): BasicReport = {
        // TODO: Implement output files
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm ⇒
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.toTraversable

        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val cg = cgAlgorithm match {
            case "CHA"               ⇒ project.get(CHACallGraphKey)
            case "RTA"               ⇒ project.get(RTACallGraphKey)
            case "TypeBasedPointsTo" ⇒ project.get(TypeBasedPointsToCallGraphKey)
            case "PointsTo"          ⇒ project.get(AllocationSiteBasedPointsToCallGraphKey)
        }

        //project.get(FPCFAnalysesManagerKey).runAll(AllocationSiteBasedPointsToAnalysisScheduler)

        val ptss = ps.entities(TypeBasedPointsToSet.key).toList
        val statistic = ptss.groupBy(p ⇒ p.ub.elements.size).mapValues(_.size).toArray.sorted
        println(statistic.mkString("\n"))

        val ptss2 = ps.entities(AllocationSitePointsToSet.key).toList
        import scala.collection.JavaConverters._
        val statistic2 = ptss2.groupBy(p ⇒ p.ub.elements.size).mapValues { spts ⇒
            (spts.size, {
                val unique = new java.util.IdentityHashMap[AllocationSitePointsToSet, AllocationSitePointsToSet]()
                unique.putAll(spts.map(x ⇒ x.ub → x.ub).toMap.asJava)
                unique.size()
            })
        }.toArray.sorted
        println(statistic2.mkString("\n"))

        println(s"TypeBased PTSs ${ptss.size}")
        println(s"AllocSite PTSs ${ptss2.size}")
        println(s"PTS entries ${ptss2.map(p ⇒ p.ub.elements.size).sum}")

        val byType = ptss2.groupBy(_.e.getClass)
        println(s"DefSite PTSs: ${byType(classOf[DefinitionSite]).size}")
        println(s"Parameter PTSs: ${byType(classOf[VirtualFormalParameter]).size}")
        println(s"Instance Field PTSs: ${byType(classOf[Tuple2[Long, Field]]).size}")
        println(s"Static Field PTSs: ${byType(classOf[Field]).size}")
        println(s"Array PTSs: ${byType(classOf[ArrayEntity[Long]]).size}")

        println(s"DefSite PTS entries: ${byType(classOf[DefinitionSite]).map(_.ub.numElements).sum}")
        println(s"Parameter PTS entries: ${byType(classOf[VirtualFormalParameter]).map(_.ub.numElements).sum}")
        println(s"Instance Field PTS entries: ${byType(classOf[Tuple2[Long, Field]]).map(_.ub.numElements).sum}")
        println(s"Static Field PTS entries: ${byType(classOf[Field]).map(_.ub.numElements).sum}")
        println(s"Array PTS entries: ${byType(classOf[ArrayEntity[Long]]).map(_.ub.numElements).sum}")

        /*
        //Prints sizes of all array PTSs
        for(pts <- byType(classOf[ArrayEntity[Long]]))
            println(s"${org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(pts.e.asInstanceOf[ArrayEntity[Long]].element)}\t${pts.ub.numElements}")*/

        /*
        //Prints all allocation sites in the PTS of Object.<init>'s this parameter in Doop's format
        val initThis = project.get(VirtualFormalParametersKey)(project.get(DeclaredMethodsKey)(ObjectType.Object, "", ObjectType.Object, "<init>", MethodDescriptor.NoArgsAndReturnVoid))(0)
        val initThisPTS = ps(initThis, AllocationSitePointsToSet.key).ub
        initThisPTS.elements.foreach { as ⇒
            try {
                val (dm, pc, tId) = org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(as)
                println(s"<${dm.declaringClassType.toJava}: ${dm.descriptor.toJava(dm.name)}>/new ${project.classHierarchy.knownTypesMap(tId).toJava}")
            } catch { case _: Exception ⇒ }
        }*/

        /*val p2 = project.recreate(e ⇒ e != PropertyStoreKey.uniqueId && e != AllocationSiteBasedPointsToCallGraphKey.uniqueId && e != FPCFAnalysesManagerKey.uniqueId && e != AllocationSiteBasedPointsToCallGraphKey.uniqueId)
        p2.get({AllocationSiteBasedPointsToCallGraphKey})
        val ps2 = p2.get(PropertyStoreKey)

        for {
            FinalEP(e, p) ← ptss2
            ub2 = ps2(e, AllocationSitePointsToSet.key).ub
            if p.elements.size != ub2.elements.size
        } {
            println(s"$e\n\t${ub2.elements.iterator.map(org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(_)).mkString(",")}\n\t${p.elements.iterator.map(org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(_)).mkString(",")}")
        }*/

        val reachableMethods = cg.reachableMethods().toTraversable

        val numEdges = cg.numEdges

        println(ps.statistics.mkString("\n"))

        println(calleesSigs.mkString("\n"))
        println(callersSigs.mkString("\n"))

        for (m ← allMethods) {
            val mSig = m.descriptor.toJava(m.name)

            for (methodSignature ← calleesSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callees of ${m.toJava}:")
                    println(ps(m, Callees.key).ub.callSites().map {
                        case (pc, callees) ⇒ pc → callees.map(_.toJava).mkString(", ")
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
            for (methodSignature ← callersSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callers of ${m.toJava}:")
                    println(ps(m, Callers.key).ub.callers.map {
                        case (caller, pc, isDirect) ⇒
                            s"${caller.toJava}, $pc${if (!isDirect) ", indirect" else ""}"
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        if (cgFile.nonEmpty) {
            CallGraphSerializer.writeCG(cg, new File(cgFile.get))
        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }

    // todo: we would like to print the edges for a given method
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        var calleesSigs: List[String] = Nil
        var callersSigs: List[String] = Nil
        var cgAlgorithm: String = "RTA"
        var cgFile: Option[String] = None
        var timingsFile: Option[String] = None
        var pointsToFile: Option[String] = None
        var mainClass: Option[String] = None
        var tamiflexLog: Option[String] = None

        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        val writeCGRegex = "-writeCG=(.*)".r
        val writeTimingsRegex = "-writeTimings=(.*)".r
        val writePointsToSetsRegex = "-writePointsToSets=(.*)".r
        val mainClassRegex = "-main=(.*)".r
        val tamiflexLogRegex = "-tamiflex-log=(.*)".r

        parameters.foreach {
            case callersRegex(methodSig) ⇒ callersSigs ::= methodSig
            case calleesRegex(methodSig) ⇒ calleesSigs ::= methodSig
            case algorithmRegex(algo)    ⇒ cgAlgorithm = algo
            case writeCGRegex(fileName) ⇒
                if (cgFile.isEmpty)
                    cgFile = Some(fileName)
                else throw new IllegalArgumentException("-writeCG was set twice")
            case writeTimingsRegex(fileName) ⇒
                if (timingsFile.isEmpty)
                    timingsFile = Some(fileName)
                else throw new IllegalArgumentException("-writeTimings was set twice")
            case writePointsToSetsRegex(fileName) ⇒
                if (pointsToFile.isEmpty)
                    pointsToFile = Some(fileName)
                else throw new IllegalArgumentException("-writePointsToSets was set twice")
            case mainClassRegex(fileName) ⇒
                if (mainClass.isEmpty)
                    mainClass = Some(fileName)
                else throw new IllegalArgumentException("-main was set twice")
            case tamiflexLogRegex(fileName) ⇒
                if (tamiflexLog.isEmpty)
                    tamiflexLog = Some(fileName)
                else throw new IllegalArgumentException("-tamiflex-log was set twice")
        }

        var newConfig = if (tamiflexLog.isDefined)
            project.config.withValue(
                TamiFlexKey.configKey,
                ConfigValueFactory.fromAnyRef(tamiflexLog.get)
            )
        else
            project.config

        if (mainClass.isDefined)
            newConfig = null //todo implement this ???
        performAnalysis(
            Project.recreate(project, newConfig),
            calleesSigs,
            callersSigs,
            cgAlgorithm,
            cgFile,
            timingsFile,
            pointsToFile
        )
    }
}
