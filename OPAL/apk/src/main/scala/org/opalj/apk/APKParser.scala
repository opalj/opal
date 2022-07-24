package org.opalj.apk


/**
 * Parses an APK file and generates a Project for it.
 * 
 * The generated Project contains the APK's Java bytecode, its native code and
 * its entry points.
 * 
 * Following external tools are utilized:
 *   - enjarify (for creating .jar from .dex)
 *   - RetDec (for lifting native code to LLVM IR)
 * 
 * @author Nicolas Gross
 */
class APKParser {
  
}
