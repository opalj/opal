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
import java.util.ArrayList;
import java.util.List;

/**
 * This class tests some (corner) cases related to type annotations.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public abstract class ATypeAnnotationUser<@ATypeAnnotation T extends @ATypeAnnotation Serializable & Cloneable>
        implements List<@ATypeAnnotation Object>, @ATypeAnnotation Serializable {

    private static final @ATypeAnnotation long serialVersionUID = 1L;

    public List<@ATypeAnnotation("annotation of generic type parameter") T> ser;

    public @ATypeAnnotation Object doSomething() throws Exception {

        @ATypeAnnotation("local variable annotation")
        List<@ATypeAnnotation("type parameter annotation") T> l = new @ATypeAnnotation ArrayList<>();

        return l;
    }

    public <X extends @ATypeAnnotation Serializable & @ATypeAnnotation("annotation of second type of intersection type") Cloneable> void crazy(X x) {
        // annotated type cast
        if(x instanceof @ATypeAnnotation("annotated instanceof") List) {
            List<?> l = (@ATypeAnnotation("annotated type cast") List<?>) x;
            System.out.println(l);
        }

        // annotated type cast
        if(x instanceof List) {
            Object l = (Serializable & @ATypeAnnotation("annotation of second type of a case to an intersection type") Cloneable) x;
            System.out.println(l);
        }
    }
}
