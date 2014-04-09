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
package reader

import scala.collection.mutable.Buffer

/**
 * Constant pool related type definitions.
 *
 * @author Michael Eichberg
 */
trait Constant_PoolAbstractions {

    type Constant_Pool

    type Constant_Pool_Entry

    type Constant_Pool_Index = Int

    // The following definitions were introduced to enable the post transformation
    // of a class file after it was (successfully) loaded. In particular to resolve
    // references to the `BootstrapMethods` attribute. 
    // The underlying idea is that the class file specific transformations are stored
    // in the class file's constant pool. The constant pool is basically loaded 
    // first and then passed to all subsequently loaded class file elements 
    // (Methods,Fields,..)

    type ClassFile

    /**
     * A DeferredActionsStore stores all functions that need to perform post load actions.
     *
     * One example is the resolution of references to attributes.
     * (The constant pool is the only structure that is passed around and hence it is the
     * only place where to store information/functions related to a specific class file).
     */
    protected[bat]type DeferredActionsStore = Buffer[ClassFile ⇒ ClassFile] with Constant_Pool_Entry

    /**
     * This method is called/needs to be called after the class file was completely
     * loaded to perform class file specific transformations.
     */
    protected[bat] def applyDeferredActions(
        cp: Constant_Pool,
        classFile: ClassFile): ClassFile

}