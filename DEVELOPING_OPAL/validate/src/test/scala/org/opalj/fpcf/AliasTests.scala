/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.AnnotationLike
import org.opalj.br.ClassFile
import org.opalj.br.ClassValue
import org.opalj.br.Code
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.StringValue
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasEntity
import org.opalj.br.fpcf.properties.alias.AliasField
import org.opalj.br.fpcf.properties.alias.AliasFormalParameter
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.alias.AliasUVar
import org.opalj.br.fpcf.properties.alias.FieldReference
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetField
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.Stmt
import org.opalj.tac.UVar
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.fpcf.analyses.alias.LazyIntraProceduralAliasAnalysisScheduler
import org.opalj.tac.fpcf.analyses.alias.persistentUVar
import org.opalj.tac.fpcf.analyses.alias.pointsto.LazyAllocationSitePointsToBasedAliasAnalysisScheduler
import org.opalj.tac.fpcf.analyses.alias.pointsto.LazyTypePointsToBasedAliasAnalysisScheduler
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

class AllocationSiteBasedAliasTests extends AliasTests(AllocationSiteBasedPointsToCallGraphKey) {

    describe("run allocation site, points-to based alias analyses") {
        runAliasTests(LazyAllocationSitePointsToBasedAliasAnalysisScheduler)
    }

}

class TypeBasedAliasTests extends AliasTests(TypeBasedPointsToCallGraphKey) {

    describe("run type based alias analyses") {
        runAliasTests(LazyTypePointsToBasedAliasAnalysisScheduler)
    }

}

class IntraProceduralAliasTests extends AliasTests(AllocationSiteBasedPointsToCallGraphKey) {

    describe("run intraProcedural alias analysis") {
        runAliasTests(LazyIntraProceduralAliasAnalysisScheduler)
    }

}

/**
 * Tests if the alias properties defined in the classes of the package org.opalj.fpcf.fixtures.alias (and it's subpackage)
 * are computed correctly
 */
class AliasTests(final val callGraphKey: CallGraphKey) extends PropertiesTest {

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/alias")
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        }

        p.get(callGraphKey)
    }

    protected[this] def runAliasTests(scheduler: BasicFPCFLazyAnalysisScheduler): Unit = {
        implicit val as: TestContext = executeAnalyses(Set(scheduler))

        val fields = fieldsWithTypeAnnotations(as.project)
            .flatMap { case (f, fun, a) => getAliasAnnotations(a).map((f, fun, _)) }

        val allocations = allocationSitesWithAnnotations(as.project)
            .flatMap { case (ds, fun, a) => getAliasAnnotations(a).map((ds, fun, _)) }
            .map { case (ds, fun, a) => ((ds.pc, ds.method), fun, a) }

        val formalParameters = explicitFormalParametersWithAnnotations(as.project)
            .flatMap { case (ds, fun, a) => getAliasAnnotations(a).map((ds, fun, _)) }

        val methods = methodsWithAnnotations(as.project)
            .flatMap { case (m, fun, a) => getAliasAnnotations(a).map((m, fun, _)) }

        val annotationToMethod = methods.map { case (m, _, a) => (a, m) }.toMap ++
            formalParameters.map { case (ds, _, a) => (a, ds.method.definedMethod) }
        val annotationToClass = fields.map { case (f, _, a) => (a, f.classFile) }.toMap ++
            allocations.map { case ((_, method), _, a) => (a, method.classFile) } ++
            formalParameters.map { case (ds, _, a) => (a, ds.method.definedMethod.classFile) } ++
            methods.map { case (m, _, a) => (a, m.classFile) }

        implicit val simpleContexts: SimpleContexts = as.project.get(SimpleContextsKey)
        implicit val declaredMethods: DeclaredMethods = as.project.get(DeclaredMethodsKey)

        // The annotations only contain one of the two sourceElements of an alias property.
        // Therefore, we first have to combine elements with the same id and store them in this ArrayBuffer.

        val IDToEntity: Map[String, Iterable[AliasSourceElement]] =
            (fields ++ allocations ++ formalParameters ++ methods)
                .filterNot(e => isLineAlias(e._3))
                .map { case (e, _, a) =>
                    getID(a, annotationToClass(a)) -> {
                        if (isThisParameterAlias(a))
                            AliasFormalParameter(VirtualFormalParameter(declaredMethods(e.asInstanceOf[Method]), -1))
                        else AliasSourceElement(e)(as.project)
                    }
                }
                .groupMap(_._1)(_._2)

        val properties = (fields ++ allocations ++ formalParameters ++ methods)
            .map {
                case (e, fun, a) =>
                    val method = annotationToMethod.get(a)
                    val clazz = annotationToClass(a)

                    val ase1 = resolveFirstElement(e, a, method, clazz, IDToEntity)
                    val ase2 = resolveSecondElement(ase1, a, method, clazz, IDToEntity)

                    val entity = AliasEntity(
                        createContext(ase1, second = false),
                        createContext(ase2, second = true),
                        ase1,
                        ase2
                    )

                    (entity, createUniqueIdentifierFunction(a, clazz, fun), Seq(a))
            }

        // force the computation of the alias property because we are using a lazy scheduler to avoid unnecessary computations
        // (a eager scheduler would cause a quadratic blowup of the number of possible alias pairs)
        properties.foreach {
            case (e, _, _) => as.propertyStore.force(e, Alias.key)
        }

        as.propertyStore.shutdown()

        validateProperties(as, properties, Set("AliasProperty"))
    }

    /**
     * Resolves the first element of an alias relation.
     *
     * If the annotation is a alias line annotation and describes the relation between two lines or a line and null,
     * the first element is resolved using the first line number.
     *
     * Otherwise the first element is the annotated entity.
     *
     * @param e The annotated entity.
     * @param a The annotation that describes the alias relation.
     * @param IDToEntity Map from id to alias source elements.
     * @return The first element of the alias relation.
     */
    private[this] def resolveFirstElement(
        e:               AnyRef,
        a:               AnnotationLike,
        annotatedMethod: Option[Method],
        clazz:           ClassFile,
        IDToEntity:      Map[String, Iterable[AliasSourceElement]]
    )(implicit
        as:              TestContext,
        simpleContexts:  SimpleContexts,
        declaredMethods: DeclaredMethods
    ): AliasSourceElement = {
        if (isLineAlias(a) && hasTwoLines(a))
            resolveLine(a, annotatedMethod, clazz, useSecond = false)
        else if (isThisParameterAlias(a))
            AliasFormalParameter(VirtualFormalParameter(declaredMethods(e.asInstanceOf[Method]), -1))
        else
            AliasSourceElement(e)(as.project)
    }

    /**
     * Resolves the second element of an alias relation.
     *
     * If the given annotation describes an alias relation with a line, the second element is resolved using the line number.
     * If the annotation contains two line numbers, the second element is resolved using the second line number.
     *
     * Otherwise, the second element is resolved using the id of the given annotation.
     *
     * @param firstElement The first, already resolved element of the alias relation.
     * @param a The annotation that describes the alias relation.
     * @param IDToEntity Map from id to alias source elements.
     * @return The second element of the alias relation.
     */
    private[this] def resolveSecondElement(
        firstElement:    AliasSourceElement,
        a:               AnnotationLike,
        annotatedMethod: Option[Method],
        clazz:           ClassFile,
        IDToEntity:      Map[String, Iterable[AliasSourceElement]]
    )(implicit
        as:              TestContext,
        simpleContexts:  SimpleContexts,
        declaredMethods: DeclaredMethods
    ): AliasSourceElement = {
        if (isLineAlias(a))
            resolveLine(a, annotatedMethod, clazz, useSecond = hasTwoLines(a))
        else {
            val matchingEntities = IDToEntity(getID(a, clazz)).toSeq.filter(!_.equals(firstElement))
            if (matchingEntities.isEmpty)
                throw new IllegalArgumentException("No other entity with id " + getID(a, clazz) + " found")
            if (matchingEntities.size > 1)
                throw new IllegalArgumentException("Multiple other entities with id " + getID(a, clazz) + " found")
            matchingEntities.head
        }
    }

    /**
     * Resolves the alias source element that is described by the given line annotation.
     *
     * @param a The line annotation.
     * @param useSecond If true, the second line number is used to resolve the line.
     * @return The alias source element that is described by the given line annotation.
     */
    private[this] def resolveLine(
        a:               AnnotationLike,
        annotatedMethod: Option[Method],
        clazz:           ClassFile,
        useSecond:       Boolean
    )(implicit
        as:              TestContext,
        simpleContexts:  SimpleContexts,
        declaredMethods: DeclaredMethods
    ): AliasSourceElement = {
        val method = if (hasMethodName(a, useSecond))
            findMethod(clazz, getMethodName(a, useSecond))
        else annotatedMethod.get
        val tac: EOptionP[Method, TACAI] = as.propertyStore(method, TACAI.key)
        val body: Code = method.body.get
        val lineNumber = getIntValue(a, if (useSecond) "secondLineNumber" else "lineNumber")
        val isFieldReference = getFieldName(a, useSecond) != null

        val pc = body.instructions.zipWithIndex
            .filter(_._1 != null)
            .filter(inst => body.lineNumber(inst._2).isDefined && body.lineNumber(inst._2).get == lineNumber)
            .filterNot(inst =>
                inst._1.isLoadConstantInstruction || inst._1.isLoadLocalVariableInstruction || inst._1.isStackManagementInstruction
            )
            .map(_._2)
            .headOption
            .orElse(throw new IllegalArgumentException(
                "No instruction found for line number " + lineNumber + " in method " + method.toJava
            ))
            .get

        val stmts = tac.ub.tac.get.stmts
        val stmt: Stmt[DUVar[ValueInformation]] = tac.ub.tac.get.stmts(tac.ub.tac.get.pcToIndex(pc))

        def handleCall(c: Call[_]): AliasSourceElement = {
            val parameterIndex = getIntValue(a, if (useSecond) "secondParameterIndex" else "parameterIndex")
            val param: Expr[_] = if (parameterIndex == -1) c.receiverOption.get else c.params(parameterIndex)

            param match {
                case uVar: UVar[ValueInformation] => AliasUVar(persistentUVar(uVar)(stmts), method, as.project)
                case _                            => throw new IllegalArgumentException("No UVar found")
            }
        }

        def handleExpr(expr: Expr[DUVar[ValueInformation]]): AliasSourceElement = {
            expr match {
                case c: Call[_]                   => handleCall(c)
                case uVar: UVar[ValueInformation] => AliasUVar(persistentUVar(uVar)(stmts), method, as.project)
                case GetField(_, _, _, _, UVar(_, objRefDefSites)) => AliasField(FieldReference(
                        findField(a, useSecond),
                        simpleContexts(declaredMethods(method)),
                        objRefDefSites
                    ))
                case _ => throw new IllegalArgumentException("No UVar found")
            }
        }

        stmt match {
            case c: Call[_]                                    => handleCall(c)
            case expr: ExprStmt[DUVar[ValueInformation]]       => handleExpr(expr.expr)
            case putStatic: PutStatic[DUVar[ValueInformation]] => handleExpr(putStatic.value)
            case PutField(_, _, _, _, UVar(_, objRefDefSites), value: UVar[ValueInformation]) =>
                if (isFieldReference) AliasField(FieldReference(
                    findField(a, useSecond),
                    simpleContexts(declaredMethods(method)),
                    objRefDefSites
                ))
                else AliasUVar(persistentUVar(value)(stmts), method, as.project)
            case returnValue: ReturnValue[DUVar[ValueInformation]] => handleExpr(returnValue.expr)
            case Assignment(_, _, expr)                            => handleExpr(expr)
            case _ => throw new IllegalArgumentException(
                    "No UVar found at line number " + lineNumber + " in method " + method.toJava
                )
        }
    }

    /**
     * Searches for the method with the given Name in the fixture project. If no method is found, an exception is thrown.
     *
     * @param methodName The name of the method.
     * @param as The test context.
     * @return The method with the given name.
     */
    private[this] def findMethod(clazz: ClassFile, methodName: String)(implicit as: TestContext): Method = {
        as.project.allMethods.find(m => m.classFile.equals(clazz) && m.name.equals(methodName))
            .getOrElse(throw new IllegalArgumentException("No method found with name " + methodName))
    }

    /**
     * Searches for the field with the given name in the fixture project. If no field is found, an exception is thrown.
     *
     * @param as The test context.
     * @return The field with the given name.
     */
    private[this] def findField(a: AnnotationLike, useSecond: Boolean)(implicit as: TestContext): Field = {

        val clazz = getFieldClass(a, useSecond)
        val name = getFieldName(a, useSecond)

        as.project.allFields.find(f => f.classFile.thisType.fqn.equals(clazz) && f.name.equals(name))
            .getOrElse(throw new IllegalArgumentException("No field found in class " + clazz + " with name " + name))
    }

    /**
     * Ensures that the given identifier function is unique for each alias annotation by appending the id or line number
     * of the given annotation.
     * @param a The alias annotation.
     * @param fun The identifier function to expand.
     * @return A new, unique identifier function.
     */
    private[this] def createUniqueIdentifierFunction(a: AnnotationLike, clazz: ClassFile, fun: String => String)(
        implicit as: TestContext
    ): String => String = {
        a match {
            case _: AnnotationLike if isLineAlias(a) =>
                (s: String) => fun(s) + ";lineNumber=" + getIntValue(a, "lineNumber") + getStringValue(a, "reason")
            case _ => (s: String) => fun(s) + ";id=" + getID(a, clazz)
        }
    }

    /**
     * Creates a context for the given alias source element.
     */
    private[this] def createContext(ase: AliasSourceElement, second: Boolean)(
        implicit simpleContexts: SimpleContexts
    ): Context = {
        if (ase.isMethodBound) simpleContexts(ase.declaredMethod)
        else NoContext
    }

    // --- Annotation Util --- //

    private[this] def getID(a: AnnotationLike, clazz: ClassFile)(implicit as: TestContext): String = {
        clazz.thisType.simpleName + "." + getIntValue(a, "id")
    }

    private[this] def getMethodName(a: AnnotationLike, useSecond: Boolean)(implicit as: TestContext): String = {
        getStringValue(a, if (useSecond) "secondMethodName" else "methodName")
    }

    private[this] def getFieldName(a: AnnotationLike, useSecond: Boolean)(implicit as: TestContext): String = {
        getStringValue(a, if (useSecond) "secondFieldName" else "fieldName")
    }

    private[this] def getFieldClass(a: AnnotationLike, useSecond: Boolean)(implicit as: TestContext): String = {
        getStringValue(a, if (useSecond) "secondFieldClass" else "fieldClass")
    }

    private[this] def getIntValue(a: AnnotationLike, element: String)(implicit as: TestContext): Int = {
        getValue(a, element).asIntValue.value
    }

    private[this] def getStringValue(a: AnnotationLike, element: String)(implicit as: TestContext): String = {
        getValue(a, element) match {
            case str: StringValue => str.value
            case ClassValue(t)    => t.asObjectType.fqn
            case _                => throw new RuntimeException("Unexpected value type")
        }
    }

    private[this] def getValue(a: AnnotationLike, element: String)(implicit as: TestContext): ElementValue = {
        a.elementValuePairs.filter(_.name == element).collectFirst {
            case ElementValuePair(`element`, value) => value
        }.getOrElse(as.project.classFile(a.annotationType.asObjectType)
            .get
            .findMethod(element)
            .headOption
            .getOrElse(throw new RuntimeException("No element value pair found for " + element))
            .annotationDefault
            .get)
    }

    private[this] def isLineAlias(a: AnnotationLike): Boolean = {
        annotationName(a).endsWith("Line")
    }

    private[this] def hasTwoLines(a: AnnotationLike)(implicit as: TestContext): Boolean = {
        getIntValue(a, "secondLineNumber") != -1
    }

    private[this] def hasMethodName(a: AnnotationLike, useSecond: Boolean)(implicit as: TestContext): Boolean = {
        getMethodName(a, useSecond) != ""
    }

    private[this] def isThisParameterAlias(a: AnnotationLike): Boolean = {
        a.elementValuePairs.find(_.name == "thisParameter").exists(_.value.asBooleanValue.value)
    }

    /**
     * Returns all alias annotations that are contained in the given annotation.
     * The given annotation must be an alias annotation.
     * @param a The annotation.
     * @return All alias annotations that are contained in the given annotation.
     */
    private[this] def getAliasAnnotations(a: Iterable[AnnotationLike]): Iterable[AnnotationLike] = {
        a.filter(annotationName(_).contains("Alias"))
            .filter(annotationName(_) != "AliasMethodID")
            .flatMap {
                case a: AnnotationLike
                    if annotationName(a).endsWith("Lines") || annotationName(a).endsWith("Aliases") =>
                    val x = a.elementValuePairs.filter(_.name == "value")
                    x.head.value.asArrayValue.values.map(_.asAnnotationValue.annotation)

                case a => Seq(a)
            }
    }

    private[this] def annotationName(a: AnnotationLike): String = {
        a.annotationType.asObjectType.simpleName
    }

    private[this] def fieldsWithTypeAnnotations(
        recreatedFixtureProject: SomeProject
    ): Iterable[(Field, String => String, Iterable[AnnotationLike])] = {
        for {
            f <- recreatedFixtureProject.allFields // cannot be parallelized; "it" is not thread safe
            annotations = f.runtimeInvisibleTypeAnnotations
            if annotations.nonEmpty
        } yield {
            (f, (a: String) => f.toJava(s"@$a").substring(24), annotations)
        }
    }

}
