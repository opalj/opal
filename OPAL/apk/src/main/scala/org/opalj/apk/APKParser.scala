package org.opalj.apk


/**
 * Parses an APK file and generates a [[org.opalj.br.analyses.Project]] for it.
 * 
 * The generated [[org.opalj.br.analyses.Project]] contains the APK's Java
 * bytecode, its native code and its entry points.
 * 
 * Following external tools are utilized:
 *   - unzip (for unzipping the APK)
 *   - enjarify (for creating .jar from .dex)
 *   - RetDec (for lifting native code to LLVM IR)
 * 
 * @author Nicolas Gross
 */
class APKParser(val path: String) {

  //private var unzipTmpDir: Option[String] = None

  /**
   * Parses the entry points of the APK.
   *
   * @return a Seq of [[APKEntryPoint]]
   */
  def parseEntryPoints: Seq[APKEntryPoint] = {
    List.empty
  }

  /**
   * Parses the Dex files of the APK.
   * 
   * Uses enjarify to create .jar files from .dex files.
   * 
   * @return (directory containing all .jar files, Seq of every single .jar file)
   */
  def parseDexCode: (String, Seq[String]) = {
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
    ("", List.empty)
  }
}
