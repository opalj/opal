/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
//import org.opalj.tac.fpcf.analyses.cg.V

object MethodHandlesUtil {
    private[reflection] def retrieveMatchersForMethodHandleConst(
        declaringClass:      ReferenceType,
        name:                String,
        fieldType:           FieldType,
        actualReceiverTypes: Option[Set[ObjectType]],
        isStatic:            Boolean
    )(implicit project: SomeProject): Set[FieldMatcher] = {
        Set(
            new TypeBasedFieldMatcher(fieldType),
            new NameBasedFieldMatcher(Set(name)),
            if (isStatic) StaticFieldMatcher else NonStaticFieldMatcher,
            if (declaringClass.isArrayType)
                new ClassBasedFieldMatcher(Set(ObjectType.Object), onlyFieldsExactlyInClass = false)
            else if (!isStatic)
                if (actualReceiverTypes.isDefined)
                new ClassBasedFieldMatcher(
                actualReceiverTypes.get,
                onlyFieldsExactlyInClass = false
            )
            else
                new ClassBasedFieldMatcher(
                    project.classHierarchy.allSubtypes(declaringClass.asObjectType, reflexive = true),
                    onlyFieldsExactlyInClass = false
                )
            else
                new ClassBasedFieldMatcher(Set(declaringClass.asObjectType), onlyFieldsExactlyInClass = false)
        )
    }

    /*
    private[reflection] def retrieveDescriptorBasedMethodMatcher(
        descriptorOpt: Option[MethodDescriptor],
        expr:          Expr[V],
        isStatic:      Boolean,
        isConstructor: Boolean,
        stmts:         Array[Stmt[V]],
        project:       SomeProject
    ): FieldMatcher = {
        val descriptorsOpt =
            if (descriptorOpt.isDefined) {
                descriptorOpt.map { md =>
                    // for instance methods, we need to peel off the receiver type
                    if (!isStatic && !isConstructor) {
                        // but the method handle might not match the expected descriptor
                        if (md.parameterTypes.isEmpty)
                            Set.empty[MethodDescriptor]
                        else
                            Set(MethodDescriptor(md.parameterTypes.tail, md.returnType))
                    } else
                        Set(md)
                }
            } else
                getPossibleDescriptorsForMethodTypes(expr, stmts, project).map(_.toSet)

        val actualDescriptorOpt =
            if (isConstructor)
                // for constructor
                descriptorsOpt.map(_.map { md =>
                    MethodDescriptor(md.parameterTypes, VoidType)
                })
            else
                descriptorsOpt

        // there should be always other information that strongly identifies potential methods,
        // e.g. name or classes.
        MatcherUtil.retrieveSuitableNonEssentialMatcher[Set[MethodDescriptor]](
            actualDescriptorOpt,
            v => new DescriptorBasedMethodMatcher(v)
        )
    }

   */

    /*
    /**
     * Returns method types (aka. descriptors) that a given expression potentially evaluates to.
     * Identifies local use of MethodType constants as well as method types acquired from
     * MethodType.methodType.
     */
    private[reflection] def getPossibleDescriptorsForMethodTypes(
        value:   Expr[V],
        stmts:   Array[Stmt[V]],
        project: SomeProject
    ): Option[Iterator[MethodDescriptor]] = {

        def isMethodType(expr: Expr[V]): Boolean = {
            expr.isStaticFunctionCall &&
                (expr.asStaticFunctionCall.declaringClass eq ObjectType.MethodType) &&
                expr.asStaticFunctionCall.name == "methodType"
        }

        val defSitesIterator = value.asVar.definedBy.iterator

        var possibleMethodTypes: Iterator[MethodDescriptor] = Iterator.empty
        while (defSitesIterator.hasNext) {
            val defSite = defSitesIterator.next()

            if (defSite < 0) {
                return None;
            }
            val expr = stmts(defSite).asAssignment.expr
            val isResolvable = expr.isMethodTypeConst || isMethodType(expr)
            if (!isResolvable) {
                return None;
            }

            if (expr.isMethodTypeConst)
                possibleMethodTypes ++=
                    Iterator(stmts(defSite).asAssignment.expr.asMethodTypeConst.value)
            else {
                val call = expr.asStaticFunctionCall
                val pmtOpt = getPossibleMethodTypes(
                    call.params, call.descriptor, stmts, project
                )
                if (pmtOpt.isEmpty) {
                    return None;
                }
                possibleMethodTypes ++= pmtOpt.get
            }
        }

        Some(possibleMethodTypes)
    }

   */

    /*
    /**
     * Returns method types that a call to MethodType.methodType may return.
     */
    private[this] def getPossibleMethodTypes(
        params:     Seq[Expr[V]],
        descriptor: MethodDescriptor,
        stmts:      Array[Stmt[V]],
        project:    SomeProject
    ): Option[Iterator[MethodDescriptor]] = {
        val returnTypesOpt = TypesUtil.getPossibleClasses(params.head, stmts, project)

        if (returnTypesOpt.isEmpty) {
            // IMPROVE: we could add all return types for method descriptors
            return None;
        }

        val returnTypes = returnTypesOpt.get

        if (params.size == 1) { // methodType(T) => ()T
            Some(returnTypes.map(MethodDescriptor.withNoArgs))
        } else if (params.size == 3) { // methodType(T1, T2, T3, ...) => (T2, T3, ...)T1
            val firstParamTypesOpt =
                TypesUtil.getPossibleClasses(params(1), stmts, project)
            if (firstParamTypesOpt.isEmpty) {
                return None;
            }

            val possibleOtherParamTypes = VarargsUtil.getTypesFromVararg(params(2), stmts)
            if (possibleOtherParamTypes.isEmpty) {
                return None;
            }

            val possibleTypes = for {
                otherParamTypes <- possibleOtherParamTypes.iterator // empty seq. if None
                returnType <- returnTypes
                firstParamType <- firstParamTypesOpt.get.asInstanceOf[Iterator[FieldType]]
            } yield MethodDescriptor(firstParamType +: otherParamTypes, returnType)

            Some(possibleTypes)

        } else {
            val secondParamType = descriptor.parameterType(1)
            if (secondParamType.isArrayType) { // methodType(T1, Class[]{T2, ...}) => (T2, ...)T1
                val possibleOtherParamTypes = VarargsUtil.getTypesFromVararg(params(1), stmts)

                if (possibleOtherParamTypes.isEmpty) {
                    return None;
                }

                val possibleMethodDescriptorsIterator =
                    for {
                        otherParamTypes <- possibleOtherParamTypes.get.iterator
                        returnType <- returnTypes
                    } yield MethodDescriptor(otherParamTypes, returnType)
                Some(possibleMethodDescriptorsIterator)
            } else if (secondParamType == ObjectType.Class) { // methodType(T1, T2) => (T2)T2
                val paramTypesOpt =
                    TypesUtil.getPossibleClasses(params(1), stmts, project)
                if (paramTypesOpt.isEmpty) {
                    return None;
                }
                val paramTypes = paramTypesOpt.get.asInstanceOf[Iterator[FieldType]]
                Some(for {
                    returnType <- returnTypes
                    paramType <- paramTypes
                } yield MethodDescriptor(paramType, returnType))
            } else { // we don't handle methodType(T1, List(T2, ...)) and methodType(T1, MethodType)
                None
            }
        }
    }

   */
}
