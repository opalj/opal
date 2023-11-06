/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

import java.net.URL

import dk.brics.tajs.flowgraph.jsnodes.JNode
import dk.brics.tajs.flowgraph.SourceLocation
import dk.brics.tajs.flowgraph.AbstractNode
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.xl.Coordinator.V

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.TheTACAI

object JavaJavaScriptTranslator {

    def Java2JavaScript[PointsToSet >: Null <: PointsToSetLike[_, _, PointsToSet], ContextType](variableName: String, context: ContextType, pointsToSetLike: PointsToSet, defSites: IntTrieSet, theTACAI: TheTACAI, assignedValue: Option[V]): (PKey.StringPKey, Value) = {

        var defaultValue: Option[Value] = None

        if (pointsToSetLike != null)
            pointsToSetLike.forNewestNTypes(pointsToSetLike.numElements) {
                tpe =>
                    {
                        println(tpe)
                        if (tpe.isNumericType || tpe == ObjectType.Integer ||
                            tpe == ObjectType.Double || tpe == ObjectType.Long) {
                            //val v: Value = Value.makeAnyNum().removeAttributes()
                            //TODO possibleValues.add(v)
                        } else if (tpe.isBooleanType || tpe == ObjectType.Boolean) {
                            //TODO possibleValues.add(Value.makeAnyBool().removeAttributes())
                        } else if (tpe == ObjectType.String) {
                            //TODO possibleValues.add(Value.makeAnyStr().removeAttributes())
                        } else {
                            val path = ""
                            val url = new URL("file", null, path)
                            val sl = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)

                            pointsToSetLike.forNewestNElements(pointsToSetLike.numElements) {
                                elementType =>
                                    {
                                        val jNode =
                                            new JNode[PointsToSet, ContextType, IntTrieSet, TheTACAI](pointsToSetLike, context, defSites, theTACAI, -1, sl)
                                        val objectLabel = ObjectLabel.make(jNode, ObjectLabel.Kind.JAVAOBJECT)

                                        var tpe: ReferenceType = null
                                        pointsToSetLike.forNewestNTypes(pointsToSetLike.numElements) {
                                            t =>
                                                {
                                                    if (tpe == null)
                                                        tpe = t
                                                    else {
                                                        if (t > tpe)
                                                            tpe = t
                                                    }
                                                }
                                        }
                                        if (tpe == null)
                                            tpe = ObjectType.Object

                                        objectLabel.setJavaName(tpe.toJava)
                                        val v = Value.makeObject(objectLabel).setDontDelete().setDontEnum().setReadOnly()
                                        if (defaultValue.isDefined)
                                            defaultValue.get.join(v)
                                        else
                                            defaultValue = Some(v)
                                    }
                            }
                        }
                    }
            }

        (PKey.StringPKey.make(variableName),
            if (defaultValue.isEmpty) {
                //assignedValue.asInstanceOf[Some].value.asInstanceOf[UVar].value.asInstanceOf[ASObjectValue].leastUpperType.asInstanceOf[Some].value.asInstanceOf[ObjectType].isNumericType
                //  if(assignedValue)
                //Value.makeUnknown()
                Value.makeAbsent() //TODO
            } //Value.makeUndef()
            else {
                defaultValue.get.removeAttributes.join(Value.makeUndef()).removeAttributes()
                //  Value.join(possibleValues)
            })
    }
    def JavaScript2Java[PointsToSet, ContextType](javaScriptValues: Set[Value]): (Set[ReferenceType], Set[PointsToSet], Set[AbstractNode]) = {
        var pointsToSetSet = Set.empty[PointsToSet]
        var typesSet = Set.empty[ReferenceType]
        var jsNodes = Set.empty[AbstractNode]
        javaScriptValues.foreach(v => {
            if (v.isStrIdentifier) typesSet += ObjectType.String
            else if (v.isMaybeSingleNum || v.isMaybeSingleNumUInt || v.isMaybeAnyNum || v.isMaybeFuzzyNum ||
                v.isMaybeNumOther || v.isMaybeNumUInt || v.isMaybeNumUIntPos) {
                typesSet += ObjectType.Double
            } else if (v.isMaybeAnyBool) {
                typesSet += ObjectType.Boolean
            } else if (v.isJavaObject) {
                typesSet += ObjectType(v.getJavaName)
                v.getObjectLabels.forEach(objectLabel => {
                    if (objectLabel.getNode().
                        isInstanceOf[JNode[_, _, _, _]]) {
                        val node = objectLabel.getNode().
                            asInstanceOf[JNode[PointsToSet, ContextType, IntTrieSet, TheTACAI]]
                        pointsToSetSet += node.getType
                    }
                })
            } else if (v.isMaybeObject) {
                typesSet += ObjectType.Object
                v.getObjectLabels.forEach(ol => {
                    jsNodes = jsNodes + ol.getNode
                })
            }
        })
        (typesSet, pointsToSetSet, jsNodes)
    }
    /*
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
    } */
}