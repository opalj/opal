/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf

import org.scalatest.Matchers
import org.scalatest.FunSpec
import java.net.URL
import java.io.File

import org.opalj.br.Type
import org.opalj.br.ObjectType
import org.opalj.br.Annotation
import org.opalj.br.StringValue
import org.opalj.br.ClassValue
import org.opalj.br.ElementValuePair
import org.opalj.bytecode.RTJar
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.AdvancedFieldMutabilityAnalysis

/**
 * Tests if the properties specified in test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Michael Eichberg
 */
class PropertiesTest extends FunSpec with Matchers {

    final val FixtureProject: Project[URL] = {
        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val fixtureFiles = new File("DEVELOPING_OPAL/validate/target/scala-2.11/test-classes")
        val fixtureClassFiles = ClassFiles(fixtureFiles)

        val projectClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/fixture")
        }

        val propertiesClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/properties")
        }
        val libraryClassFiles = ClassFiles(RTJar) ++ propertiesClassFiles

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
     * if the annotation specifies an expected property currently relevant; i.e,  if the
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
        annotation: Annotation
    ): Option[(Annotation, String, Type /* type of the matcher */ )] = {
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

    def validateProperties(
        context:       (Project[URL], PropertyStore, Set[FPCFAnalysis]),
        propertyKinds: Set[String]
    ): Unit = {
        val (p: Project[URL], ps: PropertyStore, as: Set[FPCFAnalysis]) = context
        val ats = as.map(a ⇒ ObjectType(a.getClass.getName.replace('.', '/')))

        for {
            // TODO Define a parameter to pass in the entities which we want to analyze.
            f ← p.allFields.par
            annotations = f.runtimeInvisibleAnnotations.flatMap(getPropertyMatcher(p, propertyKinds))
            (annotation, propertyKind, matcherType) ← annotations
        } {
            val annotationTypeName = annotation.annotationType.asObjectType.simpleName
            it(f.toJava(s"@$annotationTypeName").substring(24)) {
                val matcherClass = Class.forName(matcherType.toJava)
                info(s"using matcher: "+matcherClass.toString.substring(32))
                val matcherInstance = matcherClass.newInstance()
                val m = matcherClass.getMethod(
                    "hasProperty",
                    classOf[Project[_]], classOf[Set[_]], classOf[Object], classOf[Annotation], classOf[List[_]]
                )
                val properties = ps.properties(f)
                m.invoke(matcherInstance, p, ats, f, annotation, properties) match {
                    case Some(error: String) ⇒
                        fail(properties.map(_.toString).mkString("actual: ", ", ", "\nerror: ") + error)
                    case None   ⇒ /* OK */
                    case result ⇒ fail("matcher returned unexpected result: "+result)
                }
            }
        }
    }

    def executeAnalyses(
        analysisRunners: Set[FPCFAnalysisRunner]
    ): (Project[URL], PropertyStore, Set[FPCFAnalysis]) = {
        // IMPROVE Implement Project.recreate which gives a new independent project, but which share all immutable/analysis unspecific information.
        val p = Project.recreate(FixtureProject) // to ensure that this project is not "polluted"
        val ps = p.get(org.opalj.br.analyses.PropertyStoreKey)
        val as = analysisRunners.map(ar ⇒ ar.start(p, ps))
        ps.waitOnPropertyComputationCompletion(true, true)
        (p, ps, as)
    }

    //
    //
    // THE TESTS
    //
    //

    describe("analyzing the fixture project using the most primitive 'Field Mutability Analysis'") {
        val as = executeAnalyses(Set(FieldMutabilityAnalysis))
        validateProperties(as, Set("FieldMutability"))
    }

    describe("analyzing the fixture project using the ai based 'Field Mutability Analysis'") {
        val as = executeAnalyses(Set(AdvancedFieldMutabilityAnalysis))
        validateProperties(as, Set("FieldMutability"))
    }

}
