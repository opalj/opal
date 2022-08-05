/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

/**
 * Entry point of an APK.
 *
 * Parsed from manifest.xml. An entry point is a tuple, consisting of the
 * class, the functions that are to be called as entry points and a list
 * of triggers (may be empty).
 *
 * @author Nicolas Gross
 */
class ApkEntryPoint(val clazz: String, val functions: Seq[String], val triggers: Seq[String])
