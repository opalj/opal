/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

object StringUtil {

    /*val GetPropertyDescriptor = MethodDescriptor(ObjectType.String, ObjectType.String)
    val GetOrDefaultPropertyDescriptor =
        MethodDescriptor(RefArray(ObjectType.String, ObjectType.String), ObjectType.String)
    val GetDescriptor = MethodDescriptor(ObjectType.Object, ObjectType.Object)
    val PropertiesT = ObjectType("java/util/Properties")*/
    /**
     * Returns Strings that a given expression may evaluate to.
     * Identifies local use of String constants and Strings loaded from Properties objects.
     */
    def getPossibleStrings(
        value: Expr[V],
        pc:    Option[Int],
        stmts: Array[Stmt[V]]
    //onlyStringConsts: Boolean        = false
    ): Option[Set[String]] = {
        Some(value.asVar.definedBy.map[Set[String]] { index ⇒
            if (index >= 0) {
                val expr = stmts(index).asAssignment.expr
                // TODO we do not want this `getOrElse return` stmts
                expr match {
                    case StringConst(_, v) ⇒ Set(v)
                    /*case StaticFunctionCall(_, ObjectType.System, _, "getProperty", GetPropertyDescriptor, params) if !onlyStringConsts ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "getProperty", GetOrDefaultPropertyDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; } ++
                            getPossibleStrings(params(1), None, onlyStringConsts = true).getOrElse { return None; }
                    case VirtualFunctionCall(_, dc, _, "get", GetDescriptor, _, params) if !onlyStringConsts && ch.isSubtypeOf(dc, PropertiesT) ⇒
                        getStringConstsForSystemPropertiesKey(params.head, pc).getOrElse { return None; }*/
                    case _ ⇒
                        return None;
                }
            } else {
                return None;
            }
        }.flatten)
    }

    /*
     * Returns Strings that ma be loaded from a Properties object if the key is a constant String
     * that the given expression may evaluate to.
     */
    /*def getStringConstsForSystemPropertiesKey(
        value: Expr[V], pc: Option[Int]
    )(implicit state: FooBarState): Option[Set[String]] = {
        if (!state.hasSystemPropertiesDependee) {
            state.addSystemPropertiesDependee(propertyStore(project, SystemProperties.key))
        }
        if (pc.isDefined) {
            return None;
        }
        if (!state.hasSystemProperties) {
            Some(Set.empty)
        } else {
            val keysOpt = getPossibleStrings(value, None, onlyStringConsts = true)
            if (keysOpt.isEmpty) {
                None
            } else {
                Some(keysOpt.get.flatMap { key ⇒
                    state.systemProperties.getOrElse(key, Set.empty[String])
                })
            }
        }
    }*/
}
