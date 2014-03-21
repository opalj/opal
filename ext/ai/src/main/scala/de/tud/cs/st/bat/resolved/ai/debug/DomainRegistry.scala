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
package resolved
package ai
package debug

import analyses.SomeProject
import analyses.ProjectLike

/**
 * The domain registry is a registry for all domains that can be instantiated given
 * a `Project`, `ClassFile` and `Method`. The registry is primarily useful for tools
 * for debugging purposes that let the user/developer choose between different domains
 * and see the resulting output.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Eichberg
 */
object DomainRegistry {

    private[this] var descriptions: Map[String, Class[_ <: SomeDomain]] = Map.empty
    private[this] var theRegistry: Map[Class[_ <: SomeDomain], (SomeProject, ClassFile, Method) ⇒ SomeDomain] = Map.empty

    def register(domainDescription: String, domainClass: Class[_ <: SomeDomain], factory: (SomeProject, ClassFile, Method) ⇒ SomeDomain): Unit = {
        this.synchronized {
            descriptions += ((domainDescription, domainClass))
            theRegistry += ((domainClass, factory))
        }
    }

    def domainDescriptions(): Iterable[String] = this.synchronized { descriptions.keys }

    def registry = this.synchronized { theRegistry }

    // primarily introduced to facilitate the interaction with Java
    def newDomain(
        domainDescription: String,
        project: SomeProject,
        classFile: ClassFile,
        method: Method): SomeDomain = {
        this.synchronized {
            val domainClass: Class[_ <: SomeDomain] = descriptions(domainDescription)
            newDomain(domainClass, project, classFile, method)
        }
    }

    def newDomain(
        domainClass: Class[_ <: SomeDomain],
        project: SomeProject,
        classFile: ClassFile,
        method: Method): SomeDomain = {
        this.synchronized {
            theRegistry(domainClass)(project, classFile, method)
        }
    }

    // initialize with the known default domains 
    register(
        "The most basic domain; it does all computations at the type level.",
        classOf[domain.l0.BaseConfigurableDomain[(ClassFile, Method)]],
        (project: SomeProject, classFile: ClassFile, method: Method) ⇒ {
            new domain.l0.BaseConfigurableDomain((ClassFile, Method))
        }
    )

    register(
        "The most precise domain readily available; this is primarily meant as a showcase!",
        classOf[domain.l1.DefaultConfigurableDomain[(ClassFile, Method)]],
        (project: SomeProject, classFile: ClassFile, method: Method) ⇒ {
            new domain.l0.BaseConfigurableDomain((ClassFile, Method))
        }
    )
}
