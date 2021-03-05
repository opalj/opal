/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses

import java.io.File
import java.net.URL

import scala.collection.JavaConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import heros.solver.IFDSSolver
import org.opalj.BaseConfig

import org.opalj.util.ScalaMajorVersion
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalForwardICFG
import org.opalj.tac.fpcf.analyses.ifds.CalleeType
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysis
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.VariableType
import org.opalj.tac.fpcf.analyses.ifds.VTAFact
import org.opalj.tac.fpcf.analyses.ifds.VTANullFact
import org.opalj.tac.fpcf.analyses.ifds.VTAResult
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue

object VTAEquality {

    def main(args: Array[String]): Unit = {
        val herosSolver = performHerosAnalysis
        val opalResult = performOpalAnalysis

        opalResult.keys.foreach {
            /*
       * Heros only gives us results for call statements.
       * Therefore, we cannot consider return statements.
       * Heros also gives us the facts before the call statements, while Opal gives us the facts after.
       * Therefore, we cannot consider assignments of objects, which were instantiated using a constructor,
       * because the VariableTypeFact holds after the statement.
       */
            case statement if statement.stmt.astID != Return.ASTID && statement.stmt.astID != ReturnValue.ASTID
                && (statement.stmt.astID != Assignment.ASTID || statement.stmt.asAssignment.expr.astID != New.ASTID) ⇒
                val opalFacts = opalResult(statement)
                /*
         * ifdsResultsAt returns the facts before the statements.
         * However, CalleeType facts hold after the statement and are therefore not returned.
         * We do not consider them in this test.
         */
                val herosFacts =
                    herosSolver.ifdsResultsAt(statement).asScala.filter(!_.isInstanceOf[CalleeType])
                if (opalFacts.size != herosFacts.size) {
                    println("Error: Different number of facts:")
                    println(s"Statement: $statement")
                    println(s"Opal: $opalFacts")
                    println(s"Heros: $herosFacts")
                    println
                } else
                    opalFacts.filter(!herosFacts.contains(_)).foreach { fact ⇒
                        println("Error: Heros fact missing:")
                        println(s"Statement: $statement")
                        println(s"Fact: $fact")
                        println(s"Opal: $opalFacts")
                        println(s"Heros: $herosFacts")
                        println
                    }
            case _ ⇒
        }
    }

    private def performHerosAnalysis = {
        val project = FixtureProject.recreate { piKeyUnidueId ⇒
            piKeyUnidueId != PropertyStoreKey.uniqueId
        }
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
            case Some(requirements) ⇒
                requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        }
        project.get(RTACallGraphKey)
        val initialMethods =
            project.allProjectClassFiles
                .filter(_.fqn.startsWith("org/opalj/fpcf/fixtures/vta"))
                .flatMap(classFile ⇒ classFile.methods)
                .filter(isEntryPoint)
                .map(method ⇒ method -> entryPointsForMethod(method).asJava)
                .toMap
        val cgf = new OpalForwardICFG(project)
        val herosAnalysis = new HerosVariableTypeAnalysis(project, cgf, initialMethods)
        val herosSolver = new IFDSSolver(herosAnalysis)
        herosSolver.solve()
        herosSolver
    }

    private def performOpalAnalysis = {
        val project = FixtureProject.recreate { piKeyUnidueId ⇒
            piKeyUnidueId != PropertyStoreKey.uniqueId
        }
        val propertyStore = project.get(PropertyStoreKey)
        var result = Map.empty[Statement, Set[VTAFact]]
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
            case Some(requirements) ⇒
                requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        }
        project.get(RTACallGraphKey)
        project.get(FPCFAnalysesManagerKey).runAll(IFDSBasedVariableTypeAnalysis)
        propertyStore
            .entities(IFDSBasedVariableTypeAnalysis.property.key)
            .collect {
                case EPS((m: DefinedMethod, inputFact)) ⇒
                    (m, inputFact)
            }
            .foreach { entity ⇒
                val entityResult = propertyStore(entity, IFDSBasedVariableTypeAnalysis.property.key) match {
                    case FinalEP(_, VTAResult(map)) ⇒ map
                    case _                          ⇒ throw new RuntimeException
                }
                entityResult.keys.foreach { statement ⇒
                    /*
           * Heros returns the facts before the statements.
           * However, CalleeType facts hold after the statement and are therefore not returned.
           * We do not consider them in this test.
           * Additionally, Heros does not return null facts, so we filter them.
           */
                    result = result.updated(
                        statement,
                        result.getOrElse(statement, Set.empty) ++ entityResult(statement)
                            .filter(fact ⇒ fact != VTANullFact && !fact.isInstanceOf[CalleeType])
                    )
                }
            }
        result
    }

    private def isEntryPoint(
        method: Method
    )(implicit declaredMethods: DeclaredMethods, propertyStore: PropertyStore): Boolean = {
        val declaredMethod = declaredMethods(method)
        val FinalEP(_, callers) = propertyStore(declaredMethod, Callers.key)
        method.body.isDefined && callers.hasCallersWithUnknownContext
    }

    private def entryPointsForMethod(method: Method): Set[VTAFact] = {
        (method.descriptor.parameterTypes.zipWithIndex.collect {
            case (t, index) if t.isReferenceType ⇒
                VariableType(
                    switchParamAndVariableIndex(index, method.isStatic),
                    t.asReferenceType,
                    upperBound = true
                )
        } :+ VTANullFact).toSet
    }

    private def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -2 else -1) - index

    final val FixtureProject: Project[URL] = {
        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val sourceFolder = s"DEVELOPING_OPAL/validate/target/scala-$ScalaMajorVersion/test-classes"
        val fixtureFiles = new File(sourceFolder)
        val fixtureClassFiles = ClassFiles(fixtureFiles)

        val projectClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/fixtures")
        }

        val propertiesClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/properties")
        }

        val libraryClassFiles = propertiesClassFiles

        val configForEntryPoints = BaseConfig
            .withValue(
                InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.AllEntryPointsFinder")
            )
            .withValue(
                InitialEntryPointsKey.ConfigKeyPrefix+"AllEntryPointsFinder.projectMethodsOnly",
                ConfigValueFactory.fromAnyRef(true)
            )

        implicit val config: Config = configForEntryPoints
            .withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+"analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.AllInstantiatedTypesFinder")
            )
            .withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+
                    "AllInstantiatedTypesFinder.projectClassesOnly",
                ConfigValueFactory.fromAnyRef(true)
            )

        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false,
            virtualClassFiles = Traversable.empty
        )
    }

}
