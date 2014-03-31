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
 * a `Project`, `ClassFile` and `Method`.
 *
 * The registry was developed to support tools for debugging purposes that let
 * the user/developer choose between different domains to do an abstract interpretation
 * and to see the resulting output.
 *
 * The compatible domains that are part of BATAI are already registered.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Eichberg
 */
object DomainRegistry {

    private[this] var descriptions: Map[String, Class[_ <: SomeDomain]] = Map.empty
    private[this] var theRegistry: Map[Class[_ <: SomeDomain], (SomeProject, ClassFile, Method) ⇒ SomeDomain] = Map.empty

    /**
     * Register a new domain that can be used to perform an abstract interpretation
     * of a specific method.
     *
     * @param domainDescription A short description of the properties of the domain;
     * 		in particular w.r.t. the kind of computations the domain does.
     * @param domainClass The class of the domain.
     * @param factory The factory method that will be used to create instances of the
     * 		domain.
     */
    def register(
        domainDescription: String,
        domainClass: Class[_ <: SomeDomain],
        factory: (SomeProject, ClassFile, Method) ⇒ SomeDomain): Unit = {
        this.synchronized {
            descriptions += ((domainDescription, domainClass))
            theRegistry += ((domainClass, factory))
        }
    }

    /**
     * Returns an `Iterable` to make it possible to iterate over the descriptions of
     * the domain. Useful to show the (end-users) some meaningful descriptions.
     */
    def domainDescriptions(): Iterable[String] = this.synchronized { descriptions.keys }

    /**
     * Returns the current view of the registry.
     */
    def registry = this.synchronized { theRegistry }

    /**
     * Creates a new instance of the domain identified by the given `domainDescription`.
     *
     * @param domainDescription The description that identifies the domain.
     * @param project The project.
     * @param classFile A class file object that belongs to the given project.
     * @param method A non-native/non-abstract method belonging to the specified class
     * 		file.
     */
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

    /**
     * Creates a new instance of the domain identified by the given `domainClass`. To
     * create the instance the registered factory method will be used.
     *
     * @param domainClass The class object of the domain.
     * @param project The project.
     * @param classFile A class file object that belongs to the given project.
     * @param method A non-native/non-abstract method belonging to the specified class
     * 		file.
     */
    def newDomain(
        domainClass: Class[_ <: SomeDomain],
        project: SomeProject,
        classFile: ClassFile,
        method: Method): SomeDomain = {
        this.synchronized {
            theRegistry(domainClass)(project, classFile, method)
        }
    }

    // initialize the registry with the known default domains 
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
