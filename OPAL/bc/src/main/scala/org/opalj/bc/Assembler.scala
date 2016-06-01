/* BSD 2-Clause License:
 * Copyright (c) 2016
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
package bc

import org.opalj.da._
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Assembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Assembler {
    //
    //    implicit def classFileToCFE(classFile :ClassFile) : ClassFileElement[ClassFile] = {
    //        new ClassFileElement[ClassFile] {
    //            val source = classFile
    //            def write(out : DataOutputStream) : Unit={
    //                out.writeInt(org.opalj.bi.ClassFileMagic)                
    //            } 
    //        }
    //    }
    //    

    implicit object RichFieldInfo extends ClassFileElement[Field_Info] {
        def write(out: DataOutputStream)(method: Field_Info): Unit = {
            // TODO
        }
    }

    implicit object RichMethodInfo extends ClassFileElement[Method_Info] {
        def write(out: DataOutputStream)(method: Method_Info): Unit = {
            // TODO
        }
    }

    implicit object RichClassFile extends ClassFileElement[ClassFile] {

        def write(out: DataOutputStream)(classFile: ClassFile): Unit = {
            out.writeInt(org.opalj.bi.ClassFileMagic)
            classFile.methods.foreach { serialize(out) }
            classFile.fields.foreach { serialize(out) }
        }
    }

    def serialize[T](out: DataOutputStream)(t: T)(implicit cfe: ClassFileElement[T]): Unit = {
        cfe.write(out)(t)
    }

    def apply(classFile: ClassFile): Array[Byte] = {
        val data = new ByteArrayOutputStream(classFile.size)
        val out = new DataOutputStream(data)
        serialize(out)(classFile)
        out.flush()
        data.toByteArray()
    }

}

trait ClassFileElement[T] {

    def write(out: DataOutputStream)(t: T): Unit

}

