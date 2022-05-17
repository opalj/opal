/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.Project

import scala.collection.immutable.ArraySeq

/**
 * Tests that type annotations can be read.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TypeAnnotationsTest extends AnyFlatSpec with Matchers {

    import TypeAnnotationsTest._

    behavior of "OPAL when reading TypeAnnotations"

    it should "be able to correctly reify a type annotation on an implemented type" in {
        val classFileWithTypeAnnotations = taUserClassFile
        val typeAnnotations = classFileWithTypeAnnotations.runtimeInvisibleTypeAnnotations
        typeAnnotations should contain(
            TypeAnnotation(
                TAOfSupertype(1),
                TADirectlyOnType,
                aTA,
                ArraySeq(ElementValuePair("value", StringValue("superinterface")))
            )
        )
    }

    it should "be able to correctly reify a type annotation on the type parameter of an implemented type" in {
        val typeAnnotations = taUserClassFile.runtimeInvisibleTypeAnnotations
        typeAnnotations should contain(
            TypeAnnotation(
                TAOfSupertype(0),
                TAOnNestedType(ArraySeq(TAOnTypeArgument(0))),
                aTA,
                ArraySeq.empty
            )
        )
    }
}
private object TypeAnnotationsTest {

    //
    //
    // Setup
    //
    //

    val project = {
        val testResources = locateTestResources("type_annotations.jar", "bi")
        Project(ClassFiles(testResources), Iterable.empty, true)
    }

    //Classfile /Users/Michael/Code/OPAL/core/bin/type_annotations/RITypeAnnotationUser.class
    //Compiled from "RITypeAnnotationUser.java"
    //public abstract class type_annotations.RITypeAnnotationUser<T extends java.io.Serializable> extends java.lang.Object implements java.util.List<java.lang.Object>, java.io.Serializable
    //  Constant_Pool:
    //  ...
    //  #15 = Utf8               Ltype_annotations/RITypeAnnotation;
    //  ...
    //  SourceFile: "RITypeAnnotationUser.java"
    //  Signature: #42 // <T::Ljava/io/Serializable;>Ljava/lang/Object;Ljava/util/List<Ljava/lang/Object;>;Ljava/io/Serializable;
    //  RuntimeInvisibleTypeAnnotations:
    //    0: #15(): CLASS_EXTENDS, type_index=0, location=[TYPE_ARGUMENT(0)]
    //    1: #15(): CLASS_EXTENDS, type_index=1
    //    2: #15(): CLASS_TYPE_PARAMETER, param_index=0
    //    3: #15(): CLASS_TYPE_PARAMETER_BOUND, param_index=0, bound_index=1

    //  public java.util.List<T> ser;
    //    descriptor: Ljava/util/List;
    //    flags: ACC_PUBLIC
    //    Signature: #19                          // Ljava/util/List<TT;>;
    //    RuntimeInvisibleTypeAnnotations:
    //      0: #15(): FIELD, location=[TYPE_ARGUMENT(0)]
    //
    //  public java.lang.Object doSomething() throws java.lang.Exception;
    //    descriptor: ()Ljava/lang/Object;
    //    flags: ACC_PUBLIC
    //    Exceptions:
    //      throws java.lang.Exception
    //    Code:
    //      stack=2, locals=2, args_size=1
    //         0: new           #36                 // class java/util/ArrayList
    //         3: dup
    //         4: invokespecial #38                 // Method java/util/ArrayList."<init>":()V
    //         7: astore_1
    //         8: aload_1
    //         9: areturn
    //      LineNumberTable:
    //        line 54: 0
    //        line 56: 8
    //      LocalVariableTable:
    //        Start  Length  Slot  Name   Signature
    //            0      10     0  this   Ltype_annotations/RITypeAnnotationUser;
    //            8       2     1     l   Ljava/util/List;
    //      LocalVariableTypeTable:
    //        Start  Length  Slot  Name   Signature
    //            0      10     0  this   Ltype_annotations/RITypeAnnotationUser<TT;>;
    //            8       2     1     l   Ljava/util/List<TT;>;
    //      RuntimeInvisibleTypeAnnotations:
    //        0: #15(): NEW, offset=0
    //        1: #15(): LOCAL_VARIABLE, {start_pc=8, length=2, index=1}
    //        2: #15(): LOCAL_VARIABLE, {start_pc=8, length=2, index=1}, location=[TYPE_ARGUMENT(0)]
    //    RuntimeInvisibleTypeAnnotations:
    //      0: #15(): METHOD_RETURN
    //}
    val aTA = ObjectType("type_annotations/RITypeAnnotation")
    val taUser = ObjectType("type_annotations/RITypeAnnotationUser")
    val taUserClassFile = project.classFile(taUser).get

}
