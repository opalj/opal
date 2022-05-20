/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import java.io.File
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.scalactic.Equality
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.LogContext
import org.opalj.log.StandardLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.bi.TestResources.{locateTestResources => locate}
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.MethodInvocationInstruction

import scala.collection.immutable.ArraySeq

/**
 * Tests the rewriting of lambda expressions/method references using Java 8's infrastructure. I.e.,
 * tests rewriting of [[org.opalj.br.instructions.INVOKEDYNAMIC]] instruction using
 * `LambdaMetafactory`s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class BasicLambdaExpressionsRewritingTest extends AnyFunSpec with Matchers {

    val InvokedMethod = ObjectType("annotations/target/InvokedMethod")

    val InvokedMethods = ObjectType("annotations/target/InvokedMethods")

    val lambda18TestResources: File = locate("lambdas-1.8-g-parameters-genericsignature.jar", "bi")

    private def testMethod(project: SomeProject, classFile: ClassFile, name: String): Unit = {
        info(s"Testing $name")
        var successFull = false
        for {
            method <- classFile.findMethod(name)
            body <- method.body
            factoryCall <- body.instructionIterator.collect { case i: INVOKESTATIC => i }
            if factoryCall.declaringClass.fqn.matches(InvokedynamicRewriting.LambdaNameRegEx)
        } {
            val annotations = method.runtimeVisibleAnnotations
            successFull = true
            implicit val MethodDeclarationEquality: Equality[Method] =
                (a: Method, b: Any) => b match {
                    case m: Method =>
                        a.compare(m) == 0 /* <=> same name and descriptor */ &&
                            a.visibilityModifier == m.visibilityModifier &&
                            a.isStatic == m.isStatic
                    case _ => false
                }

            if (annotations.exists(_.annotationType == InvokedMethods)) {
                val invokedTarget = for {
                    a <- annotations.iterator
                    if a.annotationType == InvokedMethods
                    evp <- a.elementValuePairs
                    ArrayValue(values) = evp.value
                    ev @ AnnotationValue(annotation) <- values
                    innerAnnotation = ArraySeq(annotation)
                    expectedTarget = getInvokedMethod(project, classFile, innerAnnotation)
                    actualTarget = getCallTarget(project, factoryCall, expectedTarget.get.name)
                    if MethodDeclarationEquality.areEqual(expectedTarget.get, actualTarget.get)
                } yield {
                    ev
                }
                /*
                val invokedTarget =
                    annotations.iterator
                        .filter(_.annotationType == InvokedMethods)
                        .flatMap[ElementValuePair](_.elementValuePairs)
                        .flatMap[ElementValue](_.value.asInstanceOf[ArrayValue].values)
                        .filter { invokeMethod =>
                            val innerAnnotation = ArraySeq(invokeMethod.asInstanceOf[AnnotationValue].annotation)
                            val expectedTarget = getInvokedMethod(project, classFile, innerAnnotation)
                            val actualTarget = getCallTarget(project, factoryCall, expectedTarget.get.name)
                            MethodDeclarationEquality.areEqual(expectedTarget.get, actualTarget.get)
                        }*/

                assert(
                    invokedTarget.nonEmpty,
                    s"failed to resolve $factoryCall in ${method.toJava}"
                )

            } else {
                val expectedTarget = getInvokedMethod(project, classFile, annotations)
                val actualTarget = getCallTarget(project, factoryCall, expectedTarget.get.name)

                withClue { s"failed to resolve ${method.toJava(factoryCall.toString)}" }(
                    actualTarget.get should ===(expectedTarget.get)
                )
            }

        }
        assert(successFull, s"couldn't find factory method call in $name")
    }

    private def getCallTarget(
        project:            SomeProject,
        factoryCall:        INVOKESTATIC,
        expectedTargetName: String
    ): Option[Method] = {
        val proxy = project.classFile(factoryCall.declaringClass).get
        val forwardingMethod = proxy.methods.find { m =>
            !m.isConstructor && m.name != factoryCall.name && !m.isBridge && !m.isSynthetic
        }.get
        val invocationInstructions = forwardingMethod.body.get.instructions.collect {
            case i: MethodInvocationInstruction => i
        }

        // Make sure to get the correct instruction, Integer::compareUnsigned has 3
        // MethodInvokations in the proxy class, 2x intValue for getting the value of the int and
        // compareUnsigned for the actual comparison. This method must return the last one, which
        // is compareUnsigned
        val invocationInstruction = invocationInstructions
            .find(_.name == expectedTargetName)
            .orElse(Some(invocationInstructions.head)).get

        // declaringClass must be an ObjectType, since lambdas cannot be created on
        // array types, nor do arrays have methods that could be referenced
        val declaringType = invocationInstruction.declaringClass.asObjectType
        val targetMethodName = invocationInstruction.name
        val targetMethodDescriptor: MethodDescriptor =
            if (targetMethodName == "<init>") {
                MethodDescriptor(invocationInstruction.methodDescriptor.parameterTypes, VoidType)
            } else {
                invocationInstruction.methodDescriptor
            }

        if (project.classHierarchy.isInterface(declaringType.asObjectType).isYes)
            project.resolveInterfaceMethodReference(
                declaringType.asObjectType,
                targetMethodName,
                targetMethodDescriptor
            )
        else
            project.resolveMethodReference(
                declaringType.asObjectType,
                targetMethodName,
                targetMethodDescriptor
            )
    }

    /**
     * This method retrieves '''the first''' "invoked method" that is specified by an
     * [[InvokedMethod]] annotation present in the given annotations.
     *
     * This assumes that in the test cases, there is never more than one [[InvokedMethod]]
     * annotation on a single test method.
     *
     * The `InvokedMethod` annotation might have to be revised for use with Java 8 lambdas,
     * or used multiple times (the first time referring to the actual generated
     * invokedynamic instruction, while all other times would refer to invocations of the
     * generated object's single method).
     */
    private def getInvokedMethod(
        project:     SomeProject,
        classFile:   ClassFile,
        annotations: Annotations
    ): Option[Method] = {
        val method = for {
            invokedMethod <- annotations.filter(_.annotationType == InvokedMethod)
            pairs = invokedMethod.elementValuePairs
            ElementValuePair("receiverType", StringValue(receiverType)) <- pairs
            ElementValuePair("name", StringValue(methodName)) <- pairs
            classFileOpt = project.classFile(ObjectType(receiverType))
        } yield {
            if (classFileOpt.isEmpty) {
                throw new IllegalStateException(s"the class file $receiverType cannot be found")
            }
            val parameterTypes = getParameterTypes(pairs)
            findMethodRecursive(project, classFileOpt.get, methodName, receiverType, parameterTypes)
        }

        if (method.isEmpty) {
            val message =
                annotations.
                    filter(_.annotationType == InvokedMethod).
                    mkString("\n\t", "\n\t", "\n")
            fail(
                s"the specified invoked method $message is not defined "+
                    classFile.methods.map(_.name).mkString("; defined methods = {", ",", "}")
            )
        }

        Some(method.head)
    }

    /**
     * Get the method definition recursively -> if the method isn't implemented in `classFile`, check if
     * the super class has an implementation.
     *
     * @param project The project where to look for the classfile
     * @param classFile The classfile to check the method
     * @param methodName The name of the method to find
     * @param receiverType The type of the receiver, which was defined in the fixture annotation
     * @return The `Method` with the name `methodName`
     */
    def findMethodRecursive(
        project:        SomeProject,
        classFile:      ClassFile,
        methodName:     String,
        receiverType:   String,
        parameterTypes: Option[FieldTypes]
    ): Method = {
        /**
         * Get the method definition recursively -> if the method isn't implemented in `classFile`, check if
         * the super class has an implementation.
         *
         * @param classFile The classfile to check the method
         * @return An Option of the `Method`
         */
        def findMethodRecursiveInner(classFile: ClassFile): Method = {
            var methodOpt = classFile.findMethod(methodName)
            if (parameterTypes.isDefined) {
                methodOpt = methodOpt.filter(_.parameterTypes == parameterTypes.get)
            }
            if (methodOpt.isEmpty) {
                classFile.superclassType match {
                    case Some(superType) => findMethodRecursiveInner(project.classFile(superType).get)
                    case None => throw new IllegalStateException(
                        s"$receiverType does not define $methodName"
                    )
                }
            } else {
                methodOpt.head
            }
        }
        findMethodRecursiveInner(classFile)
    }

    private def getParameterTypes(pairs: ElementValuePairs): Option[ArraySeq[FieldType]] = {
        pairs.find(_.name == "parameterTypes").map { p =>
            p.value.asInstanceOf[ArrayValue].values.map[FieldType] {
                case ClassValue(x: ArrayType)  => x
                case ClassValue(x: ObjectType) => x
                case ClassValue(x: BaseType)   => x
                case x: ElementValue           => x.valueType
            }
        }
    }

    def testProject(project: SomeProject): Unit = {
        def testAllMethodsWithInvokedMethodAnnotation(ot: String): Unit = {
            val classFile = project.classFile(ObjectType(ot)).get
            classFile
                .methods
                .filter(_.runtimeVisibleAnnotations.exists { a =>
                    a.annotationType == InvokedMethod || a.annotationType == InvokedMethods
                })
                .foreach(m => testMethod(project, classFile, m.name))
        }

        it("should resolve all references in Lambdas") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/Lambdas")
        }

        // --- Method References ---

        it("should resolve all references in DefaultMethod") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/DefaultMethod")
        }

        it("should resolve all references in InvokeSpecial") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/InvokeSpecial")
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/InvokeSpecial$Superclass")
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/InvokeSpecial$Subclass")
        }

        it("should resolve all references in MethodReferencePrimitives") {
            testAllMethodsWithInvokedMethodAnnotation(
                "lambdas/methodreferences/MethodReferencePrimitives"
            )
        }

        it("should resolve all references in MethodReferences") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/MethodReferences")
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/MethodReferences$Child")
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/MethodReferences$SomeInterface")
        }

        it("should resolve all references in ReceiverInheritance") {
            testAllMethodsWithInvokedMethodAnnotation(
                "lambdas/methodreferences/ReceiverInheritance"
            )
        }

        it("should resolve all references in SinkTest") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/SinkTest")
        }

        it("should resolve all references in StaticInheritance") {
            testAllMethodsWithInvokedMethodAnnotation("lambdas/methodreferences/StaticInheritance")
        }
    }

    describe("rewriting of lambda expressions") {
        implicit val logContext: LogContext = new StandardLogContext()
        OPALLogger.register(logContext, new ConsoleOPALLogger(ansiColored = true))

        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = InvokedynamicRewriting.InvokedynamicRewritingConfigKey
        val logLambdaConfigKey = InvokedynamicRewriting.LambdaExpressionsLogRewritingsConfigKey
        val logConcatConfigKey = InvokedynamicRewriting.StringConcatLogRewritingsConfigKey
        val testConfig = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.TRUE)).
            withValue(logLambdaConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE)).
            withValue(logConcatConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE))
        object Framework extends Java8FrameworkWithInvokedynamicSupportAndCaching(
            new BytecodeInstructionsCache
        ) {
            override def defaultConfig = testConfig
        }

        val project = Project(
            Framework.ClassFiles(lambda18TestResources),
            Java8LibraryFramework.ClassFiles(org.opalj.bytecode.JRELibraryFolder),
            libraryClassFilesAreInterfacesOnly = true,
            Iterable.empty,
            Project.defaultHandlerForInconsistentProjects,
            testConfig,
            logContext
        )
        testProject(project)

        OPALLogger.unregister(logContext)
    }
}
