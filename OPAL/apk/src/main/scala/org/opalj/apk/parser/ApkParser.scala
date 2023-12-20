/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk
package parser

import java.io.File
import java.io.StringWriter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.sys.process.ProcessLogger
import scala.sys.process.stringToProcess
import scala.xml.Node
import scala.xml.XML

import org.opalj.apk.ApkComponent
import org.opalj.apk.ApkComponentsKey
import org.opalj.apk.ApkComponentType
import org.opalj.apk.ApkComponentType.ApkComponentType
import org.opalj.apk.parser.DexParser.DexParser
import org.opalj.br.analyses.Project
import org.opalj.ll.LLVMProjectKey
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time

import com.typesafe.config.Config
import net.dongliu.apk.parser.ApkFile
/**
 * Parses an APK file and generates a [[Project]] for it.
 *
 * The generated [[Project]] contains the APK's Java bytecode, its native code and its components / entry points.
 *
 * Following external tools are utilized:
 *   - enjarify or dex2jar (for creating .jar from .dex)
 *   - RetDec (for lifting native code to LLVM IR)
 *
 * @param apkPath path to the APK file.
 *
 * @author Nicolas Gross
 */
class ApkParser(val apkPath: String)(implicit config: Config) {

    implicit private val LogContext: LogContext = GlobalLogContext
    private val LogCategory = "APK parser"

    private var tmpDir: Option[File] = None
    private val ApkUnzippedDir = "/apk_unzipped"
    private val logOutput = config.getBoolean(ConfigKeyPrefix + "logOutput")
    /**
     * Parses the components / static entry points of the APK from AndroidManifest.xml.
     *
     * @return a Seq of [[ApkComponent]].
     */
    def parseComponents: Seq[ApkComponent] = {
        val apkFile = new ApkFile(apkPath)
        val manifestXmlString = apkFile.getManifestXml
        val manifestXml = XML.loadString(manifestXmlString)

        val xmlns = "http://schemas.android.com/apk/res/android"
        val nodeToEntryPoint = (compType: ApkComponentType, n: Node) =>
            new ApkComponent(
                compType,
                (n \ ("@{" + xmlns + "}name")).text, // class
                (n \\ "action" \\ ("@{" + xmlns + "}name")).map(_.text), // intents / triggers
                (n \\ "category" \\ ("@{" + xmlns + "}name")).map(_.text) // intents / triggers
            )
        val entryPoints: ListBuffer[ApkComponent] = ListBuffer.empty

        // collect all Activities, Services, Broadcast Receivers and Content Providers, which are all entry points
        val activities = manifestXml \ "application" \ "activity"
        activities.foreach(a => entryPoints.append(nodeToEntryPoint(ApkComponentType.Activity, a)))
        val services = manifestXml \ "application" \ "service"
        services.foreach(s => entryPoints.append(nodeToEntryPoint(ApkComponentType.Service, s)))
        val receivers = manifestXml \ "application" \ "receiver"
        receivers.foreach(
            r => entryPoints.append(nodeToEntryPoint(ApkComponentType.BroadcastReceiver, r))
        )
        val providers = manifestXml \ "application" \ "provider"
        providers.foreach(
            p => entryPoints.append(nodeToEntryPoint(ApkComponentType.ContentProvider, p))
        )

        entryPoints.toSeq
    }

    /**
     * Parses the Dex files of the APK.
     *
     * Uses enjarify to create .jar files from .dex files. This can take some time,
     * please be patient.
     *
     * @param dexParser: used dex file parser, defaults to Enjarify.
     * @return (directory containing all .jar files, Seq of every single .jar file).
     */
    def parseDexCode(dexParser: DexParser = DexParser.Enjarify): (Path, Seq[Path]) = {
        OPALLogger.info(LogCategory, "dex code parsing started")

        var jarsDir: Path = null
        val jarFiles: ListBuffer[Path] = ListBuffer.empty
        time {
            unzipApk()
            val apkRootDir = new File(tmpDir.get.toString + ApkUnzippedDir)
            val dexFiles = apkRootDir.listFiles
                .filter(_.isFile)
                .filter(_.getName.endsWith(".dex"))

            jarsDir = Files.createDirectory(Paths.get(tmpDir.get.toString + "/jars"))
            val getCmd = (dex: File) => {
                val dexBaseName = ApkParser.getFileBaseName(dex.toString)
                val cmd = if (dexParser == DexParser.Enjarify) {
                    s"enjarify.sh -o /jar/$dexBaseName.jar /dex/$dexBaseName.dex"
                } else {
                    s"d2j-dex2jar.sh -o /jar/$dexBaseName.jar /dex/$dexBaseName.dex"
                }
                s"docker run --rm " +
                    s"-v ${dex.getParent}:/dex " +
                    s"-v $jarsDir:/jar " +
                    s"opal-apk-parser $cmd"
            }

            // generate .jar files from .dex files, this can take some time ...
            dexFiles.zipWithIndex.foreach {
                case (dex, i) =>
                    jarFiles.append(Paths.get(s"$jarsDir/${ApkParser.getFileBaseName(dex.toString)}.jar"))
                    val (retval, _, _) = ApkParser.runCmd(getCmd(dex), logOutput)
                    if (retval != 0) {
                        throw ApkParserException(
                            "could not convert .dex files to .jar files, check if docker container was built"
                        )
                    }
                    OPALLogger.info(LogCategory, s"${i + 1} of ${dexFiles.length} dex code files parsed")
            }
        } { t =>
            OPALLogger.info(LogCategory, s"dex code parsing finished, took ${t.toSeconds}")
        }
        (jarsDir, jarFiles.toSeq)
    }

    /**
     * Parses the native code / .so files of the APK.
     *
     * Uses RetDec to lift .so .files to LLVM .bc files. This can take some time,
     * please be patient.
     *
     * @return Option(directory containing all .bc files, Seq of every single .bc file) or
     *         None if APK contains no native code.
     */
    def parseNativeCode: Option[(Path, Seq[Path])] = {
        OPALLogger.info(LogCategory, "native code parsing started")

        var llvmDir: Path = null
        val llvmFiles: ListBuffer[Path] = ListBuffer.empty
        time {
            unzipApk()
            val apkLibPath = tmpDir.get.toString + ApkUnzippedDir + "/lib"
            val archs = new File(apkLibPath).listFiles.filter(_.isDirectory).map(_.getName)
            if (!Files.isDirectory(Paths.get(apkLibPath)) || archs.isEmpty) {
                // APK does not contain native code
                return None
            }

            val soFilesPerArch = archs.map(arch => {
                val archDir = new File(apkLibPath + "/" + arch)
                val soFiles = archDir.listFiles.filter(_.isFile).filter(_.getName.endsWith(".so"))
                (archDir, soFiles)
            })

            // prefer arm64, then arm, then anything else that comes first
            val selectedArchSoFiles = soFilesPerArch.find(t => t._1.getName.startsWith("arm64")) match {
                case None =>
                    soFilesPerArch.find(t => t._1.getName.startsWith("arm")) match {
                        case None    => soFilesPerArch.head
                        case Some(t) => t
                    }
                case Some(t) => t
            }

            // generate .bc files from .so files, this can take some time ...
            llvmDir = Files.createDirectory(Paths.get(tmpDir.get.toString + "/llvm"))
            val getCmd = (soBaseName: String) =>
                s"docker run --rm " +
                    s"-v ${selectedArchSoFiles._1}:/so " +
                    s"-v $llvmDir:/llvm " +
                    s"opal-apk-parser " +
                    s"retdec-decompiler -o /llvm/$soBaseName.c /so/$soBaseName.so"
            selectedArchSoFiles._2
                .map(so => ApkParser.getFileBaseName(so.toString))
                .zipWithIndex
                .foreach {
                    case (soBaseName, i) =>
                        llvmFiles.append(Paths.get(s"$llvmDir/$soBaseName.bc"))
                        val (retval, _, _) = ApkParser.runCmd(getCmd(soBaseName), logOutput)
                        if (retval != 0) {
                            throw ApkParserException(
                                "could not convert .so files to .bc files, check if docker container was built"
                            )
                        }
                        OPALLogger.info(
                            LogCategory,
                            s"${i + 1} of ${selectedArchSoFiles._2.length} native code files parsed"
                        )
                }

        } { t =>
            OPALLogger.info(LogCategory, s"native code parsing finished, took ${t.toSeconds}")
        }
        Some((llvmDir, llvmFiles.toSeq))
    }

    /**
     * Cleans up temporary files/directories used for unzipping the APK and
     * generating .jar and .bc files.
     *
     * You should call this when you are done to not clutter up tmpfs.
     */
    def cleanUp(): Unit = tmpDir match {
        case Some(tmpDirPath) =>
            ApkParser.runCmd("rm -r " + tmpDirPath, logOutput)
            tmpDir = None
            OPALLogger.info(LogCategory, s"temporary unzip directory cleaned")
        case None =>
    }

    private[this] def unzipApk(): Unit = tmpDir match {
        case Some(_) =>
        case None =>
            val fileName = Paths.get(apkPath).getFileName
            tmpDir = Some(Files.createTempDirectory("opal_apk_" + fileName).toFile)
            val unzipDir = Files.createDirectory(Paths.get(tmpDir.get.getPath + ApkUnzippedDir))
            ApkParser.unzip(Paths.get(apkPath), unzipDir)
            OPALLogger.info(LogCategory, s"APK successfully unzipped")
    }
}

object ApkParser {

    implicit private val logContext: LogContext = GlobalLogContext

    /**
     * Creates a new [[Project]] from an APK file.
     *
     * Generation of .jar and .bc files takes some time, please be patient.
     *
     * @param apkPath path to the APK file.
     * @param projectConfig config values for the [[Project]].
     * @param dexParser: used dex file parser, defaults to Enjarify.
     * @return the newly created [[Project]] containing the APK's contents (dex code, native code and entry points).
     */
    def createProject(
        apkPath:       String,
        projectConfig: Config,
        dexParser:     DexParser = DexParser.Enjarify
    ): Project[URL] = {
        val apkParser = new ApkParser(apkPath)(BaseConfig)

        val jarDir = apkParser.parseDexCode(dexParser)._1

        val project =
            Project(
                jarDir.toFile,
                GlobalLogContext,
                projectConfig
            )

        project.updateProjectInformationKeyInitializationData(ApkComponentsKey)(
            _ => apkParser
        )
        project.get(ApkComponentsKey)

        apkParser.parseNativeCode match {
            case Some((_, llvmModules)) =>
                project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
                    _ => llvmModules.map(f => f.toString)
                )
                project.get(LLVMProjectKey)
            case None =>
        }

        apkParser.cleanUp()

        project
    }

    /**
     * Runs an external command.
     *
     * @param cmd the command that is executed.
     * @return a tuple consisting of the return code, stdout and stderr.
     */
    private def runCmd(
        cmd:       String,
        logOutput: Boolean
    ): (Int, StringWriter, StringWriter) = {
        val logCategory = "APK parser - command"
        OPALLogger.info(logCategory, s"run:  $cmd")
        val cmd_stdout = new StringWriter()
        val cmd_stderr = new StringWriter()
        val logger = if (logOutput) {
            ProcessLogger(
                o => {
                    OPALLogger.info(s"$logCategory stdout", o)
                    cmd_stdout.write(o + System.lineSeparator())
                },
                e => {
                    OPALLogger.info(s"$logCategory stderr", e)
                    cmd_stderr.write(e + System.lineSeparator())
                }
            )
        } else {
            ProcessLogger(
                o => cmd_stdout.write(o + System.lineSeparator()),
                e => cmd_stderr.write(e + System.lineSeparator())
            )
        }
        val cmd_result = cmd ! logger
        (cmd_result, cmd_stdout, cmd_stderr)
    }

    private def unzip(zipPath: Path, outputPath: Path): Unit = {
        val zipFile = new ZipFile(zipPath.toFile)
        for (entry <- zipFile.entries.asScala) {
            var path = outputPath.resolve(entry.getName)
            if (entry.isDirectory) {
                Files.createDirectories(path)
            } else {
                Files.createDirectories(path.getParent)
                while (Files.exists(path)) { // prepend _ since we can get clashes on case-insensitive filesystems
                    path = path.resolveSibling("_" + path.getFileName.toString)
                    OPALLogger.info("APK", s"Renamed $entry to ${path.getFileName}")
                }
                Files.copy(zipFile.getInputStream(entry), path)
            }
        }
    }

    private def getFileBaseName(fileName: String): String = {
        fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'))
    }
}

case class ApkParserException(
        message: String,
        cause:   Throwable = null
) extends Exception(message, cause)

object DexParser extends Enumeration {
    type DexParser = Value
    val Enjarify, Dex2Jar = Value
}
