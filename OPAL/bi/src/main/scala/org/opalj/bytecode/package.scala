/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.io.File

import scala.io.Source
import scala.collection.immutable.BitSet

import org.opalj.io.process

/**
 * Defines functionality commonly useful when processing Java bytecode.
 *
 * @author Michael Eichberg
 */
package object bytecode {

    /** The program counter of an instruction. A value in the range [0..65535]. */
    final type PC = UShort

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
     *
     * @param pkgSeparatorChar If the given fully qualified type names are using
     *      Java notation (i.e., packages are separated using '.') then the
     *      parameter should be `'.'`, which is the default, otherwise the parameter
     *      should be `'/'`.
     */
    def commonPackage(fqnA: String, fqnB: String, pkgSeparatorChar: Int = '.'): Option[String] = {
        val pkgSeparatorIndex = fqnA.indexOf(pkgSeparatorChar) + 1
        if (pkgSeparatorIndex <= 0)
            return None;

        val rootPkg = fqnA.substring(0, pkgSeparatorIndex)
        if (pkgSeparatorIndex == fqnB.indexOf(pkgSeparatorChar) + 1 &&
            rootPkg == fqnB.substring(0, pkgSeparatorIndex)) {
            val commonPkg = commonPackage(
                fqnA.substring(pkgSeparatorIndex, fqnA.length()),
                fqnB.substring(pkgSeparatorIndex, fqnB.length())
            )
            commonPkg match {
                case Some(childPackage) => Some(rootPkg + childPackage)
                case None               => Some(rootPkg)
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
     * scala> org.opalj.abbreviateType("a.b.T","a.T") // <= no abbrev.
     * res: String = a.T
     *
     * scala> org.opalj.abbreviateType("a.b.T","a.b.T")
     * res: String = …b.T
     *
     * scala> org.opalj.abbreviateType("a.b.c.T","a.b.c.T.X")
     * res: String = …c.T.X
     * }}}
     *
     * @param pkgSeparatorChar If the given fully qualified type names are using
     *      Java notation (i.e., packages are separated using '.') then the
     *      parameter should be `'.'`, which is the default, otherwise the parameter
     *      should be `'/'`.
     */
    def abbreviateType(
        definingTypeFQN:  String,
        memberTypeFQN:    String,
        pkgSeparatorChar: Int    = '.'
    ): String = {

        commonPackage(definingTypeFQN, memberTypeFQN) match {

            case Some(commonPkg) if commonPkg.indexOf(pkgSeparatorChar) < commonPkg.length - 1 =>
                // we have more than one common package...
                val beforeLastCommonPkgIndex = commonPkg.dropRight(1).lastIndexOf(pkgSeparatorChar)
                val length = memberTypeFQN.length
                val packagesCount = commonPkg.count(_ == pkgSeparatorChar) - 1
                val packageAbbreviation = "." * packagesCount
                packageAbbreviation +
                    memberTypeFQN.substring(beforeLastCommonPkgIndex + 1, length)

            case _ =>
                memberTypeFQN
        }
    }

    /**
     * Returns the most likely position of the JRE's library folder. (I.e., the
     * location in which the rt.jar file and the other jar files belonging to the
     * Java runtime environment can be found). If the rt.jar cannot be found an
     * exception is raised.
     */
    lazy val JRELibraryFolder: File = {
        val javaVersion = System.getProperty("java.version")
        if (javaVersion.startsWith("1.")) {
            val sunBootClassPath = System.getProperties().getProperty("sun.boot.class.path")
            val paths = sunBootClassPath.split(File.pathSeparator)
            paths.find(_.endsWith("rt.jar")) match {

                case Some(libPath) =>
                    new File(libPath.substring(0, libPath.length() - 6))

                case None =>
                    val sunBootLibraryPath = System.getProperty("sun.boot.library.path")
                    if (sunBootLibraryPath == null) {
                        throw new RuntimeException("cannot locate the JRE libraries")
                    } else {
                        new File(sunBootLibraryPath)
                    }
            }
        } else {
            val javaJMods = System.getProperty("java.home")+"/jmods"
            val directory = new File(javaJMods)
            if (!directory.exists())
                throw new RuntimeException("cannot locate the JRE libraries")
            directory
        }
    }

    /**
     * Returns the most likely position of the JAR/JMod that contains Java's main classes.
     */
    lazy val RTJar: File = { // TODO [Java9+] Rename to JavaBase

        val javaVersion = System.getProperty("java.version")
        if (javaVersion.startsWith("1.")) {
            val sunBootClassPath = System.getProperties().getProperty("sun.boot.class.path")
            val paths = sunBootClassPath.split(File.pathSeparator)

            paths.find(_.endsWith("rt.jar")) match {
                case Some(rtJarPath) => new File(rtJarPath)
                case None =>
                    val rtJarCandidates =
                        new File(System.getProperty("sun.boot.library.path")).listFiles(
                            new java.io.FilenameFilter() {
                                def accept(dir: File, name: String) = name == "rt.jar"
                            }
                        )
                    if (rtJarCandidates.length != 1) {
                        throw new RuntimeException("cannot locate the JRE libraries")
                    } else {
                        rtJarCandidates(0)
                    }
            }
        } else {
            val javaBaseJMod = System.getProperty("java.home")+"/jmods/java.base.jmod" // ~ rt.jar
            val file = new File(javaBaseJMod)
            if (!file.exists())
                throw new RuntimeException("cannot locate the JRE libraries")
            file
        }
    }

    /**
     * The list of all JVM instructions in the format: "<OPCODE><MNEMONIC>NewLine".
     */
    def JVMInstructions: List[(Int, String)] = {
        process(getClass.getClassLoader.getResourceAsStream("JVMInstructionsList.txt")) { stream =>
            val is = Source.fromInputStream(stream).getLines().toList.map(_.split(" ").map(_.trim))
            is.map { i =>
                val opcode = i(0)
                val mnemonic = i(1)
                (opcode.toInt, mnemonic)
            }.sorted
        }
    }

    /** The set of all valid/used opcodes. */
    def JVMOpcodes = BitSet(JVMInstructions.map(_._1): _*)
}
