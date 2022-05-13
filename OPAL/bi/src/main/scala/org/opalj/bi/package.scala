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
            case _: AssertionError =>
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
        // 62 == 18, 61 == 17, 60 == 16, 59 == 15, 58 == 14, 57 == 13, 56 == 12, 55 == 11, 54 == 10, 53 == 9,
        // 52 == 8, 51 == 7, 50 == 6, 49 == 5.0, 48 == 1.4, 47 == 1.3, 46 == 1.2, 45 == 1.1/1.0.2
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
    final val Java1_2MajorVersion = 46
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
    final val Java11MajorVersion = 55
    final val Java11Version = UShortPair(0, Java11MajorVersion)
    final val Java12MajorVersion = 56
    final val Java12Version = UShortPair(0, Java12MajorVersion)
    final val Java13MajorVersion = 57
    final val Java13Version = UShortPair(0, Java13MajorVersion)
    final val Java14MajorVersion = 58
    final val Java14Version = UShortPair(0, Java14MajorVersion)
    final val Java15MajorVersion = 59
    final val Java15Version = UShortPair(0, Java15MajorVersion)
    final val Java16MajorVersion = 60
    final val Java16Version = UShortPair(0, Java16MajorVersion)
    final val Java17MajorVersion = 61
    final val Java17Version = UShortPair(0, Java17MajorVersion)
    final val Java18MajorVersion = 62
    final val Java18Version = UShortPair(0, Java18MajorVersion)

    /**
     * The latest major version supported by OPAL; this constant is adapted whenever a new version
     * is supported.
     */
    final val LatestSupportedJavaMajorVersion = Java18MajorVersion
    /**
     * The latest version supported by OPAL; this constant is adapted whenever a new version
     * is supported.
     */
    final val LatestSupportedJavaVersion = Java18Version

    /**
     * Returns `true` if the current JRE is at least Java 8 or a newer version.
     *
     * @note This method makes some assumptions how the version numbers will evolve.
     */
    final lazy val isCurrentJREAtLeastJava8: Boolean = isCurrentJREAtLeastJavaX(8)

    final lazy val isCurrentJREAtLeastJava10: Boolean = isCurrentJREAtLeastJavaX(10)

    final lazy val isCurrentJREAtLeastJava11: Boolean = isCurrentJREAtLeastJavaX(11)

    final lazy val isCurrentJREAtLeastJava15: Boolean = isCurrentJREAtLeastJavaX(15)

    final lazy val isCurrentJREAtLeastJava16: Boolean = isCurrentJREAtLeastJavaX(16)

    final lazy val isCurrentJREAtLeastJava17: Boolean = isCurrentJREAtLeastJavaX(17)

    // only works for Java 8 and above
    private[this] def isCurrentJREAtLeastJavaX(x: Int): Boolean = {
        require(x >= 8)
        implicit val logContext: LogContext = GlobalLogContext
        val versionString = System.getProperty("java.version")
        try {
            val isAtLeastSpecifiedJavaVersion = versionString.split('.') match {
                case Array("1", "8", _*)     => x == 8
                case Array(majorVersion, _*) => parseInt(majorVersion) >= x
            }
            if (isAtLeastSpecifiedJavaVersion) {
                info("system configuration", s"current JRE is at least Java $x")
                true
            } else {
                info("system configuration", s"current JRE is older than Java $x")
                false // we were not able to detect/derive enough information!
            }
        } catch {
            case t: Throwable =>
                error("system configuration", s"could not interpret JRE version: $versionString", t)
                false
        }
    }

    val MissingLibraryWarning: String = {
        process(this.getClass.getResourceAsStream("MissingLibraryWarning.txt")) { in =>
            Source.fromInputStream(in).getLines().mkString("\n")
        }
    }

    final def warnMissingLibrary(implicit ctx: LogContext): Unit = {
        OPALLogger.logOnce(Warn("project configuration", MissingLibraryWarning))
    }
}
