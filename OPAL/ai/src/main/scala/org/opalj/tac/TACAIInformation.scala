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
package br
package analyses

import scala.collection.concurrent.TrieMap
import org.opalj.ai.Domain
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.tac.TACode
import org.opalj.tac.DUVar
import org.opalj.tac.TACAI

import scala.collection.concurrent.TrieMap

/**
 * ''Key'' to get the 3-address based code of a method computed using the configured
 * domain/data-flow analysis.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object TACAIKey extends ProjectInformationKey[Method ⇒ TACode[DUVar[Domain#DomainValue]]] {

    @volatile var domainFactory: (SomeProject, ClassFile, Method) ⇒ Domain with RecordDefUse =
        (p: SomeProject, cf: ClassFile, m: Method) ⇒ {
            new ai.domain.l1.DefaultDomainWithCFGAndDefUse(p, cf, m)
        }

    /**
     * This mechanism to get the TACAI code has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing]] = Nil

    /**
     * Returns an object which computes and caches the 3-address code of a method when required.
     *
     * All methods belonging to a project are converted using the same `domainFactory`. Hence,
     * the `domainFactory` needs to be set before compute is called/this key is passed to a
     * specific project. If multiple projects are instead concurrently, external synchronization
     * is necessary (e.g., on the ProjectInformationKey) to ensure that each project is
     * instantiated using the desired domain.
     */
    override protected def compute(
        project: SomeProject
    ): Method ⇒ TACode[DUVar[_ <: Domain#DomainValue]] = {
        val domainFactory = this.domainFactory

        val taCodes = TrieMap.empty[Method, TACode[DUVar[Domain#DomainValue]]]

        (m: Method) ⇒ {
            taCodes.get(m) match {
                case Some(taCode) ⇒ taCode
                case None ⇒
                    val brCode = m.body.get
                    // Basically, we use double checked locking; we really don't want to
                    // transform the code more than once!
                    brCode.synchronized {
                        taCodes.get(m) match {
                            case Some(taCode) ⇒ taCode
                            case None ⇒
                                val cf = project.classFile(m)
                                val domain = domainFactory(project, cf, m)
                                val aiResult = BaseAI(cf, m, domain)
                                val code = TACAI(m, project.classHierarchy, aiResult)(Nil)
                                // well... the following cast safe is safe, because the underlying
                                // datastructure is actually, conceptually immutable
                                val taCode = code.asInstanceOf[TACode[DUVar[Domain#DomainValue]]]
                                taCodes.put(m, taCode)
                                taCode
                        }
                    }
            }
        }
    }
}

