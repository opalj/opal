/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package frameworkconnector

import org.opalj.tac.common.DefinitionSite

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import scala.collection.mutable

trait AnalysisInterface[State] {
  def analyze(code: String, mappings: mutable.Map[String, Set[DefinitionSite]]): State

  def resume(code: String, mappings: mutable.Map[String, Set[DefinitionSite]]): State

  def asFile(language: String, code: String): File = {
    val tmpFile: File = Files.createTempFile(language, ".js").toFile
    val fos = new FileOutputStream(tmpFile)
    fos.write(code.getBytes())
    fos.close()
    tmpFile
  }
}