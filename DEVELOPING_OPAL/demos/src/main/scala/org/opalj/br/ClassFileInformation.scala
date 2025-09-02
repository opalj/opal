/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.ClassNameArg

/**
 * Loads class files and prints the signature and module related information of the classes.
 *
 * @author Michael Eichberg
 */
object ClassFileInformation extends ProjectsAnalysisApplication {

    protected class ClassFileInformationConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description =
            "Prints signature and module related information of classes"

        args(
            ClassNameArg !
        )
    }

    protected type ConfigType = ClassFileInformationConfig

    protected def createConfig(args: Array[String]): ClassFileInformationConfig = new ClassFileInformationConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ClassFileInformationConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {

        val classNames = analysisConfig(ClassNameArg).toSet

        val (project, _) = analysisConfig.setupProject(cp)

        val result = new StringBuilder()

        for {
            classFileName <- classNames
            classType = ClassType(classFileName.replace('.', '/'))
            classFile <- project.classFile(classType)
        } {

            // print the name of the type defined by this class file
            result.append(classType.toJava + "\n")

            // superclassType returns an Option, because java.lang.Object does not have a super class
            classFile.superclassType foreach { s => result.append("  extends " + s.toJava + "\n") }
            if (classFile.interfaceTypes.nonEmpty) {
                result.append(classFile.interfaceTypes.map(_.toJava).mkString("  implement ", ", ", "\n"))
            }

            // the source file attribute is an optional attribute and is only specified
            // if the compiler settings are such that debug information is added to the
            // compile class file.
            classFile.sourceFile foreach { s => result.append("\tSOURCEFILE: " + s + "\n") }

            classFile.module foreach { m =>
                result.append("\tMODULE: \n")
                if (m.requires.nonEmpty) {
                    result.append(
                        m.requires.map { r =>
                            val flags = AccessFlags.toString(r.flags, AccessFlagsContexts.MODULE)
                            s"\t\trequires $flags${r.requires};"
                        }.sorted.mkString("", "\n", "\n\n")
                    )
                }

                if (m.exports.nonEmpty) {
                    result.append(m.exports.map(_.toJava).sorted.mkString("", "\n", "\n\n"))
                }
                if (m.opens.nonEmpty) {
                    result.append(m.opens.map(_.toJava).sorted.mkString("", "\n", "\n\n"))
                }
                if (m.uses.nonEmpty) {
                    result.append(m.uses.map(use => s"uses ${use.toJava};").sorted.mkString("", "\n", "\n\n"))
                }
                if (m.provides.nonEmpty) {
                    result.append(m.provides.map(_.toJava).sorted.mkString("", "\n", "\n\n"))
                }
            }

            // The version of the class file. Basically, every major version of the
            // JDK defines additional (new) features.
            result.append(s"\tVERSION: ${classFile.majorVersion}.${classFile.minorVersion} (${bi.jdkVersion(classFile.majorVersion)})\n")

            result.append(classFile.fields.map(_.signatureToJava(false)).mkString("\tFIELDS:\n\t", "\n\t", "\n"))

            result.append(classFile.methods.map(_.signatureToJava(false)).mkString("\tMETHODS:\n\t", "\n\t", "\n\n"))
        }

        (project, BasicReport(result.toString()))
    }
}
