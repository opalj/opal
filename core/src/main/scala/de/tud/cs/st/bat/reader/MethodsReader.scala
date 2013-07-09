/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package reader

import reflect.ClassTag

import java.io.DataInputStream

/**
 * Defines a template method to read in a class file's Method_info structure.
 *
 * @author Michael Eichberg
 */
trait MethodsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type Attributes

    protected def Attributes(ap: AttributesParent,
                             cp: Constant_Pool,
                             in: DataInputStream): Attributes

    type Method_Info
    implicit val Method_InfoManifest: ClassTag[Method_Info]

    def Method_Info(accessFlags: Int,
                    name_index: Constant_Pool_Index,
                    descriptor_index: Constant_Pool_Index,
                    attributes: Attributes)(
                        implicit constant_pool: Constant_Pool): Method_Info

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    type Methods = IndexedSeq[Method_Info]

    def Methods(in: DataInputStream, cp: Constant_Pool): Methods = {
        val methods_count = in.readUnsignedShort

        if (methods_count == 0)
            IndexedSeq.empty
        else
            repeat(methods_count) {
                Method_Info(in, cp)
            }
    }

    private def Method_Info(in: DataInputStream, cp: Constant_Pool): Method_Info = {
        Method_Info(
            in.readUnsignedShort,
            in.readUnsignedShort,
            in.readUnsignedShort,
            Attributes(AttributesParent.Method, cp, in)
        )(cp)
    }
}
