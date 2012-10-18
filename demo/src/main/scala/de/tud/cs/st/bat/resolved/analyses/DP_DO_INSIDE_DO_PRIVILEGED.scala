package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.analyses.BaseAnalyses._
import de.tud.cs.st.bat.resolved.INVOKEVIRTUAL

/**
 *
 * Author: Ralf Mitschke
 * Date: 10.08.12
 * Time: 10:10
 *
 */
object DP_DO_INSIDE_DO_PRIVILEGED
        extends Analysis
{
    val reflectionField = ObjectType("java/lang/reflect/Field")

    val reflectionMethod = ObjectType("java/lang/reflect/Method")

    val priviledgedAction = ObjectType("java/security/PrivilegedAction")

    val priviledgedExceptionAction = ObjectType("java/security/PrivilegedExceptionAction")

    def analyze(project: Project) = {
        val classFiles: Traversable[ClassFile] = project.classFiles
        for (classFile ← classFiles
             if !classFile.interfaces.exists {
                 case `priviledgedAction` => true
                 case `priviledgedExceptionAction` => true
                 case _ => false
             };
             method ← classFile.methods if method.body.isDefined;
             (INVOKEVIRTUAL(receiver, "setAccessible", _), idx) ← withIndex(method.body.get.instructions)
             if (receiver == reflectionField || receiver == reflectionMethod)
        ) yield {
            ("DP_DO_INSIDE_DO_PRIVILEGED",
                    classFile.thisClass.toJava + "." +
                            method.name +
                            method.descriptor.toUMLNotation, idx)
        }

    }


    /**
     * ######### Findbugs code ############
     */

    /*
    @Override
    public void visit(JavaClass obj) {

        isDoPrivileged = Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedAction")
                || Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedExceptionAction");
    }

    @Override
    public void visit(Code obj) {
        if (isDoPrivileged && getMethodName().equals("run"))
            return;
        if (getMethod().isPrivate())
            return;
        if (DumbMethods.isTestMethod(getMethod()))
            return;
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("setAccessible")) {
            @DottedClassName
            String className = getDottedClassConstantOperand();
            if (className.equals("java.lang.reflect.Field") || className.equals("java.lang.reflect.Method"))
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "DP_DO_INSIDE_DO_PRIVILEGED", LOW_PRIORITY).addClassAndMethod(this)
                                .addCalledMethod(this), this);

        }
     */
}