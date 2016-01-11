/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package analyses

import org.opalj.br.ObjectType
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Stores the information which types of objects are (potentially) injected based on the
 * annotations that are found in the project. For example, by means of
 * reflection or by a web server or some other comparable framework.
 *
 * This information is used to compute the entry points of a JaveEE application.
 *
 * @author Michael Reif
 */
class InjectedClassesInformation(val injectedTypes: Set[ObjectType]) {

    final def isInjected(classFile: ClassFile): Boolean = isInjected(classFile.thisType)

    def isInjected(objectType: ObjectType): Boolean = injectedTypes.contains(objectType)
}

/**
 * Factory to create [[InjectedClassesInformation]].
 */
object InjectedClassesInformationAnalysis {

    def apply(project: SomeProject, isInterrupted: () ⇒ Boolean): InjectedClassesInformation = {

        val injectedTypes = new ConcurrentLinkedQueue[ObjectType]

        project.parForeachClassFile(isInterrupted) { cf ⇒
            cf.fields filter { field ⇒ field.fieldType.isObjectType } foreach { field ⇒
                // IMPROVE Check for specific annotations that are related to "Injections"
                if (field.annotations.size > 0) {
                    val fieldType = field.fieldType.asObjectType
                    injectedTypes.add(fieldType)
                }
            }
        }

        new InjectedClassesInformation(injectedTypes.asScala.toSet)
    }
}