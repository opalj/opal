package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._
import analyses.BaseAnalyses._

/**
 *
 * Author: Ralf Mitschke
 * Date: 09.08.12
 * Time: 09:35
 *
 */
object UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR
        extends Analysis
{


    def analyze(project: Project) = {
        val classFiles: Traversable[ClassFile] = project.classFiles
        val isOverride = BaseAnalyses.isOverride(project) _
        val calledSuperConstructor = BaseAnalyses.calledSuperConstructor(project) _
        for (classFile ← classFiles;
             method ← classFile.methods if (
                    method.body.isDefined &&
                            method.name != "<init>" &&
                            !method.isStatic &&
                            isOverride(classFile)(method));
             (GETFIELD(declaringClass, name, fieldType), idx) ← withIndex(method.body.get.instructions);
             constructor ← classFile.constructors;
             field ← findField(classFile)(name, fieldType);
             (superClass, superConstructor) ← calledSuperConstructor(classFile, constructor)
             if (calls(superConstructor, superClass, method))

        ) yield {
            ("UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR", declaringClass.toJava + "." + method.name, method
                    .descriptor.toUMLNotation, name + " : " + fieldType.toJava, idx)
        }

    }


    /**
     * ###### FindBugs code
     */
    /*
    public void sawOpcode(int opcode) {
        if (opcode == PUTFIELD) {
            XField f = getXFieldOperand();
            OpcodeStack.Item item = stack.getStackItem(1);
            if (item.getRegisterNumber() != 0)
                return;
            initializedFields.add(f);
            return;
        }
        if (opcode != GETFIELD)
            return;
        OpcodeStack.Item item = stack.getStackItem(0);
        if (item.getRegisterNumber() != 0)
            return;
        XField f = getXFieldOperand();

        if (f == null || !f.getClassDescriptor().equals(getClassDescriptor()))
            return;
        if (f.isSynthetic() || f.getName().startsWith("this$"))
            return;
        if (initializedFields.contains(f))
            return;
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

        ClassDescriptor superClassDescriptor = DescriptorFactory.createClassDescriptor(getSuperclassName());
        Set<ProgramPoint> calledFrom = fieldSummary.getCalledFromSuperConstructor(superClassDescriptor, getXMethod());
        if (calledFrom.isEmpty())
            return;
        UnreadFieldsData unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFieldsData();

        int priority;
        if (!unreadFields.isWrittenInConstructor(f))
            return;

        if (f.isFinal())
            priority = HIGH_PRIORITY;
        else if (unreadFields.isWrittenDuringInitialization(f) || unreadFields.isWrittenOutsideOfInitialization(f))
            priority = NORMAL_PRIORITY;
        else
            priority = HIGH_PRIORITY;

        int nextOpcode = getNextOpcode();
        if (nullCheckedFields.contains(f) || nextOpcode == IFNULL || nextOpcode == IFNONNULL || nextOpcode == IFEQ
                || nextOpcode == IFNE) {
            priority++;
            nullCheckedFields.add(f);
        }

        for (ProgramPoint p : calledFrom) {
            XMethod upcall = getConstructorThatCallsSuperConstructor(p.method);
            if (upcall == null)
                continue;
            Method upcallMethod = null;
            for (Method m : getThisClass().getMethods()) {
                if (m.getName().equals(upcall.getName()) && m.getSignature().equals(upcall.getSignature())) {
                    upcallMethod = m;
                    break;
                }
            }
            if (upcallMethod == null)
                continue;
            Map<Integer, OpcodeStack.Item> putfieldsAt = PutfieldScanner.getPutfieldsFor(getThisClass(), upcallMethod, f);
            if (putfieldsAt.isEmpty())
                continue;
            Map.Entry<Integer, OpcodeStack.Item> e = putfieldsAt.entrySet().iterator().next();
            int pc = e.getKey();
            OpcodeStack.Item value = e.getValue();
            if (value.isNull() || value.hasConstantValue(0))
                priority++;

            SourceLineAnnotation fieldSetAt = SourceLineAnnotation.fromVisitedInstruction(getThisClass(), upcallMethod, pc);

            BugInstance bug = new BugInstance(this, "UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR", priority).addClassAndMethod(
                    this).addField(f);
            bug.addMethod(p.method).describe(MethodAnnotation.METHOD_SUPERCLASS_CONSTRUCTOR)
                    .addSourceLine(p.getSourceLineAnnotation()).describe(SourceLineAnnotation.ROLE_CALLED_FROM_SUPERCLASS_AT)
                    .addMethod(upcall).describe(MethodAnnotation.METHOD_CONSTRUCTOR).add(fieldSetAt)
                    .describe(SourceLineAnnotation.ROLE_FIELD_SET_TOO_LATE_AT);

            accumulator.accumulateBug(bug, this);
        }

    }

    private @CheckForNull
    XMethod getConstructorThatCallsSuperConstructor(XMethod superConstructor) {
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

        XMethod lookfor = superConstructor.getSignature().equals("()V") ? null : superConstructor;
        for (XMethod m : getXClass().getXMethods())
            if (m.getName().equals("<init>")) {
                if (fieldSummary.getSuperCall(m) == lookfor)
                    return m;
            }
        return null;
    }
     */
}