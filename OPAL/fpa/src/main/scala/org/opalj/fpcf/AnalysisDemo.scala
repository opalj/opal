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
package fpcf

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.bi.VisibilityModifier

/**
 * @author Michael Reif
 */
trait AnalysisDemo extends DefaultOneStepAnalysis {

    def entitiesByProperty(
        property: Property
    )(
        implicit
        propertyStore: PropertyStore
    ): Traversable[(Entity, Property)] =
        propertyStore(property.key).filter { ep ⇒
            val isFactoryMethod = ep._2
            isFactoryMethod == property
        }

    def finalReport(infoStrings: Traversable[String], caption: String): String =
        infoStrings.mkString(s"\n$caption:", "\n\t", s"\nTotal: ${infoStrings.size}\n\n")
}

trait MethodAnalysisDemo extends AnalysisDemo {

    def buildMethodInfo(
        entities:    Traversable[(Entity, Property)],
        withJarInfo: Boolean                         = false
    )(
        implicit
        project: Project[URL]
    ): Traversable[String] = entities.map { e ⇒
        val method = e._1.asInstanceOf[Method]
        val methodString = getVisibilityModifier(method)+" "+method.name
        val classFile = project.classFile(method)
        val jarInfo = if (withJarInfo)
            project.source(classFile.thisType)
        else ""
        val classVisibility = if (classFile.isPublic) "public" else ""
        jarInfo + s"\n\t $classVisibility "+classFile.thisType.toJava+" | "+methodString
    }

    private[this] def getVisibilityModifier(method: Method): String = {
        method.visibilityModifier map { mod ⇒
            if (mod.javaName.nonEmpty) mod.javaName.get else ""
        } getOrElse ""
    }
}

trait ClassAnalysisDemo extends AnalysisDemo {

    def buildClassInfo(
        entities:    Traversable[(Entity, Property)],
        withJarInfo: Boolean                         = false
    )(
        implicit
        project: Project[URL]
    ): Traversable[String] = entities.map { e ⇒
        val classFile = e._1.asInstanceOf[ClassFile]
        val jarInfo = if (withJarInfo)
            project.source(classFile.thisType)
        else ""
        jarInfo+"\n\t "+classFile.thisType.toJava
    }
}