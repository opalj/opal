/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.common.DefinitionSite
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.Type

object JavaJavaScriptTranslator {

    def Java2JavaScript(variableName: String, possibleTypes: Map[Type, Set[DefinitionSite]], value: Option[Double], project: SomeProject): (PKey.StringPKey, Value) = {

        //val simpleContexts = project.get(SimpleContextsKey)
        val propertyStore = project.get(PropertyStoreKey)

        //val declaredMethods = project.get(DeclaredMethodsKey)
        val possibleValues = new java.util.ArrayList[Value]()

        propertyStore(new Object(), AllocationSitePointsToSet.key)

        possibleTypes.foreach {
            case (tpe, defSites) => {
                if (tpe.isNumericType ||
                    tpe == ObjectType.Integer ||
                    tpe == ObjectType.Double ||
                    tpe == ObjectType.Long)
                    possibleValues.add(Value.makeAnyNum().removeAttributes())

                else if (tpe.isBooleanType || tpe == ObjectType.Boolean)
                    possibleValues.add(Value.makeAnyBool().removeAttributes())

                else if (tpe == ObjectType.String)
                    possibleValues.add(Value.makeAnyStr().removeAttributes())

                else if (tpe.isObjectType) {
                    //val path = ""
                    //val url: URL = new URL("file", null, path)
                    //val sl: SourceLocation = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
                }
            }
        }
        if (possibleValues.size() > 0) {
            (PKey.StringPKey.make(variableName), {
                if (value != null && value.isDefined)
                    Value.makeNum(value.get)
                else
                    Value.join(possibleValues).removeAttributes()
            })
        } else
            (PKey.StringPKey.make(variableName), Value.makeUndef())
    }

    def JavaScript2Java(javaScriptValueList: Map[String, Value]): Map[String, (Option[Object], Option[AllocationSite])] = {
        javaScriptValueList.map(entry => {
            val entity = entry._1
            val javaScriptValue = entry._2
            if (javaScriptValue == null)
                entity -> (None, None)
            else if (javaScriptValue.isJavaObject) {
                entity -> (
                    Some(ObjectType(javaScriptValue.getJavaName.replace(".", "/")).asInstanceOf[Object]),
                    Some(entry._2.getObjectLabels.stream().findFirst().get().getNode.getIndex.toDouble.asInstanceOf[AllocationSite])
                )
            } else if (javaScriptValue.isStrIdentifier) {
                entity -> (Some(ObjectType.String), None)
            } else if (javaScriptValue.isMaybeSingleNum ||
                javaScriptValue.isMaybeSingleNumUInt ||
                javaScriptValue.isMaybeAnyNum ||
                javaScriptValue.isMaybeFuzzyNum ||
                javaScriptValue.isMaybeNumOther ||
                javaScriptValue.isMaybeNumUInt ||
                javaScriptValue.isMaybeNumUIntPos)
                entity -> (Some(Double.box(javaScriptValue.getNum)), None) //Some(ObjectType.Double)
            else if (javaScriptValue.isMaybeObject)
                entity -> (Some(ObjectType.Object), None)
            else
                entity -> (None, None)
        })
    }
}