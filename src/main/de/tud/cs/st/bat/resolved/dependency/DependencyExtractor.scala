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
package de.tud.cs.st.bat.resolved
package dependency

import DependencyType._
import de.tud.cs.st.bat.AccessFlagsContexts._
import de.tud.cs.st.bat.ACC_STATIC
import de.tud.cs.st.bat.AccessFlagsIterator

/**
 * Extracts the dependencies between
 * classes, interfaces, fields and methods.
 *
 * Unique IDs are retrieved from and
 * dependencies are passed to the 'getID' and 'addDependency' methods provided by
 * the given DependencyBuilder.
 *
 *
 * ==Implementation Note==
 * Only attributes defined by Java 5 specification are considered in this implementation.
 *
 *
 * @author Thomas Schlosser
 */
class DependencyExtractor(val builder: DependencyBuilder) extends InstructionDependencyExtractor {

    import builder._

    /**
     * Processes the given class file and all fields and methods that are defined in it.
     * I.e. it extracts all source code dependencies that start from the given
     * class file, its fields or methods, respectively.
     *
     * @param classFile The class file whose dependencies should be extracted.
     */
    def process(classFile: ClassFile) {
        val thisClassID = getID(classFile)
        val ClassFile(_, _, _, thisClass, superClass, interfaces, fields, methods, attributes) = classFile

        classFile.superClass foreach { superClass ⇒ addDependency(thisClassID, getID(superClass), EXTENDS) }

        interfaces foreach { i ⇒ addDependency(thisClassID, getID(i), IMPLEMENTS) }

        // As defined by the Java 5 specification, a class declaration can contain the following attributes:
        // InnerClasses, EnclosingMethod, Synthetic, SourceFile, Signature, Deprecated,
        // SourceDebugExtension, RuntimeVisibleAnnotations, and RuntimeInvisibleAnnotations
        attributes foreach {
            case AnnotationsAttribute(_, annotations) ⇒ // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, thisClassID) }
            case ema @ EnclosingMethodAttribute(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor) ⇒ {
                // add a dependency from this class to its enclosing method/class
                addDependency(
                    thisClassID,
                    {
                        // Check whether the enclosing method attribute refers to
                        // an enclosing method or an enclosing class.
                        if (isEnclosedByMethod(ema))
                            getID(enclosingClazz, enclosingMethodName, enclosingMethodDescriptor)
                        else
                            getID(enclosingClazz)
                    },
                    IS_INNER_CLASS_OF)
            }
            case InnerClassesAttribute(innerClasses) ⇒
                for (
                    // Check whether the outer class of the inner class attribute
                    // is equals to the currently processed class. If this is the case,
                    // a dependency from inner class to this class will be added.
                    InnerClassesEntry(innerClass, outerClass, _, _) ← innerClasses if outerClass != null if outerClass == thisClass
                ) {
                    addDependency(getID(innerClass), thisClassID, IS_INNER_CLASS_OF)
                }
            case signature: Signature ⇒ processSignature(signature, thisClassID)
            case _                    ⇒ // Synthetic, SourceFile, Deprecated, and SourceDebugExtension do not create dependencies
        }

        // process fields
        fields foreach { process(_, thisClass, thisClassID) }

        // process methods
        methods foreach { process(_, thisClass, thisClassID) }
    }

    /**
     * Processes the given field, i.e. extracts all dependencies that start
     * from this field.
     *
     * @param field The field whose dependencies should be extracted.
     * @param thisClass The type of this field's declaring class.
     * @param thisClassID The ID of the field's declaring class.
     */
    private def process(field: Field, thisClass: ObjectType, thisClassID: Int) {
        val fieldID = getID(thisClass, field)
        val Field(accessFlags, _, fieldType, attributes) = field

        addDependency(fieldID, thisClassID, if (field.isStatic) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
        // add a dependency from the field to its type
        addDependency(fieldID, getID(fieldType), IS_OF_TYPE)

        // The Java 5 specification defines the following attributes:
        // ConstantValue, Synthetic, Signature, Deprecated, RuntimeVisibleAnnotations, and
        // RuntimeInvisibleAnnotations
        attributes foreach {
            case AnnotationsAttribute(_, annotations) ⇒ // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, fieldID) }
            case cv: ConstantValue[_] ⇒
                addDependency(fieldID, getID(cv.valueType), USES_CONSTANT_VALUE_OF_TYPE)
            case signature: Signature ⇒
                processSignature(signature, fieldID)
            case _ ⇒ // Synthetic and Deprecated do not introduce new dependencies
        }
    }

    /**
     * Processes the given method, i.e. extracts all dependencies that start
     * from this method.
     *
     * @param method The method whose dependencies should be extracted.
     * @param thisClass The method's declaring class.
     * @param thisClassID The ID of the method's declaring class.
     */
    private def process(method: Method, thisClass: ObjectType, thisClassID: Int) {
        val methodID = getID(thisClass, method)
        val Method(accessFlags, _, MethodDescriptor(parameterTypes, returnType), attributes) = method

        addDependency(methodID, thisClassID, if (method.isStatic) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)

        addDependency(methodID, getID(returnType), RETURNS)
        parameterTypes foreach { pt ⇒ addDependency(methodID, getID(pt), HAS_PARAMETER_OF_TYPE) }

        // The Java 5 specification defines the following attributes:
        // Code, Exceptions, Synthetic, Signature, Deprecated, RuntimeVisibleAnnotations,
        // RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations,
        // RuntimeInvisibleParameterAnnotations, and AnnotationDefault
        attributes foreach {
            case AnnotationsAttribute(_, annotations) ⇒ // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, methodID) }
            case ParameterAnnotationsAttribute(_, parameterAnnotations) ⇒ // handles RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations
                parameterAnnotations foreach { _ foreach { process(_, methodID, PARAMETER_ANNOTATED_WITH) } }
            case ExceptionsAttribute(exceptionTable) ⇒
                exceptionTable foreach { e ⇒ addDependency(methodID, getID(e), THROWS) }
            case elementValue: ElementValue ⇒ // ElementValues encode annotation default attributes
                processElementValue(elementValue, methodID)
            case CodeAttribute(_, _, code, exceptionTable, attributes) ⇒
                // Process code instructions by calling the process method which is defined in
                // the generated InstructionDependencyExtractor super class.
                process(methodID, code)
                // add dependencies from the method to all throwables that are used in catch statements
                for (
                    ExceptionTableEntry(_, _, _, catchType) ← exceptionTable if !isFinallyBlock(catchType)
                ) {
                    addDependency(methodID, getID(catchType), CATCHES)
                }
                // The Java 5 specification defines the following attributes:
                // LineNumberTable, LocalVariableTable, and LocalVariableTypeTable)
                attributes foreach {
                    case LocalVariableTableAttribute(localVariableTable) ⇒
                        localVariableTable foreach {
                            entry ⇒ addDependency(methodID, getID(entry.fieldType), HAS_LOCAL_VARIABLE_OF_TYPE)
                        }
                    case LocalVariableTypeTableAttribute(localVariableTypeTable) ⇒
                        localVariableTypeTable foreach {
                            entry ⇒ processSignature(entry.signature, methodID)
                        }
                    case _ ⇒ // The LineNumberTable does not define relevant dependencies
                }
            case sa: Signature ⇒
                processSignature(sa, methodID)
            case _ ⇒ // The attributes Synthetic and Deprecated do not introduce further dependencies
        }
    }

    /**
     * Processes the given signature.
     * Processing a signature means extracting all references to types
     * that are used in the type parameters that occur in the signature.
     * After that extraction, dependencies from the given source to the
     * extracted types are added.
     *
     * NOTE: As described above, this method only consideres types that
     * occur in type parameters. All other types are extracted in other
     * methods.
     *
     * @param signature The signature whose type parameters should be analyzed.
     * @param srcID The ID of the source, i.e. the ID of the element the signature is from.
     * @param isInTypeParameters Signals whether the current signature (part)
     *                           is already a part of a type parameter.
     */
    private def processSignature(signature: SignatureElement, srcID: Int, isInTypeParameters: Boolean = false) {
        /**
         * Processes the given option of a formal type parameter list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e. dependencies to them will be
         * added.<br/>
         *
         * Calls the outer <code>processSignature</code> method for each
         * defined class and interface bound. The <code>isInTypeParameters</code>
         * parameter is set to <code>true</code>.
         *
         * @param formalTypeParameters The option of a formal type parameter list that should be processed.
         */
        def processFormalTypeParameters(formalTypeParameters: Option[List[FormalTypeParameter]]) {
            formalTypeParameters match {
                case Some(list) ⇒
                    for (FormalTypeParameter(_, classBound, interfaceBound) ← list) {
                        if (classBound.isDefined)
                            processSignature(classBound.get, srcID, true)
                        if (interfaceBound.isDefined)
                            processSignature(interfaceBound.get, srcID, true)
                    }
                case None ⇒ // Nothing to do if there is no class or interface bound.
            }
        }

        /**
         * Processes the given <code>SimpleClassTypeSignature</code> in the way
         * that its type arguments will be processed by the <code>processTypeArguments</code>
         * method.
         *
         * @param simpleClassTypeSignatures The simple class type signature that should be processed.
         */
        def processSimpleClassTypeSignature(simpleClassTypeSignatures: SimpleClassTypeSignature) {
            val SimpleClassTypeSignature(_, typeArguments) = simpleClassTypeSignatures
            processTypeArguments(typeArguments)
        }

        /**
         * Processes the given option of a type argument list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e. dependencies to them will be
         * added.<br/>
         *
         * Calls the outer <code>processSignature</code> method for each
         * signature that is part of a proper type argument.The
         * <code>isInTypeParameters</code> parameter is set to <code>true</code>.
         *
         * @param typeArguments The option of a type argument list that should be processed.
         */
        def processTypeArguments(typeArguments: Option[List[TypeArgument]]) {
            typeArguments match {
                case Some(args) ⇒
                    args foreach {
                        case ProperTypeArgument(_, fieldTypeSignature) ⇒
                            processSignature(fieldTypeSignature, srcID, true)
                        case Wildcard ⇒ // Wildcards refer to no type, hence there is nothing to do in this case.
                    }
                case None ⇒
            }
        }

        // process different signature types in a different way
        signature match {
            case ClassSignature(formalTypeParameters, superClassSignature, superInterfacesSignature) ⇒
                // process each part of the class's signature by calling the adequate utility method
                processFormalTypeParameters(formalTypeParameters)
                processSignature(superClassSignature, srcID)
                superInterfacesSignature foreach { cts ⇒ processSignature(cts, srcID) }
            case MethodTypeSignature(formalTypeParameters, parametersTypeSignatures, returnTypeSignature, throwsSignature) ⇒
                // process each part of the method's signature by calling the adequate utility method
                processFormalTypeParameters(formalTypeParameters)
                parametersTypeSignatures foreach { pts ⇒ processSignature(pts, srcID) }
                processSignature(returnTypeSignature, srcID)
                throwsSignature foreach { ts ⇒ processSignature(ts, srcID) }
            case ArrayTypeSignature(typeSignature) ⇒
                // process array's type signature by calling this method again and passing
                // the array's type signature as parameter
                processSignature(typeSignature, srcID)
            case cts: ClassTypeSignature ⇒
                // process the class type's signature by calling the adequate utility method
                // If the class is used in a type parameter, a dependency between the given
                // source and the class is added.
                if (isInTypeParameters)
                    addDependency(srcID, getID(cts.objectType), USES_TYPE_IN_TYPE_PARAMETERS)
                processSimpleClassTypeSignature(cts.simpleClassTypeSignature)
            case ProperTypeArgument(_, fieldTypeSignature) ⇒
                // process proper type argument by calling this method again and passing
                // the proper type's field type signature as parameter
                processSignature(fieldTypeSignature, srcID)
            case BooleanType | ByteType | IntegerType | LongType | CharType | ShortType | FloatType | DoubleType | VoidType ⇒
                // add a dependency to base or void type if the type is used in a type parameter
                if (isInTypeParameters)
                    addDependency(srcID, getID(signature.asInstanceOf[ReturnType]), USES_TYPE_IN_TYPE_PARAMETERS)
            case _ ⇒ // TypeVariableSignature and Wildcard refer to no concrete type, hence there is nothing to do in this case.
        }
    }

    /**
     * Extracts dependencies to elements contained by the given element value.
     *
     * @param elementValue The element value that should be analyzed regarding dependencies.
     * @param srcID The ID of the (source) element all discovered dependencies should start from.
     */
    private def processElementValue(elementValue: ElementValue, srcID: Int) {
        elementValue match {
            case ClassValue(returnType) ⇒
                addDependency(srcID, getID(returnType), USES_DEFAULT_CLASS_VALUE_TYPE)
            case EnumValue(enumType, constName) ⇒
                addDependency(srcID, getID(enumType), USES_DEFAULT_ENUM_VALUE_TYPE)
                addDependency(srcID, getID(enumType, constName), USES_ENUM_VALUE)
            case ArrayValue(values) ⇒
                values foreach { processElementValue(_, srcID) }
            case AnnotationValue(annotation) ⇒
                process(annotation, srcID, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
            case _ ⇒ // Remaining ElementValue types (ByteValue, CharValue, DoubleValue,
            // FloatValue, IntValue, LongValue, ShortValue, BooleanValue, StringValue)
            // do not refer to a object type, hence there is nothing to do in this case.
        }
    }

    /**
     * Extracts dependencies to elements contained by the given annotation.
     *
     * @param annotation The annotation that should be analyzed regarding dependencies.
     * @param srcID The ID of the (source) element all discovered dependencies should start from.
     * @param dependencyType The type that should be used to create new dependencies.
     *                       Default value is: ANNOTATED_WITH
     */
    private def process(annotation: Annotation, srcID: Int, dependencyType: DependencyType = ANNOTATED_WITH) {
        val Annotation(annotationType, elementValuePairs) = annotation
        // add dependency from source to the discovered annotation type
        addDependency(srcID, getID(annotationType), dependencyType)
        // extract dependencies for each element value pair that is used in the given annotation
        elementValuePairs foreach { evp ⇒ processElementValue(evp.elementValue, srcID) }
    }

    /**
     * Determines whether the given catchType refers to a finally block.
     *
     * @param catchType The type that should be analyzed.
     * @return <code>true</code> if the catchType is NULL.
     *         Otherwise, <code>false</code> is returned.
     */
    private def isFinallyBlock(catchType: ObjectType): Boolean =
        catchType == null

    /**
     * Determines whether the given EnclosingMethod attribute
     * contains method information.
     *
     * @param ema The attribute that should be analyzed.
     * @return <code>true</code> if method name and descriptor is not NULL.
     *         Otherwise, <code>false</code> is returned.
     */
    private def isEnclosedByMethod(ema: EnclosingMethodAttribute): Boolean =
        ema.name != null && ema.descriptor != null // otherwise the inner class is assigned to a field
}
