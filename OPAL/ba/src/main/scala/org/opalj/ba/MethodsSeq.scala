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

import org.opalj.br.Method

/**
 * Provides helper methods to easily collect the results annotations methods.
 *
 * @author Michael Eichberg
 */
object MethodsSeq {

    def collectAnnotations[T](
        metaInformationStore: scala.collection.mutable.Map[Method, T]
    )(
        methods: (Method, T)*
    ): IndexedSeq[Method] = {
        metaInformationStore ++= methods.toMap
        methods.map(_._1).toIndexedSeq
    }

    def collectMetaInformation[T, X](
        metaInformationStore: scala.collection.mutable.Map[Method, X]
    )(
        methods: (Method, T)*
    )(
        implicit
        f: T ⇒ X
    ): IndexedSeq[Method] = {
        metaInformationStore ++= methods.toMap.mapValues(f)
        methods.map(_._1).toIndexedSeq
    }

    /**
     * Collects the methods and throws away any potential meta information.
     */
    def apply[T](methods: (Method, T)*): IndexedSeq[Method] = methods.map(_._1).toIndexedSeq

}
