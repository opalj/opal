package de.tud.cs.st.bat.resolved.analyses.random

import de.tud.cs.st.bat.resolved._
import analyses.{Project, Analysis}
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
    extends (Project => Iterable[(ClassFile, Method, Int)])
{
    val reflectionField = ObjectType("java/lang/reflect/Field")

    val reflectionMethod = ObjectType("java/lang/reflect/Method")

    val priviledgedAction = ObjectType("java/security/PrivilegedAction")

    val priviledgedExceptionAction = ObjectType("java/security/PrivilegedExceptionAction")

    def apply(project: Project) = {
        for (classFile ← project.classFiles
             if !classFile.interfaces.exists {
                 case `priviledgedAction` => true
                 case `priviledgedExceptionAction` => true
                 case _ => false
             };
             method ← classFile.methods if method.body.isDefined;
             (INVOKEVIRTUAL(receiver, "setAccessible", _), idx) ← withIndex(method.body.get.instructions)
             if (receiver == reflectionField || receiver == reflectionMethod)
        ) yield {
            (classFile,method, idx)
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