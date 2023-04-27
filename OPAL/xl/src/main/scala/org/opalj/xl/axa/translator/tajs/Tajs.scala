/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package translator
import dk.brics.tajs.flowgraph.SourceLocation
import dk.brics.tajs.flowgraph.jsnodes.JavaNode
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.pointsto
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.xl.analyses.javascriptAnalysis.TAJS.PROTOCOL_NAME
import org.opalj.xl.axa.detector.DetectorLattice

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import scala.collection.mutable

object Tajs {

  case class TajsTranslatorState(
      method: Method,
      var file: File = null,
      var translatorDependees: Set[EOptionP[Entity, Property]] = Set.empty,
      var pointsToMapping: mutable.Map[DefinitionSite, String] = mutable.Map.empty,
      var propertyChanges: mutable.Map[PKey.StringPKey, Value] = mutable.Map.empty,
      var call: Option[DetectorLattice] = None
  ) extends BaseAnalysisState
      with TypeIteratorState

  def OpalToTajs(possibleTypes: Set[ReferenceType], defSites: Set[DefinitionSite], project: SomeProject)(
      implicit state: TajsTranslatorState): Value = {

    val simpleContexts = project.get(SimpleContextsKey)

    val declaredMethods = project.get(DeclaredMethodsKey)
    val possibleValues: java.util.ArrayList[Value] = new java.util.ArrayList()

    possibleTypes.foreach(ft => {
      if (ft.isNumericType || ft == ObjectType.Integer || ft == ObjectType.Double || ft == ObjectType.Long)
        possibleValues.add(Value.makeAnyNum().removeAttributes())
      else if (ft.isBooleanType || ft == ObjectType.Boolean)
        possibleValues.add(Value.makeAnyBool().removeAttributes())
      else if (ft == ObjectType.String)
        possibleValues.add(Value.makeAnyStr().removeAttributes())
      else if (ft.isObjectType) {

        if (defSites.size > 0) {

          defSites
            .foreach(defSite => {
              val method = defSite.method
              val simpleContext = simpleContexts(declaredMethods(method))
              val long = pointsto.allocationSiteToLong(simpleContext, defSite.pc, ft.asObjectType)
              val url: URL = new URL("file", null, state.file.getPath)
              val sl: SourceLocation = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
              val javaNode = new JavaNode(sl, long)
              javaNode.setIndex(long.toInt)
              val ol = ObjectLabel.make(javaNode, ObjectLabel.Kind.JAVAOBJECT)
              ol.javaName = ft.asObjectType.fqn
              val v = Value.makeObject(ol)
              v.setDontDelete().setDontEnum().setReadOnly()
              possibleValues.add(v)
            })

        } else {
          val url: URL = new URL(PROTOCOL_NAME, null, state.file.getPath)
          val sl: SourceLocation = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
          val ol = ObjectLabel.make(new JavaNode(sl, -1), ObjectLabel.Kind.JAVAOBJECT)
          ol.javaName = ft.asObjectType.fqn
          val v = Value.makeObject(ol)
          v.setDontDelete().setDontEnum().setReadOnly()
        }
      }
    })
    if (possibleValues.size() > 0)
      Value.join(possibleValues).removeAttributes()
    else
      Value.makeUndef()
  }

  def TajsToOpal(v: Value): Option[FieldType] = {
    if (v.isJavaObject) {
      Some(ObjectType(v.getJavaName))
    } else if (v.isStrIdentifier) {
      Some(ObjectType.String)
    } else if (v.isMaybeSingleNum || v.isMaybeSingleNumUInt || v.isMaybeAnyNum ||
               v.isMaybeFuzzyNum || v.isMaybeNumOther || v.isMaybeNumUInt || v.isMaybeNumUIntPos)
      Some(ObjectType.Double)
    else if (v.isMaybeObject)
      Some(ObjectType.Object)
    else
      None
  }

  def asFile(language: String, code: String): File = {
    val tmpFile: File = Files.createTempFile(language, ".js").toFile
    val fos = new FileOutputStream(tmpFile)
    fos.write(code.getBytes())
    fos.close()
    tmpFile
  }
}
