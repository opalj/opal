/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package debug

import java.util.Properties
import java.io.FileInputStream

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

import scala.jdk.CollectionConverters._

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

        val config: Config = BaseConfig

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
                case e: Exception =>
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
        javaSecurity.stringPropertyNames().asScala.foreach { property =>
            val entry = javaSecurity.getProperty(property)
            if (entry.contains(","))
                println(entry.split(",").mkString("\t"+property+"=\n\t\t", ",\n\t\t", ""))
            else
                println(s"\t$property=$entry")
        }
    }
}
