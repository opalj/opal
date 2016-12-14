/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.bi.ACC_ENUM
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_SYNCHRONIZED
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_VARARGS
import org.opalj.bi.ACC_TRANSIENT
import org.opalj.bi.ACC_VOLATILE
import org.opalj.bi.ACC_NATIVE
import org.opalj.bi.ACC_STRICT

/**
 * Implementation of an EDSL for creating Java bytecode.
 *
 * @author Michael Eichberg
 */
package object ba {

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext = GlobalLogContext
        import OPALLogger.info
        try {
            scala.Predef.assert(false)
            info("OPAL", "Bytecode Assembler - Production Build")
        } catch {
            case ae: AssertionError ⇒
                info("OPAL", "Bytecode Assembler - Development Build (Assertions are enabled)")
        }
    }

    final val PUBLIC = new AccessModifier(ACC_PUBLIC.mask)

    final val FINAL = new AccessModifier(ACC_FINAL.mask)

    final val SUPER = new AccessModifier(ACC_SUPER.mask)

    final val INTERFACE = new AccessModifier(ACC_INTERFACE.mask)

    final val ABSTRACT = new AccessModifier(ACC_ABSTRACT.mask)

    final val SYNTHETIC = new AccessModifier(ACC_SYNTHETIC.mask)

    final val ANNOTATION = new AccessModifier(ACC_ANNOTATION.mask)

    final val ENUM = new AccessModifier(ACC_ENUM.mask)

    final val PRIVATE = new AccessModifier(ACC_PRIVATE.mask)

    final val PROTECTED = new AccessModifier(ACC_PROTECTED.mask)

    final val STATIC = new AccessModifier(ACC_STATIC.mask)

    final val SYNCHRONIZED = new AccessModifier(ACC_SYNCHRONIZED.mask)

    final val BRIDGE = new AccessModifier(ACC_BRIDGE.mask)

    final val VARARGS = new AccessModifier(ACC_VARARGS.mask)

    final val NATIVE = new AccessModifier(ACC_NATIVE.mask)

    final val STRICT = new AccessModifier(ACC_STRICT.mask)

    final val VOLATILE = new AccessModifier(ACC_VOLATILE.mask)

    final val TRANSIENT = new AccessModifier(ACC_TRANSIENT.mask)

}
