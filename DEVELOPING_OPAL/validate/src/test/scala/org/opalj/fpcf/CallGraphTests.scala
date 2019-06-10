/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.log.GlobalLogContext
import org.opalj.tac.AITACode
import org.opalj.tac.ComputeTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.InstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.ConstructorCallInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTACallGraphAnalysisScheduler
import org.opalj.value.ValueInformation

import scala.collection.JavaConverters._
import scala.collection.mutable

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
        for (m <- project.allMethods) {
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
            case FieldReadAccess(objType, name, fieldType) =>
                project.resolveFieldReference(objType, name, fieldType)
            case _ =>
                None
        }.toSet
        val writes = code.instructions.flatMap {
            case FieldWriteAccess(objType, name, fieldType) =>
                project.resolveFieldReference(objType, name, fieldType)
            case _ =>
                None
        }.toSet

        (reads, writes)
    }
}

/**
 * Tests if the computed call graph contains (at least!) the expected call edges.
 *
 * @author Andreas Bauer
 */
class CallGraphTests extends PropertiesTest {

    override def createConfig(): Config = {
        val baseConfig = super.createConfig()
        // For these tests, we want to restrict entry points to "main" methods.
        // Also, no types should be instantiated by default.
        baseConfig.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
        ).withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+"analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder")
            )

        //configuredEntryPoint(baseConfig, "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows", "main")
    }

    // for testing
    // TODO AB debug stuff, remove later
    def configuredEntryPoint(cfg: Config, declClass: String, name: String): Config = {
        val ep = ConfigValueFactory.fromMap(Map("declaringClass" -> declClass, "name" -> name).asJava)
        cfg.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder")
        ).withValue(
                InitialEntryPointsKey.ConfigKeyPrefix+"entryPoints",
                ConfigValueFactory.fromIterable(Seq(ep).asJava)
            )
    }

    describe("the RTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                InstantiatedTypesAnalysisScheduler,
                RTACallGraphAnalysisScheduler
            )
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }

    describe("the CHA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(CHACallGraphAnalysisScheduler)
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }

    describe("the XTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                ConstructorCallInstantiatedTypesAnalysisScheduler,
                XTACallGraphAnalysisScheduler
            )
        )
        as.propertyStore.shutdown()

        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project) ++ fieldsWithAnnotations(as.project),
            Set("AvailableTypes")
        )
    }
}
