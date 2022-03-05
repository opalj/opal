/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.{ProperPropertyComputationResult, PropertyBounds, PropertyStore}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.analyses.ifds.{ForwardNativeIFDSAnalysis, MultilingualIFDSAnalysis}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.Function
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSAnalysis, ForwardIFDSAnalysis, IFDSAnalysis, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, ForwardTaintProblem, NullFact, Variable}
import org.opalj.tac.fpcf.properties.{IFDSPropertyMetaInformation, Taint}

class SimpleJavaForwardTaintProblem(p: SomeProject) extends ForwardTaintProblem(p) {
    val llvmProject = p.get(LLVMProjectKey)

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(DeclaredMethod, Fact)] = (for {
        m ← p.allMethodsWithBody
        if (m.name == "main")
    } yield declaredMethods(m) -> NullFact)

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizeParamters(call: JavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: DeclaredMethod, call: JavaStatement): Set[Fact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(
        callee: DeclaredMethod,
        call:   JavaStatement,
        in:     Set[Fact]
    ): Option[FlowFact] =
        if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(call.method)))
        else None

    // Multilingual additions here

    override def insideAnalysisContext(callee: DeclaredMethod): Boolean = {
        super.insideAnalysisContext(callee) || callee.definedMethod.isNative
    }

  override def specialCase(source: (DeclaredMethod, Fact), propertyKey: IFDSPropertyMetaInformation[JavaStatement, Fact]): Option[ProperPropertyComputationResult] = {
      val method = source._1.definedMethod
      if (method.isNative) {
        // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/design.html#resolving-native-method-names
        val nativeMethodName = "Java_" + method.classFile.fqn + "_" + method.name
        val function = llvmProject.function(nativeMethodName)
        return Some(delegate(source, ((function.get, source._2), NativeTaint), identity, propertyKey))
      }
      super.specialCase(source, propertyKey)
  }
}

class SimpleNativeForwardTaintProblem(p: SomeProject) extends NativeForwardTaintProblem(p) {
  /**
   * The analysis starts with all public methods in TaintAnalysisTestClass.
   */
  override val entryPoints: Seq[(Function, Fact)] = Seq.empty

  /**
   * The sanitize method is a sanitizer.
   */
  override protected def sanitizesReturnValue(callee: Function): Boolean =
    callee.name == "sanitize"

  /**
   * We do not sanitize paramters.
   */
  override protected def sanitizeParamters(call: JavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

  /**
   * Creates a new variable fact for the callee, if the source was called.
   */
  override protected def createTaints(callee: DeclaredMethod, call: JavaStatement): Set[Fact] =
    if (callee.name == "source") Set(Variable(call.index))
    else Set.empty

  /**
   * Create a FlowFact, if sink is called with a tainted variable.
   * Note, that sink does not accept array parameters. No need to handle them.
   */
  override protected def createFlowFact(
                                         callee: DeclaredMethod,
                                         call:   JavaStatement,
                                         in:     Set[Fact]
                                       ): Option[FlowFact] =
    if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(call.method)))
    else None

  // Multilingual additions here

  override def insideAnalysisContext(callee: DeclaredMethod): Boolean = {
    super.insideAnalysisContext(callee) || callee.definedMethod.isNative
  }

  override def specialCase(source: (DeclaredMethod, Fact), propertyKey: IFDSPropertyMetaInformation[JavaStatement, Fact]): Option[ProperPropertyComputationResult] = {
    val method = source._1.definedMethod
    if (method.isNative) {
      // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/design.html#resolving-native-method-names
      val nativeMethodName = "Java_" + method.classFile.fqn + "_" + method.name
      val function = llvmProject.function(nativeMethodName)
      return Some(delegate(source, ((function.get, source._2), NativeTaint), identity, propertyKey))
    }
    super.specialCase(source, propertyKey)
  }
}

class SimpleJavaForwardTaintAnalysis private (implicit val project: SomeProject)
  extends ForwardIFDSAnalysis(new SimpleJavaForwardTaintProblem(project), Taint)

class SimpleNativeForwardTaintAnalysis private (implicit val project: SomeProject)
  extends ForwardNativeIFDSAnalysis(new SimpleNativeForwardTaintProblem(project), NativeTaint)

object MultilingualForwardTaintAnalysis extends MultilingualIFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaForwardTaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, Fact] = Taint

    override val uses: Set[PropertyBounds] = super.uses ++ PropertyBounds.ub(NativeTaint)

  override def register(p: SomeProject, ps: PropertyStore, analysis: AbstractIFDSAnalysis[Fact]): FPCFAnalysis = super.register(p, ps, analysis)
}
