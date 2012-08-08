package de.tud.cs.st.bat.resolved.analyses

import de.tud.cs.st.bat.resolved._

/**
 *
 * Author: Ralf Mitschke
 * Date: 06.08.12
 * Time: 15:53
 *
 */
object DMI_LONG_BITS_TO_DOUBLE_INVOKED_ON_INT
{

    val doubleType = ObjectType("java/lang/Double")

    val longBitsToDoubleDescriptor = MethodDescriptor(List(LongType), doubleType)


    def analyze(classFiles: Traversable[ClassFile], classHierarchy: ClassHierarchy) = {
        for (classFile ← classFiles;
             method ← classFile.methods if method.body.isDefined
        ) yield {
            val calls =
                for (i ← 1 to method.body.get.instructions.length - 1;
                     INVOKESTATIC(`doubleType`, "longBitsToDouble", `longBitsToDoubleDescriptor`) ← method.body.get
                             .instructions(i);
                     I2L ← method.body.get.instructions(i - 1)
                ) yield i
            (classFile, method, calls)
        }
    }

    /*
        if (prevOpcode == I2L && seen == INVOKESTATIC && getClassConstantOperand().equals("java/lang/Double")
                && getNameConstantOperand().equals("longBitsToDouble")) {
            accumulator.accumulateBug(new BugInstance(this, "DMI_LONG_BITS_TO_DOUBLE_INVOKED_ON_INT", HIGH_PRIORITY)
                    .addClassAndMethod(this).addCalledMethod(this), this);
        }
     */

}