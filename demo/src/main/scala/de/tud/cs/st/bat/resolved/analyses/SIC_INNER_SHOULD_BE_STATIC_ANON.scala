package de.tud.cs.st.bat.resolved.analyses

import java.util.regex.Pattern
import de.tud.cs.st.bat.resolved._

/**
 *
 * Author: Ralf Mitschke
 * Date: 06.08.12
 * Time: 14:47
 *
 */
object SIC_INNER_SHOULD_BE_STATIC_ANON
        extends Analysis
{

    val withinAnonymousClass = Pattern.compile("[$][0-9].*[$]")

    /**
     * A heuristic for determining whether an inner class is inside an anonymous inner class based on the class name
     */
    def isWithinAnonymousInnerClass(classFile: ClassFile): Boolean = {
        withinAnonymousClass.matcher(classFile.thisClass.className).find()
    }

    def lastIndexOfInnerClassEncoding(classFile: ClassFile): Int = {
        val name = classFile.thisClass.className
        math.max(name.lastIndexOf('$'), name.lastIndexOf('+'))
    }

    /**
     * A heuristic for determining inner classes by the encoding in the name
     */
    def isInnerClass(classFile: ClassFile): Boolean = {
        lastIndexOfInnerClassEncoding(classFile) >= 0
    }

    /**
     * A heuristic for determining anonymous inner classes by the encoding in the name
     */
    def isAnonymousInnerClass(classFile: ClassFile): Boolean = {
        val lastSpecialChar = lastIndexOfInnerClassEncoding(classFile)
        isInnerClass(classFile) &&
                Character.isDigit(classFile.thisClass.className.charAt(lastSpecialChar + 1))
    }


    /**
     * A heuristic for determining whether an inner class can be made static
     */
    def canConvertToStaticInnerClass(classFile: ClassFile): Boolean = {
        !isWithinAnonymousInnerClass(classFile)
    }

    /**
     * A heuristic for determining whether the field points to the enclosing instance
     */
    def isOuterThisField(field: Field): Boolean = {
        field.name.startsWith("this$") || field.name.startsWith("this+")
    }

    /**
     * A heuristic that determines whether the outer this field is read, by counting aload_1 instructions
     * The count must be greater than 1, because the variable will be read once for storing it
     * into the field reference for the outer this instance.
     */
    def constructorReadsOuterThisField(classFile: ClassFile): Boolean = {
        (for (method ← classFile.constructors if (method.name == "<init>") && method.body.isDefined;
              instr ← method.body.get.instructions if (instr.isInstanceOf[ALOAD_1.type])
        ) yield 1).sum > 1
    }

    def analyze(project: Project) = {
        val classFiles: Traversable[ClassFile] = project.classFiles
        val readFields = BaseAnalyses.readFields(classFiles).map(_._2)
        for (classFile ← classFiles
             if (isAnonymousInnerClass(classFile) &&
                     canConvertToStaticInnerClass(classFile)
                     );
             val declaringClass = classFile.thisClass;
             field@Field(_, name, fieldType, _) ← classFile.fields
             if (isOuterThisField(field) &&
                     !readFields.contains((declaringClass, name, fieldType)) &&
                     !constructorReadsOuterThisField(classFile)
                     )
        ) yield {
            ("SIC_INNER_SHOULD_BE_STATIC_ANON", classFile.thisClass.toJava)
        }
    }


    /**
     * ########  Code from FindBugs #########
     */
    /*
        // RM: fills data about classes that cannot be static
        if (getSuperclassName().indexOf("$") >= 0 || getSuperclassName().indexOf("+") >= 0
                || withinAnonymousClass.matcher(getDottedClassName()).find()) {
            data.innerClassCannotBeStatic.add(getDottedClassName());
            data.innerClassCannotBeStatic.add(getDottedSuperclassName());
        }

        // RM: finds out whether the super constructor uses the reference to outer.this
        if (getMethodName().equals("<init>") && count_aload_1 > 1
                && (getClassName().indexOf('$') >= 0 || getClassName().indexOf('+') >= 0)) {
            data.needsOuterObjectInConstructor.add(getDottedClassName());
            // System.out.println(betterClassName +
            // " needs outer object in constructor");
        }

        // RM: actual bug reporting
        if (!data.innerClassCannotBeStatic.contains(className)) {
            boolean easyChange = !data.needsOuterObjectInConstructor.contains(className);
            if (easyChange || !isAnonymousInnerClass) {

                // easyChange isAnonymousInnerClass
                // true false medium, SIC
                // true true low, SIC_ANON
                // false true not reported
                // false false low, SIC_THIS
                int priority = LOW_PRIORITY;
                if (easyChange && !isAnonymousInnerClass)
                    priority = NORMAL_PRIORITY;

                String bug = "SIC_INNER_SHOULD_BE_STATIC_ANON$";
                if (isAnonymousInnerClass)
                    bug = "SIC_INNER_SHOULD_BE_STATIC_ANON";
                else if (!easyChange)
                    bug = "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS";

                bugReporter.reportBug(new BugInstance(this, bug, priority).addClass(className));

            }
        }
     */
}