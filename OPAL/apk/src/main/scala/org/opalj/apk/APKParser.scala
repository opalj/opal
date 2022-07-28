package org.opalj.apk

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import sys.process._

import scala.jdk.CollectionConverters._

/**
 * Parses an APK file and generates a [[org.opalj.br.analyses.Project]] for it.
 * 
 * The generated [[org.opalj.br.analyses.Project]] contains the APK's Java
 * bytecode, its native code and its entry points.
 * 
 * Following external tools are utilized:
 *   - enjarify or dex2jar(for creating .jar from .dex)
 *   - RetDec (for lifting native code to LLVM IR)
 * 
 * @author Nicolas Gross
 */
class APKParser(val apkPath: String) {

  // only temporary
  val enjarifyPath = "/home/nicolas/git/enjarify/enjarify.sh"
  val dex2jarPath = "/home/nicolas/Downloads/dex2jar-2.1/dex-tools-2.1/d2j-dex2jar.sh"
  val retdecPath = "/home/nicolas/bin/retdec/bin/retdec-decompiler.py"
  // only temporary


  private var tmpDir: Option[File] = None

  /**
   * Parses the entry points of the APK.
   *
   * @return a Seq of [[APKEntryPoint]]
   */
  def parseEntryPoints: Seq[APKEntryPoint] = {
    // TODO
    // currently only class names, needs function names
    List.empty
  }

  /**
   * Parses the Dex files of the APK.
   * 
   * Uses enjarify to create .jar files from .dex files.
   * 
   * @param useEnjarify: defaults to true, uses dex2jar if set to false
   * @return (directory containing all .jar files, Seq of every single .jar file)
   */
  def parseDexCode(useEnjarify: Boolean = true): (String, Seq[String]) = {
    unzipAPK()
    ("", List.empty)
  }

  /**
   * Parses the native code / .so files of the APK.
   * 
   * Uses RetDec to lift .so .files to LLVM .bc files.
   * 
   * @return (directory containing all .bc files, Seq of every single .bc file)
   */
  def parseNativeCode: (String, Seq[String]) = {
    unzipAPK()
    ("", List.empty)
  }

  /**
   * Cleans up temporary files/directories used for unzipping the APK and
   * generating .jar and .bc files.
   * 
   * You should call this when you are done to not clutter up tmpfs.
   */
  def close() = tmpDir match {
    case Some(tmpDirPath) => {
      APKParser.runCmd("rm -r " + tmpDirPath)
      tmpDir = None
    }
    case None =>
  }

  private[this] def unzipAPK() = tmpDir match {
    case Some(_) =>
    case None => {
      val fileName = Paths.get(apkPath).getFileName
      tmpDir = Some(Files.createTempDirectory("opal_apk_" + fileName).toFile)
      val unzipDir = Files.createDirectory(Paths.get(tmpDir.get.getAbsolutePath + "/apk_contents"))
      APKParser.unzip(Paths.get(apkPath), unzipDir)
    }
  }
}

object APKParser {

  /**
   * Runs an external command.
   * 
   * @return (return value, stdout)
   */
  private def runCmd(cmd: String): (Int, ByteArrayOutputStream) = {
    val cmd_stdout = new ByteArrayOutputStream
    val cmd_result = (cmd #> cmd_stdout).!
    return (cmd_result, cmd_stdout)
  }

  private def unzip(zipPath: Path, outputPath: Path): Unit = {
    val zipFile = new ZipFile(zipPath.toFile)
    for (entry <- zipFile.entries.asScala) {
      val path = outputPath.resolve(entry.getName)
      if (entry.isDirectory) {
        Files.createDirectories(path)
      } else {
        Files.createDirectories(path.getParent)
        Files.copy(zipFile.getInputStream(entry), path)
      }
    }
  }
}
