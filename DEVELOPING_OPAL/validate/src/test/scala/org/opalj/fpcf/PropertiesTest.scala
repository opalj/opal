/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.scalatest.Matchers
import org.scalatest.FunSpec
import java.net.URL
import java.io.File

import org.opalj.log.LogContext
import org.opalj.ai.common.DefinitionSite
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.util.ScalaMajorVersion
import org.opalj.bytecode.RTJar
import org.opalj.br.Type
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.Annotation
import org.opalj.br.Annotations
import org.opalj.br.StringValue
import org.opalj.br.ClassValue
import org.opalj.br.ElementValuePair
import org.opalj.br.AnnotationLike
import org.opalj.br.TAOfNew
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.PropertyMatcher
import org.opalj.fpcf.seq.PKESequentialPropertyStore

/**
 * Framework to test if the properties specified in the test project (the classes in the
 * (sub-)package of org.opalj.fpcf.fixture) and the computed ones match. The actual matching
 * is delegated to `PropertyMatcher`s to facilitate matching arbitrary complex property
 * specifications.
 *
 * @author Michael Eichberg
 */
abstract class PropertiesTest extends FunSpec with Matchers {

    def withRT = false

    /**
     * The representation of the fixture project.
     */
    final val FixtureProject: Project[URL] = {
        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val sourceFolder = s"DEVELOPING_OPAL/validate/target/scala-$ScalaMajorVersion/test-classes"
        val fixtureFiles = new File(sourceFolder)
        val fixtureClassFiles = ClassFiles(fixtureFiles)
        if (fixtureClassFiles.isEmpty) fail(s"no class files at $fixtureFiles")

        val projectClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/fixtures")
        }

        val propertiesClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/properties")
        }

        val libraryClassFiles = (if (withRT) ClassFiles(RTJar) else List()) ++ propertiesClassFiles

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false
        )
    }

    final val PropertyValidatorType = ObjectType("org/opalj/fpcf/properties/PropertyValidator")

    /**
     * Returns the [[org.opalj.fpcf.properties.PropertyMatcher]] associated with the annotation -
     * if the annotation specifies an expected property currently relevant; i.e, if the
     * property kind specified by the annotation is in the set `propertyKinds`.
     *
     * @param p The current project.
     * @param propertyKinds The property kinds which should be analyzed.
     * @param annotation The annotation found in the project.
     */
    def getPropertyMatcher(
        p:             Project[URL],
        propertyKinds: Set[String]
    )(
        annotation: AnnotationLike
    ): Option[(AnnotationLike, String, Type /* type of the matcher */ )] = {
        if (!annotation.annotationType.isObjectType)
            return None;

        // Get the PropertyValidator meta-annotation of the given entity's annotation:
        val annotationClassFile = p.classFile(annotation.annotationType.asObjectType).get
        annotationClassFile.runtimeInvisibleAnnotations.collectFirst {
            case Annotation(
                PropertyValidatorType,
                Seq(
                    ElementValuePair("key", StringValue(propertyKind)),
                    ElementValuePair("validator", ClassValue(propertyMatcherType))
                    )
                ) if propertyKinds.contains(propertyKind) ⇒
                (annotation, propertyKind, propertyMatcherType)
        }
    }

    /**
     * Called by tests to trigger the validation of the derived properties against the
     * specified ones.
     *
     * @param  context The validation context; typically the return value of [[executeAnalyses]].
     * @param  eas An iterator over the relevant entities along with the found annotations.
     * @param  propertyKinds The kinds of properties (as specified by the annotations) that are
     *         to be tested.
     */
    def validateProperties(
        context:       TestContext,
        eas:           TraversableOnce[(Entity, /*the processed annotation*/ String ⇒ String /* a String identifying the entity */ , Traversable[AnnotationLike])],
        propertyKinds: Set[String]
    ): Unit = {
        val TestContext(p: Project[URL], ps: PropertyStore, as: List[FPCFAnalysis]) = context
        val ats =
            as.map(a ⇒ ObjectType(a.getClass.getName.replace('.', '/'))).toSet

        for {
            (e, entityIdentifier, annotations) ← eas
            augmentedAnnotations = annotations.flatMap(getPropertyMatcher(p, propertyKinds))
            (annotation, propertyKind, matcherType) ← augmentedAnnotations
        } {
            val annotationTypeName = annotation.annotationType.asObjectType.simpleName
            val matcherClass = Class.forName(matcherType.toJava)
            val matcherClassConstructor = matcherClass.getDeclaredConstructor()
            val matcher = matcherClassConstructor.newInstance().asInstanceOf[PropertyMatcher]
            if (matcher.isRelevant(p, ats, e, annotation)) {

                it(entityIdentifier(s"$annotationTypeName")) {
                    info(s"validator: "+matcherClass.toString.substring(32))
                    val epss = ps.properties(e).toIndexedSeq
                    val nonFinalPSs = epss.filter(!_.isFinal)
                    assert(
                        nonFinalPSs.isEmpty,
                        nonFinalPSs.mkString("some epss are not final:\n\t", "\n\t", "\n")
                    )
                    val properties = epss.map(_.toFinalEP.p)
                    matcher.validateProperty(p, ats, e, annotation, properties) match {
                        case Some(error: String) ⇒
                            val propertiesAsStrings = properties.map(_.toString)
                            val m = propertiesAsStrings.mkString(
                                "actual: ",
                                ", ",
                                "\nexpectation: "+error
                            )
                            fail(m)
                        case None   ⇒ /* OK */
                        case result ⇒ fail("matcher returned unexpected result: "+result)
                    }
                }

            }
        }
    }

    //
    // CONVENIENCE METHODS
    //

    def fieldsWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(Field, String ⇒ String, Annotations)] = {
        for {
            f ← recreatedFixtureProject.allFields // cannot be parallelized; "it" is not thread safe
            annotations = f.runtimeInvisibleAnnotations
            if annotations.nonEmpty
        } yield {
            (f, (a: String) ⇒ f.toJava(s"@$a").substring(24), annotations)
        }
    }

    def methodsWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(Method, String ⇒ String, Annotations)] = {
        for {
            // cannot be parallelized; "it" is not thread safe
            m ← recreatedFixtureProject.allMethods
            annotations = m.runtimeInvisibleAnnotations
            if annotations.nonEmpty
        } yield {
            (m, (a: String) ⇒ m.toJava(s"@$a").substring(24), annotations)
        }
    }

    def declaredMethodsWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(DefinedMethod, String ⇒ String, Annotations)] = {
        val declaredMethods = recreatedFixtureProject.get(DeclaredMethodsKey)
        for {
            // cannot be parallelized; "it" is not thread safe
            m ← recreatedFixtureProject.allMethods
            dm = declaredMethods(m)
            annotations = m.runtimeInvisibleAnnotations
            if annotations.nonEmpty
        } yield {
            (
                dm,
                (a: String) ⇒ m.toJava(s"@$a").substring(24),
                annotations
            )
        }
    }

    def classFilesWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(ClassFile, String ⇒ String, Annotations)] = {
        for {
            // cannot be parallelized; "it" is not thread safe
            cf ← recreatedFixtureProject.allClassFiles
            annotations = cf.runtimeInvisibleAnnotations
            if annotations.nonEmpty
        } yield {
            (cf, (a: String) ⇒ cf.thisType.toJava.substring(24) + s"@$a", annotations)
        }
    }

    // there can't be any annotations of the implicit "this" parameter...
    def explicitFormalParametersWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(VirtualFormalParameter, String ⇒ String, Annotations)] = {
        val formalParameters = recreatedFixtureProject.get(VirtualFormalParametersKey)
        val declaredMethods = recreatedFixtureProject.get(DeclaredMethodsKey)
        for {
            // cannot be parallelized; "it" is not thread safe
            m ← recreatedFixtureProject.allMethods
            parameterAnnotations = m.runtimeInvisibleParameterAnnotations
            i ← parameterAnnotations.indices
            annotations = parameterAnnotations(i)
            if annotations.nonEmpty
            dm = declaredMethods(m)
        } yield {
            val fp = formalParameters(dm)(i + 1)
            (
                fp,
                (a: String) ⇒ s"VirtualFormalParameter: (origin ${fp.origin} in "+
                    s"${dm.declaringClassType}#${m.toJava(s"@$a")}",
                annotations
            )
        }
    }

    def allocationSitesWithAnnotations(
        recreatedFixtureProject: SomeProject
    ): Traversable[(DefinitionSite, String ⇒ String, Traversable[AnnotationLike])] = {
        val allocationSites = recreatedFixtureProject.get(DefinitionSitesKey).getAllocationSites
        for {
            as ← allocationSites
            m = as.method
            pc = as.pc
            code = m.body.get
            annotations = code.runtimeInvisibleTypeAnnotations filter { ta ⇒
                ta.target match {
                    case TAOfNew(`pc`) ⇒ true
                    case _             ⇒ false
                }
            }
            if annotations.nonEmpty
        } yield {
            (
                as,
                (a: String) ⇒ s"AllocationSite: (pc ${as.pc} in "+
                    s"${m.toJava(s"@$a").substring(24)})",
                annotations
            )
        }
    }

    def init(p: Project[URL]): Unit = {}

    def executeAnalyses(
        analysisRunners: ComputationSpecification[FPCFAnalysis]*
    ): TestContext = {
        executeAnalyses(analysisRunners.toIterable)
    }

    def executeAnalyses(
        analysisRunners: Iterable[ComputationSpecification[FPCFAnalysis]]
    ): TestContext = {
        require(analysisRunners.nonEmpty)

        val p = FixtureProject.recreate { piKeyUnidueId ⇒
            piKeyUnidueId != PropertyStoreKey.uniqueId
        } // to ensure that this project is not "polluted"
        implicit val logContext: LogContext = p.logContext
        init(p)

        PropertyStore.updateDebug(true)

        p.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                /*
                val ps = PKEParallelTasksPropertyStore.create(
                    new RecordAllPropertyStoreTracer,
                    context.iterator.map(_.asTuple).toMap
                )
                */
                val ps = PKESequentialPropertyStore(context: _*)
                ps
            }
        )

        val ps = p.get(PropertyStoreKey)

        val (_, as) = p.get(FPCFAnalysesManagerKey).runAll(analysisRunners)
        TestContext(p, ps, as)
    }
}

case class TestContext(
        project:       Project[URL],
        propertyStore: PropertyStore,
        analyses:      List[FPCFAnalysis]
)
