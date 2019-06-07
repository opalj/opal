/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection
import org.opalj.br.BaseType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.VoidType
import org.opalj.br.analyses.SomeProject

object TypesUtil {

    /**
     * Returns classes that may be loaded by an invocation of Class.forName.
     */
    def getPossibleForNameClasses(
        className: Expr[V],
        pc:        Option[Int],
        stmts:     Array[Stmt[V]],
        project:   SomeProject
    ): Option[Set[ObjectType]] = {
        val classNamesOpt = StringUtil.getPossibleStrings(className, pc, stmts)
        classNamesOpt.map(_.map(cls ⇒
            ObjectType(cls.replace('.', '/'))).filter(project.classFile(_).isDefined))
    }

    /**
     * Returns types that a given expression potentially evaluates to.
     * Identifies local uses of Class constants, class instances returned from Class.forName,
     * by accesses to a primitive type's class as well as from Object.getClass.
     */
    def getPossibleTypes(
        value:   Expr[V],
        pc:      Int,
        stmts:   Array[Stmt[V]],
        project: SomeProject
    ): Option[Iterator[Type]] = {

        def isForName(expr: Expr[V]): Boolean = { // static call to Class.forName
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.Class) &&
                expr.asStaticFunctionCall.name == "forName"
        }

        def isGetClass(expr: Expr[V]): Boolean = { // virtual call to Object.getClass
            expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
                expr.asVirtualFunctionCall.descriptor == MethodDescriptor.withNoArgs(ObjectType.Class)
        }

        var possibleTypes: Set[Type] = Set.empty
        val defSitesIterator = value.asVar.definedBy.iterator

        while (defSitesIterator.hasNext) {
            val defSite = defSitesIterator.next()
            if (defSite < 0) {
                return None;
            }
            val expr = stmts(defSite).asAssignment.expr

            if (!expr.isClassConst && !isForName(expr) && !isBaseTypeLoad(expr) & !isGetClass(expr)) {
                return None;
            }

            if (expr.isClassConst) {
                possibleTypes += stmts(defSite).asAssignment.expr.asClassConst.value
            } else if (expr.isStaticFunctionCall) {
                val possibleClassesOpt = getPossibleForNameClasses(
                    expr.asStaticFunctionCall.params.head, Some(pc), stmts, project
                )
                if (possibleClassesOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= possibleClassesOpt.get
            } else if (expr.isVirtualFunctionCall) {
                val typesOfVarOpt = getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar, pc)
                if (typesOfVarOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= typesOfVarOpt.get
            } else {
                possibleTypes += getBaseType(expr)
            }

        }

        Some(possibleTypes.iterator)
    }

    /**
     * Returns true if a given expression is a GetField instruction that retrieves the TYPE field of
     * a primitive types wrapper class, i.e. the class for the primitive type.
     */
    private[reflection] def isBaseTypeLoad(expr: Expr[V]): Boolean = {
        expr.isGetStatic && expr.asGetStatic.name == "TYPE" && {
            val declClass = expr.asGetStatic.declaringClass
            declClass == VoidType.WrapperType ||
                BaseType.baseTypes.iterator.map(_.WrapperType).contains(declClass)
        }
    }

    /**
     * Returns the [org.opalj.br.Type] for a primitive type where the given expression is a
     * GetStatic expression for the TYPE field of the primitive type's wrapper type.
     */
    private[reflection] def getBaseType(expr: Expr[V]): Type = {
        val declClass = expr.asGetStatic.declaringClass
        if (declClass == VoidType.WrapperType) VoidType
        else BaseType.baseTypes.iterator.find(declClass == _.WrapperType).get
    }

    /**
     * Retrieves the possible runtime types of a local variable if they are known precisely.
     * Otherwise, the call site is marked as incomplete and an empty Iterator is returned.
     */
    private[this] def getTypesOfVar(
        uvar: V,
        pc:   Int
    ): Option[Iterator[ReferenceType]] = {
        val value = uvar.value.asReferenceValue
        if (value.isPrecise) value.leastUpperType.map(Iterator(_))
        else if (value.allValues.forall(_.isPrecise))
            Some(value.allValues.toIterator.flatMap(_.leastUpperType))
        else {
            None
        }
    }

}
