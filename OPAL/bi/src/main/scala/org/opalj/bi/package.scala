/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.lang.Integer.parseInt

import scala.io.Source

import org.opalj.io.process
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.Warn
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.error
import org.opalj.collection.immutable.UShortPair

/**
 * Implementation of a library for parsing Java bytecode and creating arbitrary
 * representations.
 *
 * OPAL's primary representation of Java byte code
 * is the [[org.opalj.br]] representation which is defined in the
 * respective package. A second representation that represents bytecode one-by-one
 * is found in the [[org.opalj.da]] package.
 *
 * == This Package ==
 * Common constants and type definitions used across OPAL.
 *
 * @author Michael Eichberg
 */
package object bi {

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext: LogContext = GlobalLogContext
        try {
            assert(false)
            info("OPAL Bytecode Infrastructure", "Production Build")
        } catch {
            case _: AssertionError ⇒
                info("OPAL Bytecode Infrastructure", "Development Build with Assertions")
        }
    }

    final type AccessFlagsContext = AccessFlagsContexts.Value

    final type AttributeParent = AttributesParent.Value

    final type ConstantPoolTag = ConstantPoolTags.Value

    /**
     * Every Java class file starts with "0xCAFEBABE".
     */
    final val ClassFileMagic = 0xCAFEBABE

    /**
     * Returns a textual representation of the Java version used to create the respective
     * class file.
     */
    def jdkVersion(majorVersion: Int): String = {
        // 54 == 10, 53 == 9, 52 == 8; ... 50 == 6
        if (majorVersion >= 49) {
            "Java "+(majorVersion - 44)
        } else if (majorVersion > 45) {
            "Java 2 Platform version 1."+(majorVersion - 44)
        } else {
            "JDK 1.1 (JDK 1.0.2)"
        }
    }

    // previous versions are not really relevant in the context of Java bytecode
    final val Java1MajorVersion = 45
    final val Java5MajorVersion = 49
    final val Java5Version = UShortPair(0, Java5MajorVersion)
    final val Java6MajorVersion = 50
    final val Java6Version = UShortPair(0, Java6MajorVersion)
    final val Java7MajorVersion = 51
    final val Java7Version = UShortPair(0, Java7MajorVersion)
    final val Java8MajorVersion = 52
    final val Java8Version = UShortPair(0, Java8MajorVersion)
    final val Java9MajorVersion = 53
    final val Java9Version = UShortPair(0, Java9MajorVersion)
    final val Java10MajorVersion = 54
    final val Java10Version = UShortPair(0, Java10MajorVersion)

    /**
     * The latest major version supported by OPAL; this constant is adapted whenever a new version
     * is supported.
     */
    final val LatestSupportedJavaMajorVersion = Java10MajorVersion
    /**
     * The latest version supported by OPAL; this constant is adapted whenever a new version
     * is supported.
     */
    final val LatestSupportedJavaVersion = Java10Version

    /**
     * Returns `true` if the current JRE is at least Java 8 or a newer version.
     *
     * @note This method makes some assumptions how the version numbers will evolve.
     */
    final lazy val isCurrentJREAtLeastJava8: Boolean = {
        implicit val logContext: LogContext = GlobalLogContext
        val versionString = System.getProperty("java.version")
        try {
            val splittedVersionString = versionString.split('.')
            if (parseInt(splittedVersionString(0)) > 1 /*for Java <=8, the first number is "1" */ ||
                (splittedVersionString.length > 1 && parseInt(splittedVersionString(1)) >= 8)) {

                info("system configuration", s"current JRE is at least Java 8")
                true
            } else {
                info("system configuration", s"current JRE is older than Java 8")
                false // we were not able to detect/derive enough information!
            }
        } catch {
            case t: Throwable ⇒
                error("system configuration", s"could not interpret JRE version: $versionString", t)
                false
        }
    }

    val MissingLibraryWarning: String = {
        process(this.getClass.getResourceAsStream("MissingLibraryWarning.txt")) { in ⇒
            Source.fromInputStream(in).getLines.mkString("\n")
        }
    }

    final def warnMissingLibrary(implicit ctx: LogContext): Unit = {
        OPALLogger.logOnce(Warn("project configuration", MissingLibraryWarning))
    }
}
