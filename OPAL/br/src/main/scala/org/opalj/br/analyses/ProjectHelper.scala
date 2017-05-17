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
package org.opalj.br.analyses

import org.opalj.br.ObjectType
import org.opalj.log.{ConsoleOPALLogger, DefaultLogContext, Error, GlobalLogContext, OPALLogger}

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

/**
 * Helper object for getting Project information.
 *
 * @author Andreas Muttscheller
 */
object ProjectHelper {
    /**
     * Helper class to get a list of all classes that derive from objectType.
     *
     * @note To be independent of any scala version, the parameter and return types are
     *       java compliant. This way, this method can be called using an instance of a class
     *       loader.
     *
     * @param classPathListJava A list of classpaths, where to look for classes.
     * @param objectType The object type in jvm annotation (using "/" instead of ".", e.g.
     *                   "java/util/List")
     * @return A list of classes that derive from objectType.
     */
    def getAllSubclassesOfObjectType(
        classPathListJava: java.util.List[java.io.File],
        objectType:        String
    ): java.util.List[String] = {
        // Supress all opal related log messages and show only errors.
        implicit val logContext = new DefaultLogContext()
        OPALLogger.register(logContext, new ConsoleOPALLogger(true))
        OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Error))

        val classPathList = JavaConversions.asScalaBuffer(classPathListJava)
        val opalObjectType = ObjectType(objectType)

        var project = Project(classPathList.head)
        classPathList.slice(1, classPathList.length).foreach { cp ⇒
            project = Project.extend(project, cp)
        }

        project
            .classHierarchy
            .allSubtypes(opalObjectType, reflexive = false)
            .map(ot ⇒ ot.toJava)
            .toList
            .asJava
    }
}
