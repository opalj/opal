/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.BaseType
import org.opalj.br.ClassType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.Type
import org.opalj.br.VoidType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.ForNameClasses
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.PropertyStore

object TypesUtil {

    /**
     * Returns classes that may be loaded by an invocation of Class.forName.
     */
    def getPossibleForNameClasses(
        className:      Expr[V],
        stmts:          Array[Stmt[V]],
        project:        SomeProject,
        onlyClassTypes: Boolean
    ): Option[Set[ClassType]] = {
        StringUtil.getPossibleStrings(className, stmts).map(_.flatMap { cls =>
            val tpe = referenceTypeFromFQN(cls)
            if (tpe.isDefined && tpe.get.isArrayType)
                if (onlyClassTypes) None
                else Some(ClassType.Object)
            else tpe.asInstanceOf[Option[ClassType]]
        }.filter(project.classFile(_).isDefined))
    }

    /**
     * Returns classes that may be loaded by an invocation of Class.forName.
     * Clients MUST handle dependencies where the depender is the given one and the dependee
     * provides a string regex that matches the FQN class names of the loaded classes.
     */
    def getPossibleForNameClasses(
        pc:        Int,
        className: V,
        context:   Context,
        stmts:     Array[Stmt[V]],
        project:   SomeProject
    )(implicit ps: PropertyStore): Set[ClassType] = {
        val stringRegex = StringUtil.getPossibleStringsRegex(pc, className, context, stmts)
        getPossibleForNameClasses(stringRegex, project)
    }

    /**
     * Returns class that may be loaded by an invocation of Class.forName with the given String.
     */
    def getPossibleForNameClasses(
        classNameRegex:   String,
        project:          SomeProject,
    ): Set[ClassType] = {
        project.allClassFiles.filter(_.thisType.fqn.matches(classNameRegex)).map(_.thisType).toSet
    }

    @inline private[this] def referenceTypeFromFQN(fqn: String): Option[ReferenceType] = {
        if (
            fqn.matches("(^\\[+[BCDFIJSZ]$)|(^[A-Za-z](\\w|\\$)*(\\.[A-Za-z](\\w|\\$)*)*$)|(^\\[+L[A-Za-z](\\w|\\$)*(\\.[A-Za-z](\\w|\\$)*)*;$)")
        )
            Some(ReferenceType(fqn.replace('.', '/')))
        else
            None
    }

    /**
     * Returns types that a given expression potentially evaluates to.
     * Identifies local uses of Class constants, class instances returned from Class.forName,
     * by accesses to a primitive type's class as well as from Object.getClass.
     */
    def getPossibleClasses(
        value:          Expr[V],
        stmts:          Array[Stmt[V]],
        project:        SomeProject,
        onlyClassTypes: Boolean = false
    ): Option[Iterator[Type]] = {

        def isForName(expr: Expr[V]): Boolean = { // static call to Class.forName
            expr.isStaticFunctionCall &&
            (expr.asStaticFunctionCall.declaringClass eq ClassType.Class) &&
            expr.asStaticFunctionCall.name == "forName"
        }

        def isGetClass(expr: Expr[V]): Boolean = { // virtual call to Object.getClass
            expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
            expr.asVirtualFunctionCall.descriptor ==
                MethodDescriptor.withNoArgs(ClassType.Class)
        }

        var possibleTypes: Set[Type] = Set.empty
        val defSitesIterator = value.asVar.definedBy.iterator

        while (defSitesIterator.hasNext) {
            val defSite = defSitesIterator.next()
            if (defSite < 0) {
                return None;
            }
            val expr = stmts(defSite).asAssignment.expr

            if (!expr.isClassConst && !isForName(expr) && !isBaseTypeLoad(expr) &
                    !isGetClass(expr)
            ) {
                return None;
            }

            if (expr.isClassConst) {
                val tpe = stmts(defSite).asAssignment.expr.asClassConst.value
                if (tpe.isClassType || !onlyClassTypes)
                    possibleTypes += tpe
            } else if (expr.isStaticFunctionCall) {
                val className =
                    if (expr.asFunctionCall.descriptor.parameterTypes.head eq ClassType.String)
                        expr.asStaticFunctionCall.params.head
                    else
                        expr.asStaticFunctionCall.params(1)

                val possibleClassesOpt = getPossibleForNameClasses(className, stmts, project, onlyClassTypes)
                if (possibleClassesOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= possibleClassesOpt.get
            } else if (expr.isVirtualFunctionCall) {
                val typesOfVarOpt = getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar)
                if (typesOfVarOpt.isEmpty) {
                    return None;
                }

                possibleTypes ++= typesOfVarOpt.get.filter { tpe => tpe.isClassType || !onlyClassTypes }
            } else if (!onlyClassTypes) {
                possibleTypes += getBaseType(expr)
            }

        }

        Some(possibleTypes.iterator)
    }

    /**
     * Returns types that a given expression potentially evaluates to.
     * Identifies uses of Class constants, class instances returned from Class.forName,
     * by accesses to a primitive type's class as well as from Object.getClass.
     * Clients MUST handle TWO dependencies:
     * - One where the depender is the given one and the dependee are ForNameClasses and
     * - One where the depender is the given one and the dependee provides allocation sites
     */
    def getPossibleClasses(
        context:        Context,
        value:          V,
        depender:       Entity,
        stmts:          Array[Stmt[V]],
        failure:        () => Unit,
        onlyClassTypes: Boolean
    )(
        implicit
        typeIterator: TypeIterator,
        state:        TypeIteratorState,
        ps:           PropertyStore
    ): Set[Type] = {
        var possibleTypes: Set[Type] = Set.empty

        AllocationsUtil.handleAllocations(
            value,
            context,
            depender,
            stmts,
            _ eq ClassType.Class,
            failure
        ) { (allocationContext, defSite, _stmts) =>
            possibleTypes ++= getPossibleClasses(allocationContext, defSite, depender, _stmts, failure, onlyClassTypes)
        }

        possibleTypes
    }

    /**
     * Returns types provided by a given definition site.
     * Identifies uses of Class constants, class instances returned from Class.forName,
     * by accesses to a primitive type's class as well as from Object.getClass.
     * Clients MUST handle dependencies where the depender is the given one and the dependee are
     * ForNameClasses.
     */
    def getPossibleClasses(
        context:        Context,
        defSite:        Int,
        depender:       Entity,
        stmts:          Array[Stmt[V]],
        failure:        () => Unit,
        onlyClassTypes: Boolean
    )(
        implicit
        state: TypeIteratorState,
        ps:    PropertyStore
    ): Set[Type] = {
        var possibleTypes: Set[Type] = Set.empty

        val stmt = stmts(defSite).asAssignment
        val expr = stmt.expr

        if (expr.isClassConst) {
            val tpe = expr.asClassConst.value
            if (tpe.isClassType || !onlyClassTypes)
                possibleTypes += tpe
        } else if (isForName(expr)) {

            val forNameClasses = ps((context, stmt.pc), ForNameClasses.key) match {
                case eps: EPS[_, _] =>
                    if (eps.isRefinable)
                        state.addDependency(depender, eps)
                    eps.ub.classes
                case epk =>
                    state.addDependency(depender, epk)
                    UIDSet.empty
            }

            if (onlyClassTypes)
                possibleTypes ++= forNameClasses.filter(_.isClassType)
            else
                possibleTypes ++= forNameClasses

        } else if (isGetClass(expr)) {
            val typesOfVarOpt = getTypesOfVar(expr.asVirtualFunctionCall.receiver.asVar)
            if (typesOfVarOpt.isEmpty)
                failure()
            else
                possibleTypes ++= typesOfVarOpt.get.filter { tpe => tpe.isClassType || !onlyClassTypes }
        } else if (isBaseTypeLoad(expr) && !onlyClassTypes) {
            possibleTypes += getBaseType(expr)
        } else {
            // TODO Support ClassLoader.loadClass, etc.?
            failure()
        }

        possibleTypes
    }

    private[this] def isForName(expr: Expr[V]): Boolean = { // static call to Class.forName
        expr.isStaticFunctionCall &&
        (expr.asStaticFunctionCall.declaringClass eq ClassType.Class) &&
        expr.asStaticFunctionCall.name == "forName"
    }

    private[this] def isGetClass(expr: Expr[V]): Boolean = { // virtual call to Object.getClass
        expr.isVirtualFunctionCall && expr.asVirtualFunctionCall.name == "getClass" &&
        expr.asVirtualFunctionCall.descriptor == MethodDescriptor.withNoArgs(ClassType.Class)
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
     * Otherwise, an empty Iterator is returned.
     */
    private[reflection] def getTypesOfVar(
        uvar: V
    ): Option[Iterator[ReferenceType]] = {
        val value = uvar.value.asReferenceValue
        if (value.isPrecise) value.leastUpperType.map(Iterator(_))
        else if (value.allValues.forall(_.isPrecise))
            Some(value.allValues.iterator.flatMap(_.leastUpperType))
        else {
            None
        }
    }
}
