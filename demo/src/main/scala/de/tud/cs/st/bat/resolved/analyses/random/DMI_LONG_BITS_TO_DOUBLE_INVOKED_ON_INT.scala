package de.tud.cs.st.bat.resolved.analyses.random

import de.tud.cs.st.bat.resolved._
import analyses.{BaseAnalyses, Project}
import de.tud.cs.st.bat.resolved.INVOKESTATIC

/**
 *
 * Author: Ralf Mitschke
 * Date: 06.08.12
 * Time: 15:53
 *
 */
object DMI_LONG_BITS_TO_DOUBLE_INVOKED_ON_INT
    extends (Project => Iterable[(ClassFile, Method, Int)])
{

    import BaseAnalyses.withIndex

    val doubleClass = ObjectType ("java/lang/Double")

    val longBitsToDoubleDescriptor = MethodDescriptor (List (LongType), DoubleType)

    def apply(project: Project) = {
        for (classFile ← project.classFiles;
             method ← classFile.methods if method.body.isDefined;
             Seq (
             (I2L, _),
             (INVOKESTATIC (`doubleClass`, "longBitsToDouble", `longBitsToDoubleDescriptor`), idx)
             ) ← withIndex (method.body.get.instructions).sliding (2)
        ) yield
        {
            (classFile, method, idx)
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