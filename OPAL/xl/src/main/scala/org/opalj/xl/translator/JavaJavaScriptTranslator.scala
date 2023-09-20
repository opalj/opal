/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

import java.net.URL

import dk.brics.tajs.flowgraph.SourceLocation
import dk.brics.tajs.flowgraph.jsnodes.JavaNode
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value

import org.opalj.br.ObjectType
import org.opalj.br.Type

object JavaJavaScriptTranslator {

    def Java2JavaScript(variableName: String, possibleTypes: List[Type], value: Option[Double]): (PKey.StringPKey, Value) = {

        val possibleValues = new java.util.ArrayList[Value]()

        possibleTypes.map(tpe => {
            if (tpe.isNumericType || tpe == ObjectType.Integer ||
                tpe == ObjectType.Double || tpe == ObjectType.Long)
                Value.makeAnyNum().removeAttributes()
            else if (tpe.isBooleanType || tpe == ObjectType.Boolean)
                possibleValues.add(Value.makeAnyBool().removeAttributes())
            else if (tpe == ObjectType.String)
                possibleValues.add(Value.makeAnyStr().removeAttributes())
            else if (tpe.isObjectType) {
                val path = ""
                val url = new URL("file", null, path)
                val sl = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
                def generateJavaValue(long: Long, javaName: String): Value = {
                    val javaNode = new JavaNode(sl, long)
                    javaNode.setIndex(long.toInt)
                    val objectLabel = ObjectLabel.make(javaNode, ObjectLabel.Kind.JAVAOBJECT)
                    objectLabel.javaName = javaName
                    val v = Value.makeObject(objectLabel)
                    v.setDontDelete().setDontEnum().setReadOnly()
                }
                generateJavaValue(-1, tpe.asObjectType.fqn)
            }
        })
        (PKey.StringPKey.make(variableName),
            if (possibleValues.isEmpty)
                Value.makeUndef()
            else {
                if (value.isDefined)
                    possibleValues.add(Value.makeNum(value.get))
                Value.join(possibleValues).removeAttributes()
            })
    }

    def JavaScript2Java(javaScriptValues: Set[Value]): Set[ObjectType] = {
        javaScriptValues.map(javaScriptValue => {
            if (javaScriptValue.isJavaObject)
                ObjectType(javaScriptValue.getJavaName.replace(".", "/"))
            else if (javaScriptValue.isStrIdentifier)
                ObjectType.String
            else if (javaScriptValue.isMaybeSingleNum || javaScriptValue.isMaybeSingleNumUInt ||
                javaScriptValue.isMaybeAnyNum || javaScriptValue.isMaybeFuzzyNum ||
                javaScriptValue.isMaybeNumOther || javaScriptValue.isMaybeNumUInt ||
                javaScriptValue.isMaybeNumUIntPos)
                ObjectType.Double
            else if (javaScriptValue.isMaybeObject)
                ObjectType.Object
            else
                null
        }).filter(_ != null)
    }
}