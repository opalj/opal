/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

import dk.brics.tajs.flowgraph.SourceLocation
import dk.brics.tajs.flowgraph.jsnodes.JavaNode
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.Value
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.pointsto
import org.opalj.tac.common.DefinitionSite

import java.net.URL

object TajsOPALTranslatorFunctions {

    def OpalToTajsValue(possibleTypes: Map[ReferenceType, Set[DefinitionSite]], project: SomeProject): Value = {

        val simpleContexts = project.get(SimpleContextsKey)

        val declaredMethods = project.get(DeclaredMethodsKey)
        val possibleValues = new java.util.ArrayList[Value]()

        possibleTypes.foreach {
            case (referenceType, defSites) => {
                if (referenceType.isNumericType ||
                    referenceType == ObjectType.Integer ||
                    referenceType == ObjectType.Double ||
                    referenceType == ObjectType.Long)
                    possibleValues.add(Value.makeAnyNum().removeAttributes())

                else if (referenceType.isBooleanType || referenceType == ObjectType.Boolean)
                    possibleValues.add(Value.makeAnyBool().removeAttributes())

                else if (referenceType == ObjectType.String)
                    possibleValues.add(Value.makeAnyStr().removeAttributes())

                else if (referenceType.isObjectType) {
                    val path = ""
                    val url: URL = new URL("file", null, path)
                    val sl: SourceLocation = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)

                    def addJavaValue(long: Long, javaName: String): Unit = {
                        val javaNode = new JavaNode(sl, long)
                        javaNode.setIndex(long.toInt)
                        val ol = ObjectLabel.make(javaNode, ObjectLabel.Kind.JAVAOBJECT)
                        ol.javaName = javaName
                        val v = Value.makeObject(ol)
                        v.setDontDelete().setDontEnum().setReadOnly()
                        possibleValues.add(v)
                    }

                    if (defSites.size > 0) {
                        defSites.foreach {
                            case defSite =>
                                val method = defSite.method
                                val simpleContext = simpleContexts(declaredMethods(method))
                                val long = pointsto.allocationSiteToLong(simpleContext, defSite.pc, referenceType.asObjectType)
                                addJavaValue(long, referenceType.asObjectType.fqn)
                        }
                    } else {
                        addJavaValue(-1, referenceType.asObjectType.fqn)
                    }
                }
            }
        }
        if (possibleValues.size() > 0)
            Value.join(possibleValues).removeAttributes()
        else
            Value.makeUndef()
    }

    def TajsToOpal(v: Value): Option[FieldType] = {
        if (v == null)
            return None
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
}
