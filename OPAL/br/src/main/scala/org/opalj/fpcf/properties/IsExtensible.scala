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
package fpcf
package properties

import org.opalj.br.ClassFile

/**
 * This set property describes if a class is further extensible by yet unknown types (i.e., can be (transitively) inherited from).
 * This property generally depends on the kind of the project. If the project is an application, all classes
 * are considered to be closed; i.e., the class hierarchy is considered to be fixed; if the
 * analyzed project is a library then the result depends on the concrete assumption about the
 * openness of the library.
 *
 * == Extensibility w.r.t. the open-packages assumption ==
 *
 * A class is extensible if:
 *  $ - the class is not (effectively) final
 *  $ - one of its subclasses is extensible
 *
 * == Extensibility w.r.t. the closed-packages assumption ==
 *
 * A class is extensible if:
 *  $ - the class is public and not (effectively) final
 *  $ - one of its subclasses is extensible
 *
 * == Special cases ==
 *
 * If a class is defined in a package starting with '''java.*''', it has to be treated like classes that are
 * analyzed w.r.t. to closed-packages assumption. This is necessary because the default `ClassLoader` prevents
 * the definition of further classes within these packages, hence, they are closed by definition.
 *
 * If the analyzed codebase has an incomplete type hierarchy which leads to unknown subtype relationships, it is
 * necessary to add these particular classes to the computed set of extensible classes.
 *
 * @author Michael Reif
 */
case object IsExtensible extends SetProperty[ClassFile]
