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

import scala.collection.JavaConverters._

import org.opalj.log.{ConsoleOPALLogger, StandardLogContext, Error, GlobalLogContext, OPALLogger}

/**
 * Enables the querying of a project.
 *
 * @note '''The interface of this class was designed with Java interoperability in mind!'''
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
class JavaProject( final val project: Project[java.net.URL]) {

    /**
     * @param classPath A list of files and jars, where to look for classes.
     */
    def this(classPath: java.util.List[java.io.File]) {
        this({
            implicit val logCtx = new StandardLogContext()
            OPALLogger.register(logCtx, JavaProject.Logger)
            val cp = classPath.asScala
            Project(
                Project.JavaClassFileReader(theLogContext = logCtx).AllClassFiles(cp),
                Traversable.empty, true, /*true or false... doesn't matter when we have no lib. */
                Traversable.empty,
                Project.defaultHandlerForInconsistentProjects,
                BaseConfig,
                logCtx
            )
        })
    }

    /**
     * Returns the list of all classes that derive from `objectType`.
     *
     * @param objectType The object type in jvm annotation (using "/" instead of ".", e.g.
     *                   "java/util/List")
     * @return A list of classes that derive from objectType.
     */
    def getAllSubclassesOfObjectType(objectType: String): java.util.List[String] = {
        project
            .classHierarchy
            .allSubtypes(ObjectType(objectType), reflexive = false)
            .map(ot ⇒ ot.toJava)
            .toList
            .asJava
    }
}

object JavaProject {

    // Supress all opal info related log messages and show only errors.
    private final val Logger = new ConsoleOPALLogger(true, Error)

    OPALLogger.updateLogger(GlobalLogContext, Logger)

}
