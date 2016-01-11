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
package analyses

import scala.collection.Map
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import org.opalj.br.instructions.LDCString

/**
 * The ''key'' object to get information about all string constants.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object StringConstantsInformationKey extends ProjectInformationKey[StringConstantsInformation] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing]] = Nil

    /**
     * Computes the field access information.
     */
    override protected def compute(project: SomeProject): Map[String, List[(Method, PC)]] = {

        val map = new ConcurrentHashMap[String, ConcurrentLinkedQueue[(Method, PC)]](project.methodsCount)

        project.parForeachMethodWithBody(
            () ⇒ Thread.currentThread().isInterrupted()
        ) { methodInfo ⇒
                val (_ /*source*/ , _ /*classFile*/ , method) = methodInfo

                method.body.get foreach { (pc, instruction) ⇒
                    instruction match {
                        case LDCString(value) ⇒
                            var list = new ConcurrentLinkedQueue[(Method, PC)]();
                            val previousList = map.putIfAbsent(value, list)
                            if (previousList != null) list = previousList
                            list.add((method, pc))
                        case _ ⇒ // we don't care
                    }
                }
            }

        map.asScala.map(kv ⇒ (kv._1, kv._2.asScala.toList))
    }
}

