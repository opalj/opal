/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st
package bat.resolved
package analyses

import util.perf.{ Counting, PerformanceEvaluation }
import util.graphs.{ Node, toDot }
import reader.Java6Framework

/**
 * Implementation of some simple static analyses to demonstrate the flexibility
 * and power offered by Scala and BAT when analyzing class files.
 *
 * The implemented static analyses are insprired by Findbugs
 * (http://findbugs.sourceforge.net/bugDescriptions.html).
 *
 * @author Michael Eichberg
 */
class Main
object Main extends Main {

    private val CountingPerformanceEvaluator = new PerformanceEvaluation with Counting
    import CountingPerformanceEvaluator._

    private def printUsage: Unit = {
        println("Usage: java …ClassHierarchy <ZIP or JAR file containing class files>+")
        println("(c) 2011 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
    }

    def main(args: Array[String]) {

        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".zip") || arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead() || file.isDirectory()) {
                println("The file: " + file + " cannot be read.");
                printUsage
                sys.exit(1)
            }
        }

        analyze(args)
        sys.exit(0)
    }

    // The following code is meant to show how easy it is to write analyses;
    // it is not meant to demonstrate how to write such analyses in an effecient
    // manner.
    def analyze(zipFiles: Array[String]) {
        val classHierarchy = new ClassHierarchy {}

        var classFilesCount = 0
        val classFiles = time(t ⇒ println("Reading all class files took: " + nsToSecs(t))) {
            for (zipFile ← zipFiles; classFile ← Java6Framework.ClassFiles(zipFile)) yield {
                classFilesCount += 1
                classHierarchy.update(classFile)
                classFile
            }
        }
        val getClassFile = classFiles.map(cf ⇒ (cf.thisClass, cf)).toMap
        println("Number of class files: " + classFilesCount)

        // FINDBUGS: CI: Class is final but declares protected field (CI_CONFUSED_INHERITANCE) // http://code.google.com/p/findbugs/source/browse/branches/2.0_gui_rework/findbugs/src/java/edu/umd/cs/findbugs/detect/ConfusedInheritance.java
        val protectedFields = time(t ⇒ println("CI_CONFUSED_INHERITANCE: " + nsToSecs(t))) {
            for (
                classFile ← classFiles if classFile.isFinal;
                field ← classFile.fields if field.isProtected
            ) yield (classFile, field)
        }
        println("\tViolations: " + protectedFields.size)

        // FINDBUGS: UuF: Unused field (UUF_UNUSED_FIELD)
        var unusedFields: List[(ClassFile, Traversable[String])] = Nil
        time(t ⇒ println("UUF_UNUSED_FIELD: " + nsToSecs(t))) {
            for (classFile ← classFiles if !classFile.isInterfaceDeclaration) {
                val declaringClass = classFile.thisClass
                var privateFields = (for (field ← classFile.fields if field.isPrivate) yield field.name).toSet
                for (
                    method ← classFile.methods if method.body.isDefined;
                    instruction ← method.body.get.code
                ) {
                    instruction match {
                        case GETFIELD(`declaringClass`, name, _)  ⇒ privateFields -= name
                        case GETSTATIC(`declaringClass`, name, _) ⇒ privateFields -= name
                        case _                                    ⇒
                    }
                }
                if (privateFields.size > 0)
                    unusedFields = (classFile, privateFields) :: unusedFields
            }
        }
        println("\tViolations: " + unusedFields.size)

        // FINDBUGS: Dm: Explicit garbage collection; extremely dubious except in benchmarking code (DM_GC)
        var garbageCollectingMethods: List[(ClassFile, Method, Instruction)] = Nil
        time(t ⇒ println("DM_GC: " + nsToSecs(t))) {
            for (
                classFile ← classFiles;
                method ← classFile.methods if method.body.isDefined;
                instruction ← method.body.get.code
            ) {
                instruction match {
                    case INVOKESTATIC(ObjectType("java/lang/System"), "gc", MethodDescriptor(Seq(), VoidType)) |
                        INVOKEVIRTUAL(ObjectType("java/lang/Runtime"), "gc", MethodDescriptor(Seq(), VoidType)) ⇒
                        garbageCollectingMethods = (classFile, method, instruction) :: garbageCollectingMethods
                    case _ ⇒
                }
            }
        }
        println("\tViolations: " + garbageCollectingMethods.size)

        // FINDBUGS: FI: Finalizer should be protected, not public (FI_PUBLIC_SHOULD_BE_PROTECTED)
        var classesWithPublicFinalizeMethods = time(t ⇒ println("FI_PUBLIC_SHOULD_BE_PROTECTED: " + nsToSecs(t))) {
            for (
                classFile ← classFiles if classFile.methods.exists(method ⇒ method.name == "finalize" && method.isPublic && method.descriptor.returnType == VoidType && method.descriptor.parameterTypes.size == 0)
            ) yield classFile
        }
        println("\tViolations: " + classesWithPublicFinalizeMethods.length)

        // FINDBUGS: Se: Class is Serializable but its superclass doesn't define a void constructor (SE_NO_SUITABLE_CONSTRUCTOR)
        val serializableClasses = classHierarchy.subclasses(ObjectType("java/io/Serializable"))
        val classesWithoutDefaultConstructor = time(t ⇒ println("SE_NO_SUITABLE_CONSTRUCTOR: " + nsToSecs(t))) {
            for (
                superclass ← classHierarchy.superclasses(serializableClasses) if getClassFile.isDefinedAt(superclass) && // the class file of some supertypes (defined in libraries, which we do not analyze) may not be available
                    {
                        val superClassFile = getClassFile(superclass)
                        !superClassFile.isInterfaceDeclaration &&
                            !superClassFile.constructors.exists(_.descriptor.parameterTypes.length == 0)
                    }
            ) yield superclass // there can be at most one method
        }
        println("\tViolations: " + classesWithoutDefaultConstructor.size)

        // FINDBUGS: (IMSE_DONT_CATCH_IMSE) http://code.google.com/p/findbugs/source/browse/branches/2.0_gui_rework/findbugs/src/java/edu/umd/cs/findbugs/detect/DontCatchIllegalMonitorStateException.java
        val IllegalMonitorStateExceptionType = ObjectType("java/lang/IllegalMonitorStateException")
        val catchesIllegalMonitorStateException = time(t ⇒ println("IMSE_DONT_CATCH_IMSE: " + nsToSecs(t))) {
            for (
                classFile ← classFiles if classFile.isClassDeclaration;
                method ← classFile.methods if method.body.isDefined;
                exceptionHandler ← method.body.get.exceptionTable if exceptionHandler.catchType == IllegalMonitorStateExceptionType
            ) yield (classFile, method)
        }
        println("\tViolations: " + catchesIllegalMonitorStateException.size)
    }
}