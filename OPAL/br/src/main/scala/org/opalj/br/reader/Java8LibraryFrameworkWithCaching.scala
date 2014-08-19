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
package org.opalj
package br
package reader

import org.opalj.bi.reader.AttributesReader
import org.opalj.bi.reader.SkipUnknown_attributeReader

/**
 * This "framework" can be used to read in Java 8 (version 52) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method bodies.
 *
 * @author Michael Eichberg
 */
class Java8LibraryFrameworkWithCaching(
    final val cache: BytecodeInstructionsCache)
        extends ConstantPoolBinding
        with FieldsBinding
        with MethodsBinding
        with ClassFileBinding
        with AttributesReader
        /* If you want unknown attributes to be represented uncomment the following: */
        // with Unknown_attributeBinding 
        /* and comment out the following line: */
        with SkipUnknown_attributeReader
        with AnnotationAttributesBinding
        with InnerClasses_attributeBinding
        with EnclosingMethod_attributeBinding
        with SourceFile_attributeBinding
        with Deprecated_attributeBinding
        with Signature_attributeBinding
        with Synthetic_attributeBinding
        with ConstantValue_attributeBinding
        with MethodParameters_attributeBinding
        with TypeAnnotationAttributesBinding

