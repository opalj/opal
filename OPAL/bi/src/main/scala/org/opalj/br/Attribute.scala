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
package de.tud.cs.st
package bat
package resolved

import java.util.concurrent.atomic.AtomicInteger

/**
 * A class file attribute.
 *
 * ==Note==
 * Some class file attributes are skipped or resolved while loading
 * the class file and hence, are no longer represented at runtime.
 *
 * @author Michael Eichberg
 */
trait Attribute {

    /**
     * Returns the unique ID that identifies this kind of attribute (Signature,
     * LineNumberTable,...)
     *
     * This id can then be used in a switch statement to efficiently identify the
     * attribute.
     * {{{
     * (attribute.id : @scala.annotation.switch) match {
     *      case Signature.Id => ...
     * }
     * }}}
     *
     * ==Associating Unique Id==
     * The unique ids are manually associated with the attributes.
     * The attributes use the following IDs:
     *  - (-1 **Unknown Attribute**) 
     *  - 1-5  The ConstantValue Attribute
     *  - 6 The Code Attribute
     *  - 7 The StackMapTable Attribute
     *  - 8 The Exceptions Attribute
     *  - 9 The InnerClasses Attribute
     *  - 10 The EnclosingMethod Attribute
     *  - 11 The Synthetic Attribute
     *  - 12-16 The Signature Attribute
     *  - 17 The SourceFile Attribute
     *  - 18 The SourceDebugExtension Attribute
     *  - 19 The LineNumberTable Attribute
     *  - 20 The LocalVariableTable Attribute
     *  - 21 The LocalVariableTypeTable Attribute
     *  - 22 The Deprecated Attribute
     *  - 23 The RuntimeVisibleAnnotations Attribute
     *  - 24 The RuntimeInvisibleAnnotations Attribute
     *  - 25 The RuntimeVisibleParameterAnnotations Attribute
     *  - 26 The RuntimeInvisibleParameterAnnotations Attribute
     *  - 27 The RuntimeVisibleTypeAnnotations Attribute
     *  - 28 The RuntimeInvisibleTypeAnnotations Attribute
     *  - 29-41 The AnnotationDefault Attribute
     *  - 42 The BootstrapMethods Attribute
     *  - 43 The MethodParameters Attribute
     */
    def kindId: Int
}

 