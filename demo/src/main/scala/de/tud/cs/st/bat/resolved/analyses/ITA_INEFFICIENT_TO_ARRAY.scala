package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._

/**
 *
 * Author: Ralf Mitschke
 * Date: 06.08.12
 * Time: 15:22
 *
 */
object ITA_INEFFICIENT_TO_ARRAY
{

    val objectArrayType = ArrayType(ObjectType("java/lang/Object"))

    val toArrayDescriptor = MethodDescriptor(List(objectArrayType), objectArrayType)

    val collectionInterface = ObjectType("java/util/Collection")

    def isCollectionType(classHierarchy: ClassHierarchy)(t: ObjectType) = {
        classHierarchy.isSubtypeOf(t, collectionInterface)
    }

    def analyze(classFiles: Traversable[ClassFile], classHierarchy: ClassHierarchy) = {
        val isCollectionType = isCollectionType(classHierarchy) _
        for (classFile ← classFiles;
             method ← classFile.methods if method.body.isDefined;

        ) yield {
            val calls = for (i ← 2 to method.body.get.instructions.length - 1;
                 current ← method.body.get.instructions(i) if (
                        instruction match {
                            case INVOKEINTERFACE(targetType, "toArray", `toArrayDescriptor`)
                                if (isCollectionType(targetType)) => true
                            case INVOKEVIRTUAL(targetType, "toArray", `toArrayDescriptor`)
                                if (isCollectionType(targetType)) => true
                            case _ => false
                        }
                        );
                 ANEWARRAY ← method.body.get.instructions(i - 1);
                 ICONST_0 ← method.body.get.instructions(i - 2)
            ) yield i
            (classFile, method, calls)
        }
    }


    /**
     * ########  Code from FindBugs #########
     */
    /*

    // RM: setting of constant operand!
    else if (constantRefOperand instanceof ConstantCP) {
                        ConstantCP cp = (ConstantCP) constantRefOperand;
                        ConstantClass clazz = (ConstantClass) getConstantPool().getConstant(cp.getClassIndex());
                        classConstantOperand = getStringFromIndex(clazz.getNameIndex());
                        referencedClass = DescriptorFactory.createClassDescriptor(classConstantOperand);
                        referencedXClass = null;
                        ConstantNameAndType sig = (ConstantNameAndType) getConstantPool().getConstant(
                                cp.getNameAndTypeIndex());
                        nameConstantOperand = getStringFromIndex(sig.getNameIndex());
                        sigConstantOperand = getStringFromIndex(sig.getSignatureIndex());
                        refConstantOperand = null;
                    }

    public String getSigConstantOperand() {
        if (sigConstantOperand == NOT_AVAILABLE)
            throw new IllegalStateException("getSigConstantOperand called but value not available");
        return sigConstantOperand;
    }
    // RM: Report bug on heuristic based on seen invoke instructions
    public void sawOpcode(int seen) {
        if (DEBUG)
            System.out.println("State: " + state + "  Opcode: " + OPCODE_NAMES[seen]);

        switch (state) {
        case SEEN_NOTHING:
            if (seen == ICONST_0)
                state = SEEN_ICONST_0;
            break;

        case SEEN_ICONST_0:
            if (seen == ANEWARRAY) {
                state = SEEN_ANEWARRAY;
            } else
                state = SEEN_NOTHING;
            break;

        case SEEN_ANEWARRAY:
            if (((seen == INVOKEVIRTUAL) || (seen == INVOKEINTERFACE)) && (getNameConstantOperand().equals("toArray"))
                    && (getSigConstantOperand().equals("([Ljava/lang/Object;)[Ljava/lang/Object;"))) {
                try {
                    String clsName = getDottedClassConstantOperand();
                    JavaClass cls = Repository.lookupClass(clsName);
                    if (cls.implementationOf(collectionClass))
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "ITA_INEFFICIENT_TO_ARRAY", LOW_PRIORITY).addClassAndMethod(this), this);

                } catch (ClassNotFoundException cnfe) {
                    bugReporter.reportMissingClass(cnfe);
                }
            }
            state = SEEN_NOTHING;
            break;

        default:
            state = SEEN_NOTHING;
            break;
        }
    }
     */
}