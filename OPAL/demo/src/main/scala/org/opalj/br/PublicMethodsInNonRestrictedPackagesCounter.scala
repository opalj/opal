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
package de.tud.cs.st
package bat
package resolved

import analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import java.net.URL

/**
 * Counts the number of native methods.
 *
 * @author Michael Eichberg
 */
object PublicMethodsInNonRestrictedPackagesCounter extends AnalysisExecutor {

    val restrictedPackages = List( // set of restricted packages for Java 7 
        "sun/",
        "com/sun/xml/internal/",
        "com/sun/imageio/",
        "com/sun/istack/internal/",
        "com/sun/jmx/",
        "com/sun/proxy/",
        "com/sun/org/apache/bcel/internal/",
        "com/sun/org/apache/regexp/internal/",
        "com/sun/org/apache/xerces/internal/",
        "com/sun/org/apache/xpath/internal/",
        "com/sun/org/apache/xalan/internal/extensions/",
        "com/sun/org/apache/xalan/internal/lib/",
        "com/sun/org/apache/xalan/internal/res/",
        "com/sun/org/apache/xalan/internal/templates/",
        "com/sun/org/apache/xalan/internal/utils/",
        "com/sun/org/apache/xalan/internal/xslt/",
        "com/sun/org/apache/xalan/internal/xsltc/cmdline/",
        "com/sun/org/apache/xalan/internal/xsltc/compiler/",
        "com/sun/org/apache/xalan/internal/xsltc/trax/",
        "com/sun/org/apache/xalan/internal/xsltc/util/",
        "com/sun/org/apache/xml/internal/res/",
        "com/sun/org/apache/xml/internal/serializer/utils/",
        "com/sun/org/apache/xml/internal/utils/",
        "com/sun/org/apache/xml/internal/security/",
        "com/sun/org/glassfish/",
        "org/jcp/xml/dsig/internal/",
        "com/sun/java/accessibility/")

    val analysis = new Analysis[URL, BasicReport] {

        def description = "Counts the number of public methods in non-restricted packages."

        def analyze(project: Project[URL], parameters: Seq[String] = List.empty) = {
            val methods =
                for {
                    classFile ← project.classFiles
                    if classFile.isPublic
                    if !restrictedPackages.exists(classFile.fqn.startsWith(_))
                    method ← classFile.methods
                    if method.body.isDefined
                    if (method.isPublic || (method.isProtected && !classFile.isFinal))
                } yield (
                    classFile.thisType.toJava,
                    method.toJava,
                    method.parameterTypes.filter(_.isReferenceType).size
                )

            BasicReport(
                "Public methods in non-restricted packages found ("+methods.size+"):\n"+
                    methods.map { t ⇒
                        val (typeName, methodSignature, count) = t
                        typeName+" -> "+methodSignature+" ("+count+")"
                    }.mkString("\t", "\n\t", "\n")+
                    "Overall non-native method parameters: "+(methods.map(_._3).foldLeft(0)(_ + _))
            )
        }
    }
}