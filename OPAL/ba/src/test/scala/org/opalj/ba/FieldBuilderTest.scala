/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.postfixOps

import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import org.opalj.bc.Assembler
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.instructions.*
import org.opalj.br.reader.Java8Framework
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the properties of fields build with the BytecodeAssembler DSL. The class is build,
 * assembled as a [[org.opalj.da.ClassFile]] and read again as a [[org.opalj.br.ClassFile]]. It is
 * also loaded, instantiated and executed with the JVM.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class FieldBuilderTest extends AnyFlatSpec {

    behavior of "Fields"

    val binaryClassName = "test/FieldClass"
    val (daClassFile, _) =
        CLASS(
            accessModifiers = SUPER PUBLIC,
            thisType = binaryClassName,
            fields = FIELDS(
                FIELD(FINAL PUBLIC, "publicField", "I"),
                FIELD(PRIVATE, "privateField", "Z")
            ),
            methods = METHODS(
                METHOD(
                    PUBLIC,
                    "<init>",
                    "()V",
                    CODE(
                        ALOAD_0,
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        ALOAD_0,
                        ICONST_3,
                        PUTFIELD("test/FieldClass", "publicField", "I"),
                        ALOAD_0,
                        ICONST_1,
                        PUTFIELD("test/FieldClass", "privateField", "Z"),
                        RETURN
                    )
                ),
                METHOD(
                    PUBLIC,
                    "packageField",
                    "()Z",
                    CODE(
                        ALOAD_0,
                        GETFIELD("test/FieldClass", "privateField", "Z"),
                        IRETURN
                    )
                ),
                METHOD(
                    PUBLIC,
                    "publicField",
                    "()I",
                    CODE(
                        ALOAD_0,
                        GETFIELD("test/FieldClass", "publicField", "I"),
                        IRETURN
                    )
                )
            )
        ).toDA()

    val rawClassFile = Assembler(daClassFile)
    val javaClassName = binaryClassName.replace('/', '.')
    val loader = new InMemoryClassLoader(
        Map(javaClassName -> rawClassFile),
        this.getClass.getClassLoader
    )

    val clazz = loader.loadClass(javaClassName)
    val fieldInstance = clazz.getDeclaredConstructor().newInstance()

    val brClassFile = Java8Framework.ClassFile(() => new ByteArrayInputStream(rawClassFile)).head

    def getField(name: String) = brClassFile.fields.find(f => f.name == name).get

    "the fields in `FieldClass`" should "have the correct visibility modifiers" in {
        assert(getField("privateField").accessFlags == ACC_PRIVATE.mask)
        assert(getField("publicField").accessFlags == (ACC_PUBLIC.mask | ACC_FINAL.mask))
    }

    "the field `FieldClass.privateField`" should "be initialized as true" in {
        val field = clazz.getDeclaredField("privateField")
        field.setAccessible(true)
        assert(field.get(fieldInstance) == true)
    }

    "FieldClass.publicField" should "be initialized as 3" in {
        val field = clazz.getDeclaredField("publicField")
        assert(field.get(fieldInstance) == 3)
    }

}
