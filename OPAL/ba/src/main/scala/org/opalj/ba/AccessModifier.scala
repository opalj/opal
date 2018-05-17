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
package org.opalj
package ba

/**
 * Represents the access flags of a class (module), method or field declaration.
 *
 * All standard access flags are predefined.
 *
 * @example
 *         To create a class file's, a field's or a method's access modifier, you can chain them
 *         using post fix notation, e.g.:
 *         {{{
 *         PUBLIC FINAL
 *         }}}
 *         or you just append the using '.', e.g.:
 *         {{{
 *         PUBLIC.FINAL.SYNTHETIC.VARARGS
 *         }}}
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
final class AccessModifier(private[ba] val accessFlags: Int) extends AnyVal {

    final def PUBLIC: AccessModifier = new AccessModifier(this.accessFlags | ba.PUBLIC.accessFlags)

    final def FINAL: AccessModifier = new AccessModifier(this.accessFlags | ba.FINAL.accessFlags)

    final def SUPER: AccessModifier = new AccessModifier(this.accessFlags | ba.SUPER.accessFlags)

    final def INTERFACE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.INTERFACE.accessFlags)
    }

    final def ABSTRACT: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.ABSTRACT.accessFlags)
    }

    final def SYNTHETIC: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.SYNTHETIC.accessFlags)
    }

    final def ANNOTATION: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.ANNOTATION.accessFlags)
    }

    final def ENUM: AccessModifier = new AccessModifier(this.accessFlags | ba.ENUM.accessFlags)

    final def PRIVATE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.PRIVATE.accessFlags)
    }

    final def PROTECTED: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.PROTECTED.accessFlags)
    }

    final def STATIC: AccessModifier = new AccessModifier(this.accessFlags | ba.STATIC.accessFlags)

    final def SYNCHRONIZED: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.SYNCHRONIZED.accessFlags)
    }

    final def BRIDGE: AccessModifier = new AccessModifier(this.accessFlags | ba.BRIDGE.accessFlags)

    final def VARARGS: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.VARARGS.accessFlags)
    }

    final def NATIVE: AccessModifier = new AccessModifier(this.accessFlags | ba.NATIVE.accessFlags)

    final def STRICT: AccessModifier = new AccessModifier(this.accessFlags | ba.STRICT.accessFlags)

    final def VOLATILE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.VOLATILE.accessFlags)
    }

    final def TRANSIENT: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.TRANSIENT.accessFlags)
    }

    final def MODULE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.MODULE.accessFlags)
    }
    final def OPEN: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.OPEN.accessFlags)
    }
    final def MANDATED: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.MANDATED.accessFlags)
    }
    final def TRANSITIVE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.TRANSITIVE.accessFlags)
    }
    final def STATIC_PHASE: AccessModifier = {
        new AccessModifier(this.accessFlags | ba.STATIC_PHASE.accessFlags)
    }
}
