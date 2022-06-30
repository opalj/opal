/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.xml.Node
import scala.xml.Text
import scala.collection.mutable.Builder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.BitArraySet
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

import scala.collection.immutable.ArraySeq

/**
 * In this representation of Java bytecode references to a Java class file's constant
 * pool and to attributes are replaced by direct references to the corresponding constant
 * pool entries. This facilitates developing analyses and fosters comprehension.
 *
 * Based on the fact that indirect
 * references to constant pool entries are resolved and replaced by direct references this
 * representation is called the resolved representation.
 *
 * This representation of Java bytecode is considered as OPAL's standard representation
 * for writing Scala based analyses. This representation is engineered such
 * that it facilitates writing analyses that use pattern matching.
 *
 * @author Michael Eichberg
 */
package object br {

    final val FrameworkName = "OPAL Bytecode Representation"

    {
        implicit val logContext: LogContext = GlobalLogContext
        try {
            assert(false) // <= test whether assertions are turned on or off...
            info(FrameworkName, "Production Build")
        } catch {
            case _: AssertionError => info(FrameworkName, "Development Build with Assertions")
        }
    }

    // We want to make sure that the class loader is used which potentially can
    // find the config files; the libraries (e.g., Typesafe Config) may have
    // been loaded using the parent class loader and, hence, may not be able to
    // find the config files at all.
    val BaseConfig: Config = ConfigFactory.load(this.getClass.getClassLoader)

    final val ConfigKeyPrefix = "org.opalj.br."

    type LiveVariables = Array[BitArraySet]

    type Attributes = ArraySeq[Attribute]
    val Attributes: ArraySeq.type = ArraySeq
    final def NoAttributes: Attributes = ArraySeq.empty

    type ElementValues = ArraySeq[ElementValue]
    type ElementValuePairs = ArraySeq[ElementValuePair]
    val ElementValuePairs: ArraySeq.type = ArraySeq
    final def NoElementValuePairs: ElementValuePairs = ArraySeq.empty

    type Annotations = ArraySeq[Annotation]
    def NoAnnotations: ArraySeq[Annotation] = ArraySeq.empty
    type TypeAnnotations = ArraySeq[TypeAnnotation]
    final def NoTypeAnnotations: ArraySeq[TypeAnnotation] = ArraySeq.empty

    type InnerClasses = ArraySeq[InnerClass]

    type Interfaces = ArraySeq[ObjectType]
    final def NoInterfaces: Interfaces = ArraySeq.empty

    type Methods = ArraySeq[Method]
    val Methods: ArraySeq.type = ArraySeq
    final def NoMethods: Methods = ArraySeq.empty
    type MethodTemplates = ArraySeq[MethodTemplate]
    final def NoMethodTemplates: MethodTemplates = ArraySeq.empty

    type Exceptions = ArraySeq[ObjectType]
    type ExceptionHandlers = ArraySeq[ExceptionHandler]
    final def NoExceptionHandlers: ExceptionHandlers = ArraySeq.empty

    type LineNumbers = ArraySeq[LineNumber]
    type LocalVariableTypes = ArraySeq[LocalVariableType]
    type LocalVariables = ArraySeq[LocalVariable]
    type BootstrapMethods = ArraySeq[BootstrapMethod]
    type BootstrapArguments = ArraySeq[BootstrapArgument]
    type ParameterAnnotations = ArraySeq[Annotations]
    final def NoParameterAnnotations: ParameterAnnotations = ArraySeq.empty
    type StackMapFrames = ArraySeq[StackMapFrame]
    type VerificationTypeInfoLocals = ArraySeq[VerificationTypeInfo]
    type VerificationTypeInfoStack = ArraySeq[VerificationTypeInfo]
    type MethodParameters = ArraySeq[MethodParameter]

    type Fields = ArraySeq[Field]
    final def NoFields: Fields = ArraySeq.empty

    type FieldTemplates = ArraySeq[FieldTemplate]
    final def NoFieldTemplates: FieldTemplates = ArraySeq.empty

    type Instructions = Array[instructions.Instruction]

    type MethodDescriptors = ArraySeq[MethodDescriptor]

    type InstructionLabels = ArraySeq[instructions.InstructionLabel]

    type ObjectTypes = ArraySeq[ObjectType]
    val ObjectTypes: ArraySeq.type = ArraySeq

    type FieldTypes = ArraySeq[FieldType]
    val FieldTypes: ArraySeq.type = ArraySeq
    final def NoFieldTypes: FieldTypes = ArraySeq.empty
    final def newFieldTypesBuilder(): Builder[FieldType, ArraySeq[FieldType]] = {
        ArraySeq.newBuilder[FieldType]
    }

    type Packages = ArraySeq[String]

    type Classes = ArraySeq[ObjectType]

    type RecordComponents = ArraySeq[RecordComponent]

    final type SourceElementID = Int

    final type Opcode = Int

    /**
     * A program counter identifies an instruction in a code array.
     *
     * A program counter is a value in the range `[0/*UShort.min*/, 65535/*UShort.max*/]`.
     *
     * @note This type alias serves comprehension purposes.
     */
    final type PC = bytecode.PC

    /**
     * A collection of program counters using an IntArraySet as its backing collection.
     *
     * Using PCs is in particular well suited for small(er) collections.
     *
     * @note This type alias serves comprehension purposes.
     */
    final type PCs = IntTrieSet

    final val NoPCs: IntTrieSet = IntTrieSet.empty

    /**
     * Converts a given list of annotations into a Java-like representation.
     */
    def annotationsToJava(
        annotations: Annotations,
        before:      String      = "",
        after:       String      = ""
    ): String = {

        val annotationToJava: Annotation => String = { annotation: Annotation =>
            val s = annotation.toJava
            if (s.length() > 50 && annotation.elementValuePairs.nonEmpty)
                annotation.annotationType.toJava+"(...)"
            else
                s
        }

        if (annotations.nonEmpty) {
            before + annotations.map(annotationToJava).mkString(" ") + after
        } else {
            ""
        }
    }

    /**
     * An upper type bound represents the available type information about a
     * reference value. It is always "just" an upper bound for a concrete type;
     * i.e., we know that the runtime type has to be a subtype (reflexive) of the
     * type identified by the upper bound.
     * Furthermore, an upper bound can identify multiple '''independent''' types. E.g.,
     * a type bound for array objects could be: `java.io.Serializable` and
     * `java.lang.Cloneable`. Here, independent means that no two types of the bound
     * are in a subtype relationship. Hence, an upper bound is always a special set where
     * the values are not equal and are not in an inheritance relation. However,
     * identifying independent types is a class hierarchy's responsibility.
     *
     * In general, an upper bound identifies a single class type and a set of independent
     * interface types that are known to be implemented by the current object. '''Even if
     * the type contains a class type''' it may just be a super class of the concrete type
     * and, hence, just represent an abstraction.
     */
    type UpperTypeBound = UIDSet[ReferenceType]

    /**
     * Creates an (X)HTML5 representation of the given Java type declaration.
     */
    def typeToXHTML(t: Type, abbreviateType: Boolean = true): Node = {
        t match {
            case ot: ObjectType =>
                if (abbreviateType)
                    <abbr class="type object_type" title={ ot.toJava }>
                        { ot.simpleName }
                    </abbr>
                else
                    <span class="type object_type">{ ot.toJava }</span>
            case at: ArrayType =>
                <span class="type array_type">
                    { typeToXHTML(at.elementType, abbreviateType) }{ "[]" * at.dimensions }
                </span>
            case bt: BaseType =>
                <span class="type base_type">{ bt.toJava }</span>
            case VoidType =>
                <span class="type void_type">void</span>
            case CTIntType =>
                <span class="type base_type">{ "<Computational Type Integer>" }</span>
        }
    }

    def classAccessFlagsToXHTML(accessFlags: Int): Node = {
        <span class="access_flags">{ AccessFlags.toString(accessFlags, AccessFlagsContexts.CLASS) }</span>
    }

    def classAccessFlagsToString(accessFlags: Int): String = {
        AccessFlags.toString(accessFlags, AccessFlagsContexts.CLASS)
    }

    def typeToXHTML(accessFlags: Int, t: Type, abbreviateTypes: Boolean): Node = {

        val signature = typeToXHTML(t, abbreviateTypes)

        <span class="type_signature_with_access_flags">
            { classAccessFlagsToXHTML(accessFlags) }
            { signature }
        </span>
    }

    /**
     * Creates an (X)HTML5 representation that resembles Java source code method signature.
     */
    def methodToXHTML(
        name:            String,
        descriptor:      MethodDescriptor,
        abbreviateTypes: Boolean          = true
    ): Node = {

        val parameterTypes =
            if (descriptor.parametersCount == 0)
                List(Text(""))
            else {
                val parameterTypes = descriptor.parameterTypes.map(typeToXHTML(_, abbreviateTypes))
                parameterTypes.tail.foldLeft(List(parameterTypes.head)) { (c, r) =>
                    r :: Text(", ") :: c
                }.reverse
            }

        <span class="method_signature">
            <span class="method_return_type">{ typeToXHTML(descriptor.returnType, abbreviateTypes) }</span>
            <span class="method_name">{ name }</span>
            <span class="method_parameters">({ parameterTypes })</span>
        </span>
    }

    def methodToXHTML(
        accessFlags:     Int,
        name:            String,
        descriptor:      MethodDescriptor,
        abbreviateTypes: Boolean
    ): Node = {

        val signature = methodToXHTML(name, descriptor, abbreviateTypes)

        <span class="method_signature_with_access_flags">
            <span class="access_flags">{ methodAccessFlagsToString(accessFlags) }</span>
            { signature }
        </span>
    }

    def methodAccessFlagsToString(accessFlags: Int): String = {
        AccessFlags.toString(accessFlags, AccessFlagsContexts.METHOD)
    }

    /**
     * Calculates the parameter index associated with a method's local variable register index.
     * The index of the first parameter is 0. If the method is not static the *this* reference
     * stored in register index `0` has the parameter index `-1`.
     *
     * @param  isStatic `true` if method is static and, hence, has no implicit parameter for `this`.
     * @return The parameter index for the specified register.
     */
    def registerIndexToParameterIndex(
        isStatic:      Boolean,
        descriptor:    MethodDescriptor,
        registerIndex: Int
    ): Int = {

        var parameterIndex = 0
        val parameterTypes = descriptor.parameterTypes
        var currentIndex = 0
        while (currentIndex < registerIndex) {
            currentIndex += parameterTypes(parameterIndex).computationalType.operandSize
            parameterIndex += 1
        }
        if (isStatic) parameterIndex else parameterIndex - 1
    }

}
