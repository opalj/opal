package de.tud.cs.st.bat.resolved.ai

import de.tud.cs.st.bat.resolved.Method
import de.tud.cs.st.bat.resolved.ClassFile
package object taint {

  type CallStackEntry = (ClassFile, Method)

  /**
   * Set of ids (integer values) associated with the relevant parameters passed
   * to a method.
   */
  type RelevantParameters = Seq[Int]

  // Initialized (exactly once) by the "analyze" method of the main analysis class.
  protected[taint] var restrictedPackages: Seq[String] = null

  def definedInRestrictedPackage(packageName: String): Boolean =
    restrictedPackages.exists(packageName.startsWith(_))
}