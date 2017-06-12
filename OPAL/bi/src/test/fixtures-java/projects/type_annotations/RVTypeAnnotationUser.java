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
package type_annotations;

import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class tests some (corner) cases related to type annotations.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public abstract class RVTypeAnnotationUser<@RVTypeAnnotation T extends @RVTypeAnnotation Serializable & Cloneable>
        implements List<@RVTypeAnnotation Object>, @RVTypeAnnotation("superinterface") Serializable {

    public @RVTypeAnnotation("on nested class declaration") class Nested {
        public @RVTypeAnnotation("on nested class declaration") class SubNested {
            public @RVTypeAnnotation("on nested class declaration") class SubSubNested {
            }
        }
    }

    private static final Object process(Object o) {return o;} // just a helper method

    private static final @RVTypeAnnotation("field declaration") long serialVersionUID = 1L;

    public List<@RVTypeAnnotation("annotation of generic type parameter") T> ser;

    @SuppressWarnings("all")
    public Object onNestedTypeAnnotations() throws Exception {
        Nested nested =  new RVTypeAnnotationUser.@RVTypeAnnotation("on nested class instantiation") Nested();
        Nested.SubNested subNested = nested.new @RVTypeAnnotation("on nested^2 class instantiation") SubNested();
        Nested.SubNested.SubSubNested subSubNested = subNested.new @RVTypeAnnotation("on nested^2 class instantiation") SubSubNested();

        return subSubNested;
    }

    public Object localVariableDeclarationTypeAnnotations() throws Exception {
        @RVTypeAnnotation("local variable annotation")
        List<@RVTypeAnnotation("type parameter annotation") T> l = new ArrayList<>();
        return l;
    }

    public void wildcardsRelatedTypeAnnotation() throws Exception {
        List<@RVTypeAnnotation ? extends Cloneable> l = new ArrayList<>();
        process(l);
    }

    @SuppressWarnings("unchecked")
    public void arrayRelatedTypeAnnotation() throws Exception {
        @RVTypeAnnotation("array as such") int[] @RVTypeAnnotation("2nd dimension of array")[] ls = new int[10][1];
        process(ls);

        List<@RVTypeAnnotation("type parameter annotation of array of generic type") Serializable>[] lgs = new List[10];
        process(lgs);
    }

    public Supplier<Vector<?>> instanceCreationRelatedTypeAnnotation() throws Exception {
        List<?> l = new @RVTypeAnnotation ArrayList<>();
        process(l);
        return (@RVTypeAnnotation Vector::new);
    }

    public Supplier<Object> methodCallRelatedTypeAnnotation() throws Exception {
        return (@RVTypeAnnotation("receiver class") System::lineSeparator);
    }

    public @RVTypeAnnotation("return type") Function<@RVTypeAnnotation("first parameter of generic type") Object,  @RVTypeAnnotation("second parameter of generic type") Integer> methodSignatureRelatedTypeAnnotations() throws Exception {
        List<Object> l = new ArrayList<>();
        return (l::indexOf);
    }

    @SuppressWarnings("all")
    public <X extends @RVTypeAnnotation Serializable & @RVTypeAnnotation("annotation of second type of intersection type") Cloneable> void typeCheckRelatedTypeAnnotations(X x) {
        // annotated type test
        if(x instanceof @RVTypeAnnotation("annotated instanceof") List) {
            List<X> l = (@RVTypeAnnotation("annotated type cast") List< @RVTypeAnnotation("annotated generic type variable in (unsafe) type cast")X>) x;
            process(l);
        }

        // annotated type cast
        if(x instanceof List) {
            Object l = (Serializable & @RVTypeAnnotation("annotation of second type of a case to an intersection type") Cloneable) x;
            process(l);
        }
    }

    public String doIt(File file) throws Exception{
        try (
            @RVTypeAnnotation("resource variable") BufferedReader br =
                new BufferedReader(new FileReader(file))
        ) {
            return br.readLine();
        }
    }

    // TODO Arrays
}
