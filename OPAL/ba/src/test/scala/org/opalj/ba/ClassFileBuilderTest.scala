/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.postfixOps
import java.io.ByteArrayInputStream

import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.junit.runner.RunWith
import org.opalj.collection.immutable.UShortPair
import org.opalj.util.InMemoryClassLoader
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.MethodDescriptor
import org.opalj.br.reader.Java8Framework.{ClassFile ⇒ ClassFileReader}
import org.opalj.bc.Assembler
import org.opalj.collection.immutable.RefArray

/**
 * Tests general properties of a classes build with the BytecodeAssembler DSL by loading and
 * instantiating them with the JVM and doing a round trip `BRClassFile` -> `DAClassFile` ->
 * `Serialized Class File` -> `BRClassFile`.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class ClassFileBuilderTest extends AnyFlatSpec {

    behavior of "the ClassFileBuilder"

    val (markerInterface1, _) =
        CLASS[Nothing](accessModifiers = ABSTRACT INTERFACE, thisType = "MarkerInterface1").toDA()
    val (markerInterface2, _) =
        CLASS[Nothing](accessModifiers = ABSTRACT INTERFACE, thisType = "MarkerInterface2").toDA()

    val (abstractClass, _) =
        CLASS[Nothing](
            accessModifiers = ABSTRACT PUBLIC,
            thisType = "org/opalj/bc/AbstractClass"
        ).toDA()

    val (simpleConcreteClass, _) =
        CLASS[Nothing](
            // Note: Java 11 checks if the minor version is a known one!
            version = UShortPair(0, 49),
            accessModifiers = PUBLIC.SUPER.FINAL.SYNTHETIC,
            thisType = "ConcreteClass",
            superclassType = Some("org/opalj/bc/AbstractClass"),
            interfaceTypes = RefArray("MarkerInterface1", "MarkerInterface2"),
            attributes = RefArray(br.SourceFile("ClassFileBuilderTest.scala"), br.Synthetic)
        ).toDA()

    val abstractAsm = Assembler(abstractClass)
    val concreteAsm = Assembler(simpleConcreteClass)
    val abstractBRClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(abstractAsm)).head
    val concreteBRClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(concreteAsm)).head

    val loader = new InMemoryClassLoader(
        Map(
            "MarkerInterface1" → Assembler(markerInterface1),
            "MarkerInterface2" → Assembler(markerInterface2),
            "org.opalj.bc.AbstractClass" → abstractAsm,
            "ConcreteClass" → concreteAsm
        ),
        this.getClass.getClassLoader
    )
    import loader.loadClass

    "the generated classes" should "load correctly" in {
        assert("MarkerInterface1" == loadClass("MarkerInterface1").getSimpleName)
        assert("MarkerInterface2" == loadClass("MarkerInterface2").getSimpleName)
        assert("org.opalj.bc.AbstractClass" == loadClass("org.opalj.bc.AbstractClass").getName)
        assert("ConcreteClass" == loadClass("ConcreteClass").getSimpleName)
    }

    "the generated class 'ConcreteClass'" should "have only the generated default Constructor" in {
        val methods = concreteBRClassFile.methods
        assert(methods.size == 1)
        assert(methods.head.name == "<init>")
        assert(methods.head.descriptor == MethodDescriptor("()V"))
    }

    it should "be possible to create an instance" in {
        assert(loader.loadClass("ConcreteClass").getDeclaredConstructor().newInstance() != null)
    }

    it should "extend org/opalj/bc/AbstractClass" in {
        assert(concreteBRClassFile.superclassType.get.fqn == "org/opalj/bc/AbstractClass")
    }

    it should "implement MarkerInterface1 and MarkerInterface2" in {
        assert(concreteBRClassFile.interfaceTypes.map[String](i ⇒ i.fqn).contains("MarkerInterface1"))
        assert(concreteBRClassFile.interfaceTypes.map[String](i ⇒ i.fqn).contains("MarkerInterface2"))
    }

    it should "be public final synthetic (super)" in {
        assert(concreteBRClassFile.accessFlags ==
            (ACC_PUBLIC.mask | ACC_FINAL.mask | ACC_SYNTHETIC.mask | ACC_SUPER.mask))
    }

    it should "have the specified minor version: 0" in {
        assert(concreteBRClassFile.minorVersion == 0)
    }

    it should "have the specified major version: 49" in {
        assert(concreteBRClassFile.majorVersion == 49)
    }

    "the generated class 'AbstractClass'" should "have the default minor version" in {
        assert(abstractBRClassFile.minorVersion == CLASS.DefaultMinorVersion)
    }

    it should "have the default major version" in {
        assert(abstractBRClassFile.majorVersion == CLASS.DefaultMajorVersion)
    }
}
