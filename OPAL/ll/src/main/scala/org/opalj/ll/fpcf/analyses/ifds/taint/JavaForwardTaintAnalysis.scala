/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.fpcf._
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.value.{Function, Ret}
import org.opalj.tac.Assignment
import org.opalj.tac.fpcf.analyses.ifds.taint._
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.{TACAI, Taint}

class SimpleJavaForwardTaintProblem(p: SomeProject) extends ForwardTaintProblem(p) {
    val llvmProject = p.get(LLVMProjectKey)

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(Method, Fact)] = for {
        m ← p.allMethodsWithBody
    } yield m -> NullFact

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: Fact): Boolean = false

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: Method, call: JavaStatement): Set[Fact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(
        callee: Method,
        call:   JavaStatement,
        in:     Fact
    ): Option[FlowFact] =
        if (callee.name == "sink" && in == Variable(-2))
            Some(FlowFact(Seq(JavaMethod(call.method), JavaMethod(callee))))
        else None

    // Multilingual additions here
    override def outsideAnalysisContext(callee: Method): Option[OutsideAnalysisContextHandler] = {
        def handleNativeMethod(call: JavaStatement, successor: JavaStatement, in: Fact, dependeesGetter: Getter): Set[Fact] = {
            // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/design.html#resolving-native-method-names
            val calleeName = callee.name.map(c ⇒ c match {
                case c if isAlphaNumeric(c) ⇒ c
                case '_'                    ⇒ "_1"
                case ';'                    ⇒ "_2"
                case '['                    ⇒ "_3"
                case c                      ⇒ s"_${c.toInt.toHexString.reverse.padTo(4, '0').reverse}"
            }).mkString
            val nativeFunctionName = "Java_"+callee.classFile.fqn+"_"+calleeName
            val function = llvmProject.function(nativeFunctionName).get
            var result = Set.empty[Fact]
            val entryFacts = nativeCallFlow(call, function, in, callee)
            for (entryFact ← entryFacts) { // ifds line 14
                val e = (function, entryFact)
                val exitFacts: Map[LLVMStatement, Set[NativeFact]] =
                    dependeesGetter(e, NativeTaint.key).asInstanceOf[EOptionP[(LLVMStatement, NativeFact), IFDSProperty[LLVMStatement, NativeFact]]] match {
                        case ep: FinalEP[_, IFDSProperty[LLVMStatement, NativeFact]] ⇒
                            ep.p.flows
                        case ep: InterimEUBP[_, IFDSProperty[LLVMStatement, NativeFact]] ⇒
                            ep.ub.flows
                        case _ ⇒
                            Map.empty
                    }
                for {
                    (exitStatement, exitStatementFacts) ← exitFacts // ifds line 15.2
                    exitStatementFact ← exitStatementFacts // ifds line 15.3
                } {
                    result ++= nativeReturnFlow(exitStatement, exitStatementFact, call, in, callee, successor)
                }
            }
            result
        }

        if (callee.isNative) {
            Some(handleNativeMethod _)
        } else {
            super.outsideAnalysisContext(callee)
        }
    }

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The fact which holds before the execution of the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    private def nativeCallFlow(
        call:         JavaStatement,
        callee:       Function,
        in:           Fact,
        nativeCallee: Method
    ): Set[NativeFact] = {
        val callObject = asCall(call.stmt)
        val allParams = callObject.allParams
        val allParamsWithIndices = allParams.zipWithIndex
        in match {
            // Taint formal parameter if actual parameter is tainted
            case Variable(index) ⇒
                allParamsWithIndices.flatMap {
                    case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                        // TODO: this is passed
                        Some(NativeVariable(callee.argument(paramIndex + 1))) // offset JNIEnv
                    case _ ⇒ None // Nothing to do
                }.toSet

            // Taint element of formal parameter if element of actual parameter is tainted
            case ArrayElement(index, taintedIndex) ⇒
                allParamsWithIndices.flatMap {
                    case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                        Some(NativeVariable(callee.argument(paramIndex + 1))) // offset JNIEnv
                    case _ ⇒ None // Nothing to do
                }.toSet

            case InstanceField(index, declaredClass, taintedField) ⇒
                // Taint field of formal parameter if field of actual parameter is tainted
                // Only if the formal parameter is of a type that may have that field!
                allParamsWithIndices.flatMap {
                    case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                        Some(JavaInstanceField(paramIndex + 1, declaredClass, taintedField)) // TODO subtype check
                    case _ ⇒ None // Nothing to do
                }.toSet

            case StaticField(classType, fieldName) ⇒ Set(JavaStaticField(classType, fieldName))

            case NullFact                          ⇒ Set(NativeNullFact)

            case _                                 ⇒ Set() // Nothing to do

        }
    }

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param exit The statement, which terminated the `callee`.
     * @param in The fact which holds before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    private def nativeReturnFlow(
        exit:         LLVMStatement,
        in:           NativeFact,
        call:         JavaStatement,
        callFact:     Fact,
        nativeCallee: Method,
        successor:    JavaStatement
    ): Set[Fact] = {
        /**
         * Checks whether the callee's formal parameter is of a reference type.
         */
        def isRefTypeParam(index: Int): Boolean =
            if (index == -1) true
            else {
                val parameterOffset = if (nativeCallee.isStatic) 0 else 1
                nativeCallee.descriptor.parameterType(
                    JavaIFDSProblem.switchParamAndVariableIndex(index, nativeCallee.isStatic)
                        - parameterOffset
                ).isReferenceType
            }

        if (sanitizesReturnValue(nativeCallee)) return Set.empty
        val callStatement = asCall(call.stmt)
        val allParams = callStatement.allParams
        var flows: Set[Fact] = Set.empty
        in match {
            // Taint actual parameter if formal parameter is tainted
            case JavaVariable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                val param = allParams(
                    JavaIFDSProblem.switchParamAndVariableIndex(index, nativeCallee.isStatic)
                )
                flows ++= param.asVar.definedBy.iterator.map(Variable)

            // Taint element of actual parameter if element of formal parameter is tainted
            case JavaArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                val param = allParams(
                    JavaIFDSProblem.switchParamAndVariableIndex(index, nativeCallee.isStatic)
                )
                flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

            case JavaInstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                // Taint field of actual parameter if field of formal parameter is tainted
                val param =
                    allParams(JavaIFDSProblem.switchParamAndVariableIndex(index, nativeCallee.isStatic))
                param.asVar.definedBy.foreach { defSite ⇒
                    flows += InstanceField(defSite, declClass, taintedField)
                }

            case JavaStaticField(objectType, fieldName) ⇒ flows += StaticField(objectType, fieldName)

            // Track the call chain to the sink back
            case NativeFlowFact(flow) if !flow.contains(JavaMethod(call.method)) ⇒
                flows += FlowFact(JavaMethod(call.method) +: flow)
            case NativeNullFact ⇒ flows += NullFact
            case _              ⇒
        }

        // Propagate taints of the return value
        exit.instruction match {
            case ret: Ret ⇒ {
                in match {
                    case NativeVariable(value) if value == ret.value && call.stmt.astID == Assignment.ASTID ⇒
                        flows += Variable(call.index)
                    // TODO
                    /*case ArrayElement(index, taintedIndex) if returnValueDefinedBy.contains(index) ⇒
            flows += ArrayElement(call.index, taintedIndex)
          case InstanceField(index, declClass, taintedField) if returnValueDefinedBy.contains(index) ⇒
            flows += InstanceField(call.index, declClass, taintedField)*/
                    case NativeNullFact ⇒
                        val taints = createTaints(nativeCallee, call)
                        if (taints.nonEmpty) flows ++= taints
                    case _ ⇒ // Nothing to do
                }
            }
            case _ ⇒
        }
        flows
    }

    private def isAlphaNumeric(char: Char): Boolean = {
        char >= 'a' && char <= 'z' || char >= 'A' && char <= 'Z' || char >= '0' && char <= '9'
    }
}

class SimpleJavaForwardTaintAnalysis(project: SomeProject)
    extends IFDSAnalysis()(project, new SimpleJavaForwardTaintProblem(project), Taint)

object JavaForwardTaintAnalysisScheduler extends IFDSAnalysisScheduler[Fact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaForwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, Fact] = Taint
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI), PropertyBounds.ub(NativeTaint))
}
