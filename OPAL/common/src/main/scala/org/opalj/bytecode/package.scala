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

import java.io.File

/**
 * Defines functionality commonly useful when processing Java bytecode.
 *
 * @author Michael Eichberg
 */
package object bytecode {

    /**
     * Returns the package definitions shared by both fully qualified type names.
     * If both types do not define a common package `None` is returned.
     *
     * @example
     * {{{
     * scala> org.opalj.commonPackage("a.b.T","a.c.T")
     * res: Option[String] = Some(a.)
     *
     * scala> org.opalj.commonPackage("a.b.T","a.T")
     * res: Option[String] = Some(a.)
     *
     * scala> org.opalj.commonPackage("a.b.T","a.b.T")
     * res: Option[String] = Some(a.b.)
     *
     * scala> org.opalj.commonPackage("c.b.T","a.T")
     * res: Option[String] = None
     *
     * scala> org.opalj.commonPackage("a.b.T","d.c.T")
     * res: Option[String] = None
     * }}}
     */
    def commonPackage(
        fqnA: String, fqnB: String,
        packageSeperatorChar: Int = '.'): Option[String] = {
        val packageSeperatorIndex = fqnA.indexOf(packageSeperatorChar) + 1
        if (packageSeperatorIndex <= 0)
            return None;

        val rootPackage = fqnA.substring(0, packageSeperatorIndex)
        if (packageSeperatorIndex == fqnB.indexOf(packageSeperatorChar) + 1 &&
            rootPackage == fqnB.substring(0, packageSeperatorIndex)) {
            commonPackage(
                fqnA.substring(packageSeperatorIndex, fqnA.length()),
                fqnB.substring(packageSeperatorIndex, fqnB.length())
            ) match {
                    case Some(childPackage) ⇒ Some(rootPackage + childPackage)
                    case None               ⇒ Some(rootPackage)
                }
        } else {
            None
        }
    }

    /**
     * Abbreviates the given `memberTypeFQN` by abbreviating the common packages
     * (except of the last shared package) of both fully qualified type names using '…'.
     *
     * @example
     * {{{
     * scala> org.opalj.abbreviateFQN("a.b.T","a.T") // <= no abbrev.
     * res: String = a.T
     *
     * scala> org.opalj.abbreviateFQN("a.b.T","a.b.T")
     * res: String = …b.T
     *
     * scala> org.opalj.abbreviateFQN("a.b.c.T","a.b.c.T.X")
     * res: String = …c.T.X
     * }}}
     */
    def abbreviateFQN(
        definingTypeFQN: String, memberTypeFQN: String,
        packageSeperatorChar: Int = '.'): String = {

        commonPackage(definingTypeFQN, memberTypeFQN) match {
            case Some(commonPackages) if commonPackages.indexOf(packageSeperatorChar) < commonPackages.length - 1 ⇒
                // we have more than one common package...
                val beforeLastCommonPackageIndex = commonPackages.dropRight(1).lastIndexOf(packageSeperatorChar)
                val length = memberTypeFQN.length
                val packagesCount = commonPackages.count(_ == packageSeperatorChar) - 1
                val packageAbbreviation = "." * packagesCount
                packageAbbreviation +
                    memberTypeFQN.substring(beforeLastCommonPackageIndex + 1, length)
            case _ ⇒
                memberTypeFQN
        }
    }

    /**
     * Tries to locate the JRE's library folder. (I.e., the
     * location in which the rt.jar file and the other jar files belonging to the
     * Java runtime environment can be found).
     */
    lazy val JRELibraryFolder: File = {
        val paths =
            System.getProperties().getProperty("sun.boot.class.path").split(File.pathSeparator)
        var libPath =
            paths.find(_.endsWith("rt.jar")).map { path ⇒
                path.substring(0, path.length() - 6)
            }.getOrElse("null")

        if (libPath == null) {
            libPath = System.getProperty("sun.boot.library.path")
            if (libPath == null) {
                throw new RuntimeException("cannot locate the JRE libraries")
            }
        }

        new File(libPath)
    }

    /**
     * Tries to locate the JRE's library folder. (I.e., the
     * location in which the rt.jar file and the other jar files belonging to the
     * Java runtime environment can be found).
     */
    lazy val RTJar: File = {
        val paths =
            System.getProperties().getProperty("sun.boot.class.path").split(File.pathSeparator)
        val rtJarPath =
            paths.find(_.endsWith("rt.jar")).getOrElse("null")

        if (rtJarPath == null) {
            val rtJarCandidates =
                new File(System.getProperty("sun.boot.library.path")).listFiles(
                    new java.io.FilenameFilter() {
                        def accept(dir: File, name: String) = name == "rt.jar"
                    }
                )
            if (rtJarCandidates.length != 1) {
                throw new RuntimeException("cannot locate the JRE libraries")
            }
            rtJarCandidates(0)
        } else
            new File(rtJarPath)
    }
}
