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
package support
package debug

import java.util.Properties
import java.io.FileInputStream

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

import scala.collection.JavaConverters._

/**
 * Prints the current explicit (application.conf/reference.conf files) and
 * implicit configuration settings (java.security, ''environment''). To show the explicit
 * configuration use "-config" as a parameter.
 *
 * Information that is shown:
 *  - the set of restricted packages configured by the JRE if a `SecurityManager` is installed.
 *    Note that the `package.definition` property is by default not checked by any class loader.
 *    Hence, it is not safe to leverage this information.
 *  - packages with access restrictions (package.access property); a security exception will be
 *    thrown when a class is a respective package is accessed (`checkPackageAccess`) unless the
 *    corresponding RuntimePermission ("accessClassInPackage."+package) has been granted.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
object ShowConfiguration {

    def renderConfig(config: Config): String = {
        val defaultRenderingOptions = ConfigRenderOptions.defaults()
        val renderingOptions =
            defaultRenderingOptions.
                setOriginComments(false).
                setComments(true).
                setJson(false)
        config.root().render(renderingOptions)
    }

    def main(args: Array[String]): Unit = {
        import Console.err

        val config: Config = ConfigFactory.load()

        if (args.contains("-config")) {
            println(s"\nContext Configuration (application/reference.conf): ")
            println(renderConfig(config).toString.replace("\n", "\n\t"))
        }

        //
        // Validate and show environment information
        //
        val javaHome: String =
            try {
                config.getString("java.home")
            } catch {
                case e: Exception ⇒
                    err.println("failed while reading \"java.home\"")
                    e.printStackTrace(err)
                    return ;
            }
        val javaVersion = System.getProperty("java.version")

        println(s"\nEnvironment:")
        println("\t$JAVA_HOME = "+javaHome)
        println("\t$JAVA_VERSION = "+javaVersion)
        if (!javaHome.contains(javaVersion)) {
            err.println("\tJava runtime environment does not match with read java.security file.")
            return ;
        }

        //
        // Handling of java.security
        //
        val javaSecurityFile =
            if (!javaVersion.startsWith("1.")) {
                // Java 9+
                javaHome+"/conf/security/java.security"
            } else {
                javaHome+"/lib/security/java.security"
            }
        val javaSecurity = new Properties()
        javaSecurity.load(new FileInputStream(javaSecurityFile))
        println(s"\nSecurity Configuration ($javaSecurityFile): ")
        import javaSecurity.getProperty
        if (getProperty("package.access") != getProperty("package.definition")) {
            err.println("package.access and package.definition define different packages")
        }
        javaSecurity.stringPropertyNames().asScala.foreach { property ⇒
            val entry = javaSecurity.getProperty(property)
            if (entry.contains(","))
                println(entry.split(",").mkString("\t"+property+"=\n\t\t", ",\n\t\t", ""))
            else
                println(s"\t$property=$entry")
        }
    }
}
