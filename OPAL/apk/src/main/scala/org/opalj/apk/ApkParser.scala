/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import com.typesafe.config.Config

import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import net.dongliu.apk.parser.ApkFile
import org.opalj.br.analyses.Project
import org.opalj.ll.LLVMProjectKey
import org.opalj.log.GlobalLogContext

import scala.jdk.CollectionConverters._
import scala.xml.Node
import scala.xml.XML
import sys.process._

/**
 * Parses an APK file and generates a [[Project]] for it.
 *
 * The generated [[Project]] contains the APK's Java
 * bytecode, its native code and its entry points.
 *
 * Following external tools are utilized:
 *   - enjarify or dex2jar (for creating .jar from .dex)
 *   - RetDec (for lifting native code to LLVM IR)
 *
 * @author Nicolas Gross
 */
class ApkParser(val apkPath: String) {

    private var tmpDir: Option[File] = None

  /**
     * Parses the entry points of the APK.
     *
     * @return a Seq of [[ApkEntryPoint]]
     */
    def parseEntryPoints: Seq[ApkEntryPoint] = {
        val activityEntryPoints = Seq("onActionModeFinished", "onActionModeStarted", "onActivityReenter", "onAttachFragment",
            "onAttachedToWindow", "onBackPressed", "onConfigurationChanged", "onContentChanged", "onContextItemSelected",
            "onContextMenuClosed", "onCreate", "onCreateContextMenu", "onCreateDescription", "onCreateNavigateUpTaskStack",
            "onCreateOptionsMenu", "onCreatePanelMenu", "onCreatePanelView", "onCreateThumbnail", "onCreateView",
            "onDetachedFromWindow", "onEnterAnimationComplete", "onGenericMotionEvent", "onGetDirectActions", "onKeyDown",
            "onKeyLongPress", "onKeyMultiple", "onKeyShortcut", "onKeyUp", "onLocalVoiceInteractionStarted",
            "onLocalVoiceInteractionStopped", "onLowMemory", "onMenuItemSelected", "onMenuOpened", "onMultiWindowModeChanged",
            "onNavigateUp", "onNavigateUpFromChild", "onOptionsItemSelected", "onOptionsMenuClosed", "onPanelClosed",
            "onPerformDirectAction", "onPictureInPictureModeChanged", "onPictureInPictureRequested",
            "onPictureInPictureUiStateChanged", "onPostCreate", "onPrepareNavigateUpTaskStack", "onPrepareOptionsMenu",
            "onPreparePanel", "onProvideAssistContent", "onProvideAssistData", "onProvideKeyboardShortcuts",
            "onProvideReferrer", "onRequestPermissionsResult", "onRestoreInstanceState", "onRetainNonConfigurationInstance",
            "onSaveInstanceState", "onSearchRequested", "onStateNotSaved", "onTopResumedActivityChanged", "onTouchEvent",
            "onTrackballEvent", "onTrimMemory", "onUserInteraction", "onVisibleBehindCanceled", "onWindowAttributesChanged",
            "onWindowFocusChanged", "onWindowStartingActionMode", "onActivityResult", "onApplyThemeResource",
            "onChildTitleChanged", "onCreateDialog", "onDestroy", "onNewIntent", "onPause", "onPostCreate", "onPostResume",
            "onPrepareDialog", "onRestart", "onResume", "onStart", "onStop", "onTitleChanged", "onUserLeaveHint")
        val serviceEntryPoints = Seq("onBind", "onConfigurationChanged", "onCreate", "onDestroy", "onLowMemory", "onRebind",
            "onStart", "onStartCommand", "onTaskRemoved", "onTrimMemory", "onUnbind")
        val receiverEntryPoints = Seq("onReceive")
        val providerEntryPoints = Seq("onCallingPackageChanged", "onConfigurationChanged", "onCreate", "onLowMemory",
            "onTrimMemory", "applyBatch", "bulkInsert", "call", "canonicalize", "delete", "getStreamTypes", "getType",
            "insert", "openAssetFile", "openFile", "openTypedAssetFile", "query", "refresh", "shutdown", "update")

        val apkFile = new ApkFile(apkPath)
        val manifestXmlString = apkFile.getManifestXml
        val manifestXml = XML.loadString(manifestXmlString)

        val xmlns = "http://schemas.android.com/apk/res/android"
        val nodeToEntryPoint = (n: Node, entries: Seq[String]) =>
            new ApkEntryPoint(
                (n \ ("@{"+xmlns+"}name")).text, // class
                entries, // entry points
                (n \\ "action" \\ ("@{"+xmlns+"}name")).map(_.text) // intents / triggers
            )
        var entryPoints: Seq[ApkEntryPoint] = Seq.empty

        // collect all Activities, Services, Broadcast Receivers and Content Providers, which are all entry points
        val activities = manifestXml \ "application" \ "activity"
        activities.foreach(a => entryPoints = entryPoints :+ nodeToEntryPoint(a, activityEntryPoints))
        val services = manifestXml \ "application" \ "service"
        services.foreach(s => entryPoints = entryPoints :+ nodeToEntryPoint(s, serviceEntryPoints))
        val receivers = manifestXml \ "application" \ "receiver"
        receivers.foreach(r => entryPoints = entryPoints :+ nodeToEntryPoint(r, receiverEntryPoints))
        val providers = manifestXml \ "application" \ "provider"
        providers.foreach(p => entryPoints = entryPoints :+ nodeToEntryPoint(p, providerEntryPoints))

        entryPoints
    }

    /**
     * Parses the Dex files of the APK.
     *
     * Uses enjarify to create .jar files from .dex files. This can take some time,
     * please be patient.
     *
     * @param useEnjarify: defaults to true, uses dex2jar if set to false
     * @return (directory containing all .jar files, Seq of every single .jar file)
     */
    def parseDexCode(useEnjarify: Boolean = true): (Path, Seq[Path]) = {
        // TODO docker
        // --- ONLY TEMPORARY ---
        val enjarifyPath = "/home/nicolas/git/enjarify/enjarify.sh"
        val dex2jarPath = "/home/nicolas/Downloads/dex2jar-2.1/dex-tools-2.1/d2j-dex2jar.sh"
        // --- ONLY TEMPORARY ---

        unzipApk()
        val apkRootDir = new File(tmpDir.get.toString + ApkParser.ApkUnzipped)
        val dexFiles = apkRootDir.listFiles
            .filter(_.isFile)
            .filter(_.getName.endsWith(".dex"))

        val jarsDir = Files.createDirectory(Paths.get(tmpDir.get.toString + "/jars"))
        val getJarPath = (dexPath: String) => jarsDir.toString + "/" + ApkParser.getFileBaseName(dexPath) + ".jar"
        val getCmd =
            if (useEnjarify) {
                (dex: File) => enjarifyPath + " -o " + getJarPath(dex.toString) + " " + dex
            } else {
                (dex: File) => dex2jarPath + " -o " + getJarPath(dex.toString) + " " + dex
            }

        var jarFiles: Seq[Path] = Seq.empty
        dexFiles.foreach(dex => {
            val jarName = ApkParser.getFileBaseName(dex.toString) + ".jar"
            jarFiles = jarFiles :+ Paths.get(jarName)
            if (ApkParser.runCmd(getCmd(dex))._1 != 0) {
                throw ApkParserException("could not convert .dex files to .jar files")
            }
        })

        (jarsDir, jarFiles)
    }

    /**
     * Parses the native code / .so files of the APK.
     *
     * Uses RetDec to lift .so .files to LLVM .bc files. This can take some time,
     * please be patient.
     *
     * @return (directory containing all .bc files, Seq of every single .bc file)
     */
    def parseNativeCode: (Path, Seq[Path]) = {
        // TODO docker
        // --- ONLY TEMPORARY ---
        //val retdecPath = "/home/nicolas/bin/retdec/bin/retdec-decompiler.py"
        // --- ONLY TEMPORARY ---
        //val Llvm = "/llvm"
        unzipApk()
        // TODO
        (Paths.get(""), List.empty)
    }

    /**
     * Cleans up temporary files/directories used for unzipping the APK and
     * generating .jar and .bc files.
     *
     * You should call this when you are done to not clutter up tmpfs.
     */
    def cleanUp() = tmpDir match {
        case Some(tmpDirPath) => {
            ApkParser.runCmd("rm -r " + tmpDirPath)
            tmpDir = None

        }
        case None =>
    }

    private[this] def unzipApk() = tmpDir match {
        case Some(_) =>
        case None => {
            val fileName = Paths.get(apkPath).getFileName
            tmpDir = Some(Files.createTempDirectory("opal_apk_" + fileName).toFile)
            val unzipDir = Files.createDirectory(Paths.get(tmpDir.get.getPath + ApkParser.ApkUnzipped))
            ApkParser.unzip(Paths.get(apkPath), unzipDir)
        }
    }
}

object ApkParser {

    private val ApkUnzipped = "/apk_unzipped"

    /**
     * Creates a new [[Project]] from an APK file.
     *
     * Generation of .jar and .bc files takes some time, please be patient.
     *
     * @param apkPath path to the APK file.
     * @param projectConfig config values for the [[Project]].
     * @return the newly created [[Project]] containing the APK's contents (dex code, native code and entry points).
     */
    def createProject(apkPath: String, projectConfig: Config): Project[URL] = {
        val apkParser = new ApkParser(apkPath)

        val jarDir = apkParser.parseDexCode()._1

        val project =
            Project(
                jarDir.toFile,
                GlobalLogContext,
                projectConfig
            )

        project.updateProjectInformationKeyInitializationData(ApkEntriesKey)(
            current => apkParser
        )
        project.get(ApkEntriesKey)

        val llvmModules = apkParser.parseNativeCode._2
        project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            current => llvmModules.map(f => f.toString)
        )
        project.get(LLVMProjectKey)

        apkParser.cleanUp()

        project
    }

    private def runCmd(cmd: String): (Int, ByteArrayOutputStream) = {
        val cmd_stdout = new ByteArrayOutputStream
        val cmd_result = (cmd #> cmd_stdout).!
        (cmd_result, cmd_stdout)
    }

    private def unzip(zipPath: Path, outputPath: Path) = {
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

    private def getFileBaseName(fileName: String): String = {
        fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'))
    }
}
