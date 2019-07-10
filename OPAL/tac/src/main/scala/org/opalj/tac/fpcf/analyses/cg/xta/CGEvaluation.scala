/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.file.Paths

import scala.collection.mutable

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import org.opalj.log.GlobalLogContext
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.ReferenceType
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.XTACallGraphKey

object CGEvaluation {
    def main(args: Array[String]): Unit = {
        def getArgOrElse(index: Int, alt: String): String = {
            if (index >= args.length)
                alt
            else
                args(index)
        }

        val testJar = getArgOrElse(0, "C:\\Users\\Andreas\\Dropbox\\Masterarbeit\\testjars\\bantamc-gruppe7.jar")

        val algo = getArgOrElse(1, "xta")

        val defaultOutDir = "C:\\Users\\Andreas\\Dropbox\\Masterarbeit\\rta-vs-xta"
        val outDir = getArgOrElse(2, defaultOutDir)

        execute(testJar, algo, outDir)
    }

    case class CGConstructionResult(
            projectName:       String,
            cgFile:            File,
            instantiatedTypes: Map[Entity, UIDSet[ReferenceType]],
            projectStatistics: Map[String, Int],
            creationRuntime:   Nanoseconds
    )

    def execute(testJar: String, algo: String, outDir: String): CGConstructionResult = {
        execute(new File(testJar), Array.empty, Array.empty, algo, outDir)
    }

    def execute(mainProjectFile: File, otherProjectFiles: Array[File], libraryFiles: Array[File], algo: String, outDir: String): CGConstructionResult = {

        val projectName =
            if (mainProjectFile.getName == "bin.zip") { // for xcorpus projects
                mainProjectFile.getParentFile.getParentFile.getName
            } else {
                mainProjectFile.getName
            }

        val algoKey = algo match {
            case "cha" ⇒ CHACallGraphKey
            case "rta" ⇒ RTACallGraphKey
            case "xta" ⇒ XTACallGraphKey
            case _     ⇒ sys.error("invalid cg algorithm!")
        }

        val outFileName = s"$projectName-$algo.json"
        val outFile = Paths.get(outDir, outFileName).toFile

        // Application mode!
        val cfg = ConfigFactory.load().withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
        ).withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+"analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder")
            )

        val project = Project(mainProjectFile +: otherProjectFiles, libraryFiles, GlobalLogContext, cfg)

        val (creationTimeNs, cg) = PerformanceEvaluation.timed {
            project.get(algoKey)
        }

        val statistics = project.statistics.toMap

        // Dump per-entity types for XTA.
        val instantiatedTypes =
            if (algo == "xta") {
                val ps = project.get(PropertyStoreKey)
                val map = Map.newBuilder[Entity, UIDSet[ReferenceType]]
                for (eps ← ps.entities(InstantiatedTypes.key)) {
                    map += (eps.e → ps(eps.e, InstantiatedTypes.key).ub.types)
                }
                map.result()
            } else {
                Map.empty[Entity, UIDSet[ReferenceType]]
            }

        implicit val dm: DeclaredMethods = project.get(DeclaredMethodsKey)
        CallGraphSerializer.writeCG(cg, outFile)

        CGConstructionResult(projectName, outFile, instantiatedTypes, statistics, creationTimeNs)
    }

    def dumpInstantiatedTypes(projectName: String, instantiatedTypes: Map[Entity, UIDSet[ReferenceType]], outDir: String): Unit = {
        val typesOutFile = Paths.get(outDir, s"$projectName-xta-types.txt").toFile
        val writer = new PrintWriter(new FileOutputStream(typesOutFile))

        for ((entity, types) ← instantiatedTypes) {
            writer.println(entity)
            for (t ← types) {
                writer.println(s"-> $t")
            }
            writer.println()
        }
        writer.flush()
        writer.close()
    }
}

// TODO AB for debugging; remove later
object TACTestRunner {
    def main(args: Array[String]): Unit = {
        val testJar = "C:/Users/Andreas/Dropbox/Masterarbeit/javastuff/out/artifacts/javastuff_jar/javastuff.jar"
        val project = Project(new File(testJar), GlobalLogContext, ConfigFactory.load())
        val tacai = project.get(ComputeTACAIKey)
        val declaredMethods = project.get(DeclaredMethodsKey)
        val taCodes = mutable.Map[Method, AITACode[TACMethodParameter, ValueInformation]]()
        val fieldReads = mutable.Map[Method, Set[Field]]()
        val fieldWrites = mutable.Map[Method, Set[Field]]()
        for (m ← project.allMethods) {
            val taCode = tacai(m)
            taCodes += m -> taCode

            val (reads, writes) = findFieldAccessesInBytecode(project, declaredMethods(m))
            fieldReads += m -> reads
            fieldWrites += m -> writes
        }

        scala.io.StdIn.readChar()
    }

    def findFieldAccessesInBytecode(project: Project[_], method: DefinedMethod): (Set[Field], Set[Field]) = {
        val code = method.definedMethod.body.get
        val reads = code.instructions.flatMap {
            case FieldReadAccess(objType, name, fieldType) ⇒
                project.resolveFieldReference(objType, name, fieldType)
            case _ ⇒
                None
        }.toSet
        val writes = code.instructions.flatMap {
            case FieldWriteAccess(objType, name, fieldType) ⇒
                project.resolveFieldReference(objType, name, fieldType)
            case _ ⇒
                None
        }.toSet

        (reads, writes)
    }
}
