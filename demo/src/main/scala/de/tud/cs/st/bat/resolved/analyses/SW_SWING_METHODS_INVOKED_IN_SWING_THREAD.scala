package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.analyses.BaseAnalyses._

/**
 *
 * Author: Ralf Mitschke
 * Date: 09.08.12
 * Time: 14:47
 *
 */
object SW_SWING_METHODS_INVOKED_IN_SWING_THREAD
        extends Analysis
{
    def analyze(project: Project) = {
        val classFiles: Traversable[ClassFile] = project.classFiles
        for (classFile ← classFiles;
             method ← classFile.methods if (
                    method.body.isDefined &&
                            method.isPublic &&
                            method.isStatic &&
                            method.name == "main" ||
                            classFile.thisClass.className.toLowerCase.indexOf("benchmark") >= 0
                    );
             (INVOKEVIRTUAL(targetType, name, desc), idx) ← withIndex(method.body.get.instructions)
             if (
                     targetType.isObjectType &&
                             targetType.asInstanceOf[ObjectType].className.startsWith("javax/swing/")) &&
                     (
                             name == "show" && desc == MethodDescriptor(Nil, VoidType) ||
                                     name == "pack" && desc == MethodDescriptor(Nil, VoidType) ||
                                     name == "setVisible" && desc == MethodDescriptor(List(BooleanType), VoidType)
                             )
        ) yield {
            ("SW_SWING_METHODS_INVOKED_IN_SWING_THREAD", classFile.thisClass.toJava + "." + method.name + method
                    .descriptor
                    .toUMLNotation, idx)
        }

    }

    /**
     * ###### FindBugs Code
     */
    /*
        @Override
    public void visit(Method method) {
        String cName = getDottedClassName();

        // System.out.println(getFullyQualifiedMethodName());
        isPublicStaticVoidMain = method.isPublic() && method.isStatic() && getMethodName().equals("main")
                || cName.toLowerCase().indexOf("benchmark") >= 0;

    @Override
    public void sawOpcode(int seen) {
        ... // > 500 lines of code
        if (isPublicStaticVoidMain
                && seen == INVOKEVIRTUAL
                && getClassConstantOperand().startsWith("javax/swing/")
                && (getNameConstantOperand().equals("show") && getSigConstantOperand().equals("()V")
                || getNameConstantOperand().equals("pack") && getSigConstantOperand().equals("()V") || getNameConstantOperand()
                .equals("setVisible") && getSigConstantOperand().equals("(Z)V"))) {
            accumulator.accumulateBug(
                new BugInstance(this, "SW_SWING_METHODS_INVOKED_IN_SWING_THREAD", LOW_PRIORITY).addClassAndMethod(this),
                this);
        }

   */
}