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
package hermes

import org.opalj.log.OPALLogger.error
import org.opalj.log.GlobalLogContext

/**
 * Container for feature queries.
 *
 * @note   Used to represent the corresponding information in the general configuration file.
 * @param  query The name of a concrete class which inherits from `FeatureQuery` and implements
 *         a default constructor.
 *
 * @author Michael Eichberg
 */
class Query(val query: String, private[this] var activate: Boolean = true) {

    def isEnabled: Boolean = activate

    private[this] var reifiedQuery: Option[FeatureQuery] = null

    def reify(implicit hermes: HermesConfig): Option[FeatureQuery] = this.synchronized {
        if (reifiedQuery ne null) {
            return reifiedQuery;
        }

        reifiedQuery =
            try {
                val queryClass = Class.forName(query, false, getClass.getClassLoader)
                val queryClassConstructor = queryClass.getDeclaredConstructor(classOf[HermesConfig])
                Some(queryClassConstructor.newInstance(hermes).asInstanceOf[FeatureQuery])
            } catch {
                case t: Throwable ⇒
                    error("application configuration", s"failed to load: $query", t)(GlobalLogContext)
                    activate = false
                    None
            }
        reifiedQuery
    }

}
