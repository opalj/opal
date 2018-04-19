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
package cg

/**
 * Stores the information about those class that are not instantiable. The set of
 * classes that are not instantiable is usually only a small fraction of all classes
 * and hence, more efficient to store/query than those which are instantiable.
 *
 * A class is considered instantiable if it is possible that at some point in time an
 * instance of the respective class is created (via a direct constructor call, a factory method
 * call, an indirect instance creation by means of creating an instance of a subtype).
 *
 * An example of a class which is not instaniable is a class which defines a private constructor
 * which is not called by other (factory) methods and which is also not serializable. A class
 * which defines no constructor at all (not possible using Java, but still valid bytecode) is
 * also not instantiable.
 *
 * @author Michael Eichberg
 */
// RENAME => MayHaveInstances
class InstantiableClasses(
        val project:         SomeProject,
        val notInstantiable: Set[ObjectType]
) {

    def isNotInstantiable(classType: ObjectType): Boolean = notInstantiable.contains(classType)

    def statistics: Map[String, Int] = Map(
        "# of not instantiable classes in the project" → notInstantiable.size
    )

    override def toString: String = notInstantiable.mkString("Not instantiable: ", ", ", ".")

}
