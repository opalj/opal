package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.analyses.BaseAnalyses._

/**
 *
 * Author: Ralf Mitschke
 * Date: 09.08.12
 * Time: 16:28
 *
 */
object BX_BOXING_IMMEDIATELY_UNBOXED_TO_PERFORM_COERCION
        extends Analysis
{
    // With the analyzed sequence check FindBugs only finds
    // new Integer(1).doubleValue()
    // and not
    // Integer.valueOf(1).doubleValue()
    def analyze(project: Project) = {
        val classFiles: Traversable[ClassFile] = project.classFiles
        for (classFile ← classFiles if classFile.majorVersion >= 49;
             method ← classFile.methods if method.body.isDefined;
             Seq(
             (INVOKESPECIAL(firstReceiver, _ , MethodDescriptor(Seq(paramType), _)), _),
             (INVOKEVIRTUAL(secondReceiver,name, MethodDescriptor(Seq(), returnType)), idx)
             ) ← withIndex(method.body.get.instructions).sliding(2)
             if (
                     !paramType.isReferenceType &&
                             firstReceiver.asInstanceOf[ObjectType].className.startsWith("java/lang") &&
                             firstReceiver == secondReceiver &&
                             name.endsWith("Value") &&
                             returnType != paramType // coercion to another type performed
                     )
        ) yield {
            ("BX_BOXING_IMMEDIATELY_UNBOXED_TO_PERFORM_COERCION",
                    classFile.thisClass.toJava + "." +
                            method.name +
                            method.descriptor.toUMLNotation, idx)
        }
    }

    /**
     * ###### FindBugs Code
     */
    /*
    @Override
    public void visit(JavaClass obj) {
        isTigerOrHigher = obj.getMajor() >= MAJOR_1_5;

   @Override
    public void sawOpcode(int seen) {
        if (stack.isTop()) {
            ...
            return;
        }


        if (isTigerOrHigher) {
            if (previousMethodInvocation != null && prevOpCode == INVOKESPECIAL && seen == INVOKEVIRTUAL) {
                String classNameForPreviousMethod = previousMethodInvocation.getClassName();
                String classNameForThisMethod = getClassConstantOperand();
                if (classNameForPreviousMethod.startsWith("java.lang.")
                        && classNameForPreviousMethod.equals(classNameForThisMethod.replace('/', '.'))
                        && getNameConstantOperand().endsWith("Value") && getSigConstantOperand().length() == 3) {
                    if (getSigConstantOperand().charAt(2) == previousMethodInvocation.getSignature().charAt(1))
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "BX_BOXING_IMMEDIATELY_UNBOXED", NORMAL_PRIORITY).addClassAndMethod(this),
                                this);

                    else
                        bugAccumulator.accumulateBug(new BugInstance(this, "BX_BOXING_IMMEDIATELY_UNBOXED_TO_PERFORM_COERCION",
                                NORMAL_PRIORITY).addClassAndMethod(this), this);

                    ternaryConversionState = 1;
                } else
                    ternaryConversionState = 0;
     */

}
