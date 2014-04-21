/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st.bat
package resolved

import reader.Java8Framework.ClassFiles
import analyses.Project
import instructions._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

/**
 * Tests some of the core methods of the Code attribute.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TypeAnnotationsTest
        extends FlatSpec
        with Matchers /*with BeforeAndAfterAll */
        with ParallelTestExecution {

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
                IndexedSeq.empty
            )
        )
    }

    it should "be able to correctly reify a type annotation on the type parameter of an implemented type" in {
        val typeAnnotations = taUserClassFile.runtimeInvisibleTypeAnnotations
        typeAnnotations should contain(
            TypeAnnotation(
                TAOfSupertype(0),
                TAOnNestedType(IndexedSeq(TAOnTypeArgument(0))),
                aTA,
                IndexedSeq.empty
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

    val project =
        Project(
            ClassFiles(TestSupport.locateTestResources("classfiles/TypeAnnotations.jar"))
        )

    //Classfile /Users/Michael/Code/OPAL/core/bin/type_annotations/ATypeAnnotationUser.class
    //Compiled from "ATypeAnnotationUser.java"
    //public abstract class type_annotations.ATypeAnnotationUser<T extends java.io.Serializable> extends java.lang.Object implements java.util.List<java.lang.Object>, java.io.Serializable
    //  Constant_Pool:
    //  ...
    //  #15 = Utf8               Ltype_annotations/ATypeAnnotation;
    //  ...
    //  SourceFile: "ATypeAnnotationUser.java"
    //  Signature: #42                          // <T::Ljava/io/Serializable;>Ljava/lang/Object;Ljava/util/List<Ljava/lang/Object;>;Ljava/io/Serializable;
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
    //            0      10     0  this   Ltype_annotations/ATypeAnnotationUser;
    //            8       2     1     l   Ljava/util/List;
    //      LocalVariableTypeTable:
    //        Start  Length  Slot  Name   Signature
    //            0      10     0  this   Ltype_annotations/ATypeAnnotationUser<TT;>;
    //            8       2     1     l   Ljava/util/List<TT;>;
    //      RuntimeInvisibleTypeAnnotations:
    //        0: #15(): NEW, offset=0
    //        1: #15(): LOCAL_VARIABLE, {start_pc=8, length=2, index=1}
    //        2: #15(): LOCAL_VARIABLE, {start_pc=8, length=2, index=1}, location=[TYPE_ARGUMENT(0)]
    //    RuntimeInvisibleTypeAnnotations:
    //      0: #15(): METHOD_RETURN
    //}
    val aTA = ObjectType("type_annotations/ATypeAnnotation")
    val taUser = ObjectType("type_annotations/ATypeAnnotationUser")
    val taUserClassFile = project.classFile(taUser).get

}
