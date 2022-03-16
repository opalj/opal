/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.fpcf.{ProperPropertyComputationResult, PropertyBounds, PropertyStore}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.tac.fpcf.analyses.ifds.{ForwardIFDSAnalysis, IFDSAnalysisScheduler, JavaStatement}
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
     * We do not sanitize parameters.
     */
    override protected def sanitizeParameters(call: JavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

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
    ): Option[FlowFact[Method]] =
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
            val nativeMethodName = "Java_"+method.classFile.fqn+"_"+method.name
            val function = llvmProject.function(nativeMethodName)
            return Some(delegate(source, ((function.get, source._2), NativeTaint), identity, propertyKey))
        }
        super.specialCase(source, propertyKey)
    }
}

class SimpleJavaForwardTaintAnalysis(implicit val project: SomeProject)
    extends ForwardIFDSAnalysis(new SimpleJavaForwardTaintProblem(project), Taint)

object JavaForwardTaintAnalysisScheduler extends IFDSAnalysisScheduler[Fact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaForwardTaintAnalysis()(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, Fact] = Taint
    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++ Seq(LLVMProjectKey)
    override val uses: Set[PropertyBounds] = super.uses ++ Set(PropertyBounds.ub(NativeTaint))
}
