/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.io.File
import java.net.URL

import org.opalj.collection.immutable.ConstArray
import org.opalj.util.ScalaMajorVersion
import org.scalatest.Matchers
import org.scalatest.FunSpec

/**
 * Tests whether the DeclaredMethodsKey creates the correct declared method objects for each class
 *
 * @author Dominik Helm
 */
class DeclaredMethodsKeyTest extends FunSpec with Matchers {

    val singleAnnotationType =
        ObjectType("org/opalj/br/analyses/properties/declared_methods/DeclaredMethod")
    val multiAnnotationType =
        ObjectType("org/opalj/br/analyses/properties/declared_methods/DeclaredMethods")

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
            cf.thisType.packageName.startsWith("org/opalj/br/analyses/fixtures/declared_methods")
        }

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(projectClassFiles)
    }

    val declaredMethodsKey: DeclaredMethods = FixtureProject.get(DeclaredMethodsKey)
    val numDeclaredMethods: Int = declaredMethodsKey.declaredMethods.size

    val declaredMethods: Set[DeclaredMethod] = declaredMethodsKey.declaredMethods.toSet

    it("should not have duplicate declared methods") {
        assert(declaredMethods.size == numDeclaredMethods, "duplicate methods found")
    }

    var annotated: Set[DeclaredMethod] = Set.empty

    for (cf ← FixtureProject.allProjectClassFiles) {
        val classType = cf.thisType
        it(classType.simpleName) {
            val annotations = cf.runtimeInvisibleAnnotations
            if (annotations.nonEmpty)
                for {
                    annotation ← annotations
                    annotationType = annotation.annotationType
                } {
                    if (annotationType == singleAnnotationType)
                        checkDeclaredMethod(classType, annotation)
                    else if (annotationType == multiAnnotationType) {
                        for (value ← getValue(annotation, "value").asArrayValue.values) {
                            val annotation = value.asAnnotationValue.annotation
                            checkDeclaredMethod(classType, annotation)
                        }
                    }
                }
        }
    }

    // The number of defined methods should be the same as the number of annotations processed, i.e.
    // there may not be any additional defined method. There may be VirtualDeclaredMethods for
    // predefined methods from the JDK, though.
    it("should not create excess declared methods") {
        val excessMethods = declaredMethods.filter(m ⇒ m.hasSingleDefinedMethod && !annotated.contains(m))
        if (excessMethods.nonEmpty)
            fail(
                "fonud unexpected methods: \n\t"+excessMethods.mkString("\n\t")
            )
    }

    def checkDeclaredMethod(classType: ObjectType, annotation: Annotation): Unit = {
        val name = getValue(annotation, "name").asStringValue.value
        val descriptor = MethodDescriptor(getValue(annotation, "descriptor").asStringValue.value)
        val declaringClasses = getValue(annotation, "declaringClass").asArrayValue.values

        val methodOs = declaringClasses map { declClass ⇒
            val classType = declClass.asClassValue.value.asObjectType
            (classType, FixtureProject.classFile(classType).get.findMethod(name, descriptor))
        }

        val emptyMethodO = methodOs.find(_._2.isEmpty)
        if (emptyMethodO.isDefined)
            fail(s"method ${emptyMethodO.get._1.simpleName}.${descriptor.toJava(name)}"+
                "not found in fixture project")

        val expected =
            if (methodOs.size == 1) DefinedMethod(classType, methodOs.head._2.get)
            else MultipleDefinedMethods(classType, ConstArray(methodOs.map(_._2.get).toSeq: _*))

        if (declaredMethods.contains(expected))
            annotated += expected
        else
            fail(
                s"No declared method for ${classType.simpleName}: $expected"
            )
    }

    def getValue(a: Annotation, name: String): ElementValue = {
        a.elementValuePairs.collectFirst { case ElementValuePair(`name`, value) ⇒ value }.get
    }
}
