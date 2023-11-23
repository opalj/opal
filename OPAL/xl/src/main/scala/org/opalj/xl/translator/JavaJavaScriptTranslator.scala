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

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.TheTACAI

object JavaJavaScriptTranslator {

    def Java2JavaScript[PointsToSet >: Null <: PointsToSetLike[_, _, PointsToSet], ContextType](
        variableName:    String,
        context:         ContextType,
        pointsToSetLike: PointsToSet,
        theTACAI:        TheTACAI,
        tpe:             Option[ObjectType] = None
    ): (PKey.StringPKey, Value) = {

        var defaultValue = Value.makeAbsent()

        if (pointsToSetLike != null) {
            pointsToSetLike.forNewestNTypes(pointsToSetLike.numElements) { tpe =>
                {
                    if (tpe == ObjectType.String)
                        defaultValue = defaultValue.join(Value.makeAnyStr().removeAttributes())
                    if (tpe.isNumericType || tpe == ObjectType.Integer ||
                        tpe == ObjectType.Double || tpe == ObjectType.Long) {
                        defaultValue = defaultValue.join(Value.makeAnyNum().removeAttributes())
                    } else if (tpe.isBooleanType || tpe == ObjectType.Boolean) {
                        defaultValue = defaultValue.join(Value.makeAnyBool().removeAttributes())
                    } else {
                        val url = new URL("file", null, "")
                        val sl = new SourceLocation.StaticLocationMaker(url).make(0, 0, 1, 1)
                        val jNode = new JNode[PointsToSet, ContextType, IntTrieSet, TheTACAI](
                            pointsToSetLike,
                            context,
                            theTACAI,
                            -1,
                            sl
                        )
                        val javaObjectLabel = ObjectLabel.make(jNode, ObjectLabel.Kind.JAVAOBJECT)

                        pointsToSetLike.forNewestNTypes(pointsToSetLike.numElements) { tpe =>
                            {
                                javaObjectLabel.setJavaName(tpe.toJava)
                                defaultValue = defaultValue.join(Value.makeObject(javaObjectLabel))
                            }
                        }
                    }
                }
            }
            if (defaultValue == Value.makeAbsent()) {
                if (tpe != null && tpe.isDefined) {
                    val t = tpe.get
                    if (t.isBooleanType)
                        defaultValue = defaultValue.join(Value.makeAnyBool())
                    if (t.isPrimitiveTypeWrapper)
                        defaultValue = defaultValue.join(Value.makeAnyNum())
                }
            }
        }
        (PKey.StringPKey.make(variableName), defaultValue)
    }
    def JavaScript2Java[PointsToSet, ContextType](javaScriptValues: Set[Value]): (Set[ReferenceType], Set[PointsToSet], Integer) = {
        var pointsToSetSet = Set.empty[PointsToSet]
        var typesSet = Set.empty[ReferenceType]
        var jsNodes = Set.empty[AbstractNode]
        javaScriptValues.foreach(v => {
            if (v.isMaybeSingleNum ||
                v.isMaybeSingleNumUInt ||
                v.isMaybeAnyNum ||
                v.isMaybeFuzzyNum ||
                v.isMaybeNumOther ||
                v.isMaybeNumUInt ||
                v.isMaybeNumUIntPos) {
                typesSet += ObjectType.Double
            } else if (v.isMaybeAnyBool) {
                typesSet += ObjectType.Boolean
            } else if (v.isAlsoJavaObject) {
                v.getObjectLabels.forEach(objectLabel => {
                    if (objectLabel.getNode().
                        isInstanceOf[JNode[_, _, _, _]]) {
                        typesSet += ObjectType(objectLabel.getJavaName)
                        val node = objectLabel.getNode().
                            asInstanceOf[JNode[PointsToSet, ContextType, IntTrieSet, TheTACAI]]
                        pointsToSetSet += node.getPointsToSet
                        jsNodes += node
                    }
                })
            } else if (v.isJSJavaObject) {
                v.getObjectLabels.forEach(ol => {
                    typesSet += ObjectType(ol.getJavaName.replace(".", "/"))
                    v.getObjectLabels.forEach(ol => {
                        jsNodes = jsNodes + ol.getNode
                    })
                })
            } else if (v.isStrIdentifier || v.isMaybeAnyStr) {
                typesSet += ObjectType.String
            } else if (v.isMaybeObject) {
                typesSet += ObjectType.Object
                v.getObjectLabels.forEach(ol => {
                    jsNodes = jsNodes + ol.getNode
                })
            }
        })
        val index = if (jsNodes.isEmpty) -50
        else -100 - jsNodes.head.getIndex

        if (pointsToSetSet.isEmpty && typesSet.isEmpty)
            typesSet += ObjectType.Object

        (typesSet, pointsToSetSet, index)
    }
}
