/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat
package resolved
package dependency

import instructions._
import DependencyType._

/**
  * Traverses a source element and identifies all dependencies between the element (class,
  * field or method declaration) that is traversed and any element the traversed element
  * depends on.
  *
  * By default, self dependencies will be reported (e.g. a method that calls itself). If
  * necessary or undesired, self dependencies can easily be filtered by a
  * [[de.tud.cs.st.bat.resolved.dependency.DependencyProcessor]]'s processDependency
  * method (which is called by the dependency extractor, but whose implementation needs
  * to be provided.)
  *
  * ==Usage==
  * The demo class `de.tud.cs.st.bat.resolved.dependency.DependencyMatrix` (see the
  * implementation) provides an example how to use the dependency extractor.
  *
  * To assign ids to source elements the dependency extractor relies on source element
  * ids as provided by implementations of `de.tud.cs.st.bat.resolved.SourceElementIDs`.
  * After getting the ids of the depending source elements, a
  * [[de.tud.cs.st.bat.resolved.dependency.DependencyProcessor]]'s
  * `processDependency` method is called with the ids and the type of the dependency as a
  * parameter.
  *
  * @author Thomas Schlosser
  * @author Michael Eichberg
  */
abstract class DependencyExtractor(
    val sourceElementIDs: SourceElementIDs)
        extends DependencyProcessor
        with SourceElementsVisitor[Unit] {

    import sourceElementIDs._

    /**
      * Processes the given class file and all fields and methods that are defined in it.
      * I.e. it extracts all source code dependencies that start from the given
      * class file, its fields or methods, respectively.
      *
      * @param classFile The class file whose dependencies should be extracted.
      */
    def process(classFile: ClassFile) {
        visit(classFile)
        val thisClassID = sourceElementID(classFile)

        val thisType = classFile.thisType
        val superclass = classFile.superclassType
        val interfaces = classFile.interfaceTypes
        val fields = classFile.fields
        val methods = classFile.methods
        val attributes = classFile.attributes

        superclass foreach { processDependency(thisClassID, _, EXTENDS) }
        interfaces foreach { processDependency(thisClassID, _, IMPLEMENTS) }
        fields foreach { process(_, classFile, thisClassID) }
        methods foreach { process(_, classFile, thisClassID) }

        // As defined by the Java 5 specification, a class declaration can contain the 
        // following attributes:
        // InnerClasses, EnclosingMethod, Synthetic, SourceFile, Signature, Deprecated,
        // SourceDebugExtension, RuntimeVisibleAnnotations and 
        // RuntimeInvisibleAnnotations
        attributes foreach {
            case AnnotationTable(_, annotations) ⇒
                // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, thisClassID) }
            case EnclosingMethod(enclosingClass, name, descriptor) ⇒ {
                // Check whether the enclosing method attribute refers to an enclosing 
                // method or an enclosing class.
                if (name != null && descriptor != null)
                    processDependency(
                        thisClassID,
                        enclosingClass,
                        name,
                        descriptor,
                        IS_INNER_CLASS_OF)
                else
                    processDependency(thisClassID, enclosingClass, IS_INNER_CLASS_OF)
            }
            case InnerClassTable(innerClasses) ⇒
                for {
                    // Check whether the outer class of the inner class attribute
                    // is equal to the currently processed class. If this is the case,
                    // a dependency from inner class to this class will be added.
                    InnerClass(innerClass, outerClass, _, _) ← innerClasses
                    if outerClass.exists(_ == thisType)
                } {
                    processDependency(innerClass, thisClassID, IS_INNER_CLASS_OF)
                }
            case signature: Signature ⇒ processSignature(signature, thisClassID)
            case _                    ⇒
            // Synthetic, SourceFile, Deprecated, and SourceDebugExtension do not create dependencies
        }
    }

    /**
      * Extracts all source elements on which the given field definition depends.
      *
      * @param field The field whose dependencies will be extracted.
      * @param declaringClass This field's declaring class.
      * @param declaringTypeID The ID of the field's declaring class.
      */
    protected def process(field: Field, declaringClass: ClassFile, declaringTypeID: Int) {
        visit(declaringClass, field)
        val fieldID = sourceElementID(declaringClass.thisType, field)
        val Field(_ /*accessFlags*/ , _, fieldType) = field

        processDependency(
            fieldID,
            declaringTypeID,
            if (field.isStatic) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
        processDependency(fieldID, fieldType, IS_OF_TYPE)

        // The JVM 5 specification defines the following attributes:
        // ConstantValue, Synthetic, Signature, Deprecated, RuntimeVisibleAnnotations, 
        // and RuntimeInvisibleAnnotations
        field.attributes foreach {
            case AnnotationTable(_, annotations) ⇒
                // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, fieldID) }
            case ConstantValue(_, ot: ObjectType) ⇒
                processDependency(fieldID, ot, USES_CONSTANT_VALUE_OF_TYPE)
            case ConstantValue(_, at: ArrayType) ⇒
                processDependency(fieldID, at, USES_CONSTANT_VALUE_OF_TYPE)
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
      * @param declaringType The method's declaring class.
      * @param declaringTypeID The ID of the method's declaring class.
      */
    protected def process(method: Method, declaringClass: ClassFile, declaringTypeID: Int) {
        visit(declaringClass, method)
        val methodID = sourceElementID(declaringClass.thisType, method)
        val MethodDescriptor(parameterTypes, returnType) = method.descriptor

        processDependency(
            methodID,
            declaringTypeID,
            if (method.isStatic) IS_CLASS_MEMBER_OF else IS_INSTANCE_MEMBER_OF)
        processDependency(methodID, returnType, RETURNS)
        parameterTypes foreach { processDependency(methodID, _, HAS_PARAMETER_OF_TYPE) }

        // The Java 5 specification defines the following attributes:
        // Code, Exceptions, Synthetic, Signature, Deprecated, RuntimeVisibleAnnotations,
        // RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations,
        // RuntimeInvisibleParameterAnnotations, and AnnotationDefault
        method.body foreach { code ⇒
            val Code(_, _, instructions, exceptionTable, codeAttributes) = code
            // Process code instructions by calling the process method which is defined in
            // the generated InstructionDependencyExtractor super class.
            process(methodID, instructions)
            // add dependencies from the method to all throwables that are used in catch statements
            for {
                exceptionHandler ← exceptionTable
                if exceptionHandler.catchType.isDefined
            } {
                processDependency(methodID, exceptionHandler.catchType.get, CATCHES)
            }
            // The Java 5 specification defines the following attributes:
            // LineNumberTable, LocalVariableTable, and LocalVariableTypeTable)
            codeAttributes foreach {
                case LocalVariableTable(localVariableTable) ⇒
                    localVariableTable foreach { entry ⇒
                        processDependency(methodID, entry.fieldType, HAS_LOCAL_VARIABLE_OF_TYPE)
                    }
                case LocalVariableTypeTable(localVariableTypeTable) ⇒
                    localVariableTypeTable foreach { entry ⇒
                        processSignature(entry.signature, methodID)
                    }
                case _ ⇒ // The LineNumberTable does not define relevant dependencies
            }
        }
        method.attributes foreach {
            case AnnotationTable(_, annotations) ⇒
                // handles RuntimeVisibleAnnotations and RuntimeInvisibleAnnotations
                annotations foreach { process(_, methodID) }
            case ParameterAnnotationTable(_, parameterAnnotations) ⇒
                // handles RuntimeVisibleParameterAnnotations and 
                //RuntimeInvisibleParameterAnnotations
                parameterAnnotations foreach { pa ⇒
                    pa foreach { process(_, methodID, PARAMETER_ANNOTATED_WITH) }
                }
            case ExceptionTable(exceptionTable) ⇒
                exceptionTable foreach { processDependency(methodID, _, THROWS) }
            case elementValue: ElementValue ⇒ // ElementValues encode annotation default attributes
                processElementValue(elementValue, methodID)
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
      * NOTE: As described above, this method only considers types that
      * occur in type parameters. All other types are extracted in other
      * methods.
      *
      * @param signature The signature whose type parameters should be analyzed.
      * @param srcID The ID of the source, i.e. the ID of the element the signature is from.
      * @param isInTypeParameters Signals whether the current signature (part)
      *                           is already a part of a type parameter.
      */
    protected def processSignature(
        signature: SignatureElement,
        srcID: Int,
        isInTypeParameters: Boolean = false) {
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
        def processFormalTypeParameters(
            formalTypeParameters: Option[List[FormalTypeParameter]]) {
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
        def processSimpleClassTypeSignature(
            simpleClassTypeSignatures: SimpleClassTypeSignature) {
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
                        case Wildcard ⇒
                        // Wildcards refer to no type, hence there is nothing to do in this case.
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
                    processDependency(srcID, cts.objectType, USES_TYPE_IN_TYPE_PARAMETERS)
                processSimpleClassTypeSignature(cts.simpleClassTypeSignature)
            case ProperTypeArgument(_, fieldTypeSignature) ⇒
                // process proper type argument by calling this method again and passing
                // the proper type's field type signature as parameter
                processSignature(fieldTypeSignature, srcID)
            case BooleanType | ByteType | IntegerType | LongType | CharType | ShortType | FloatType | DoubleType | VoidType ⇒
                // add a dependency to base or void type if the type is used in a type parameter
                if (isInTypeParameters)
                    processDependency(srcID, signature.asInstanceOf[Type], USES_TYPE_IN_TYPE_PARAMETERS)
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
                processDependency(srcID, returnType, USES_DEFAULT_CLASS_VALUE_TYPE)
            case EnumValue(enumType, constName) ⇒
                processDependency(srcID, enumType, USES_DEFAULT_ENUM_VALUE_TYPE)
                processDependency(srcID, enumType, constName, USES_ENUM_VALUE)
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
    private def process(
        annotation: Annotation,
        srcID: Int,
        dependencyType: DependencyType = ANNOTATED_WITH) {
        val Annotation(annotationType, elementValuePairs) = annotation
        processDependency(srcID, annotationType, dependencyType)
        elementValuePairs foreach { evp ⇒ processElementValue(evp.value, srcID) }
    }

    /**
      * Extracts all dependencies found in the given instructions.
      *
      * @param methodId The ID of the source of the extracted dependencies.
      * @param instructions The instructions that should be analyzed for dependencies.
      */
    private def process(methodId: Int, instructions: Instructions) {

        for (i ← instructions if i != null) {
            (i.opcode: @annotation.switch) match {

                case 178 ⇒
                    val GETSTATIC(declaringClass, name, fieldType) = i.asInstanceOf[GETSTATIC]
                    processDependency(methodId, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, READS_FIELD)
                    processDependency(methodId, fieldType, USES_FIELD_READ_TYPE)

                case 179 ⇒
                    val PUTSTATIC(declaringClass, fieldName, fieldType) = i.asInstanceOf[PUTSTATIC]
                    processDependency(methodId, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, fieldName, WRITES_FIELD)
                    processDependency(methodId, fieldType, USES_FIELD_WRITE_TYPE)

                case 180 ⇒
                    val GETFIELD(declaringClass, name, fieldType) = i.asInstanceOf[GETFIELD]
                    processDependency(methodId, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, READS_FIELD)
                    processDependency(methodId, fieldType, USES_FIELD_READ_TYPE)

                case 181 ⇒
                    val PUTFIELD(declaringClass, fieldName, fieldType) = i.asInstanceOf[PUTFIELD]
                    processDependency(methodId, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, fieldName, WRITES_FIELD)
                    processDependency(methodId, fieldType, USES_FIELD_WRITE_TYPE)

                case 182 ⇒
                    val INVOKEVIRTUAL(declaringClass, name, methodDescriptor) = i.asInstanceOf[INVOKEVIRTUAL]
                    processDependency(methodId, declaringClass, USES_METHOD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, methodDescriptor, CALLS_METHOD)
                    methodDescriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(methodId, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(methodId, methodDescriptor.returnType, USES_RETURN_TYPE)

                case 183 ⇒
                    val INVOKESPECIAL(declaringClass, name, methodDescriptor) = i.asInstanceOf[INVOKESPECIAL]
                    processDependency(methodId, declaringClass, USES_METHOD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, methodDescriptor, CALLS_METHOD)
                    methodDescriptor.parameterTypes foreach {
                        parameterType ⇒ processDependency(methodId, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(methodId, methodDescriptor.returnType, USES_RETURN_TYPE)

                case 184 ⇒
                    val INVOKESTATIC(declaringClass, name, methodDescriptor) = i.asInstanceOf[INVOKESTATIC]
                    processDependency(methodId, declaringClass, USES_METHOD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, methodDescriptor, CALLS_METHOD)
                    methodDescriptor.parameterTypes foreach {
                        parameterType ⇒ processDependency(methodId, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(methodId, methodDescriptor.returnType, USES_RETURN_TYPE)

                case 185 ⇒
                    val INVOKEINTERFACE(declaringClass, name, methodDescriptor) = i.asInstanceOf[INVOKEINTERFACE]
                    processDependency(methodId, declaringClass, USES_METHOD_DECLARING_TYPE)
                    processDependency(methodId, declaringClass, name, methodDescriptor, CALLS_INTERFACE_METHOD)
                    methodDescriptor.parameterTypes foreach {
                        parameterType ⇒ processDependency(methodId, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(methodId, methodDescriptor.returnType, USES_RETURN_TYPE)

                case 186 ⇒
                    val INVOKEDYNAMIC(bootstrapMethod, name, methodDescriptor) = i.asInstanceOf[INVOKEDYNAMIC]
                    methodDescriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(methodId, parameterType, USES_PARAMETER_TYPE)
                    }
                // TODO complete the implementation of Invokedynamic w.r.t. the static dependencies defined in the "bootstrapmethod" 
                //    throw new UnsupportedOperationException("Java 7 Invokedynamic is not supported.")

                case 187 ⇒
                    processDependency(methodId, i.asInstanceOf[NEW].objectType, CREATES)

                case 189 ⇒
                    processDependency(methodId, i.asInstanceOf[ANEWARRAY].componentType, CREATES_ARRAY_OF_TYPE)

                case 192 ⇒
                    processDependency(methodId, i.asInstanceOf[CHECKCAST].referenceType, CASTS_INTO)

                case 193 ⇒
                    processDependency(methodId, i.asInstanceOf[INSTANCEOF].referenceType, CHECKS_INSTANCEOF)

                case 197 ⇒
                    processDependency(methodId, i.asInstanceOf[MULTIANEWARRAY].componentType, CREATES_ARRAY_OF_TYPE)

                case _ ⇒
            }
        }
    }

    protected def processDependency(
        id: Int,
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        dType: DependencyType) {
        processDependency(id, sourceElementID(declaringClass, methodName, methodDescriptor), dType)
    }

    protected def processDependency(
        id: Int,
        declaringClass: ObjectType,
        fieldName: String,
        dType: DependencyType) {
        processDependency(id, sourceElementID(declaringClass, fieldName), dType)
    }

    protected def processDependency(
        objectType: ObjectType,
        id: Int,
        dType: DependencyType) {
        processDependency(sourceElementID(objectType), id, dType)
    }

    /**
      * Processes a dependency between a source element identified by the given id to
      * some type.
      *
      * By default dependencies to primitive types (which includes dependencies to
      * arrays of primitive types) are not reported. If dependencies to primitive types
      * should be reported you can override this method with the following method:
      * {{{
      * override protected def processDependency(
      *      id: Int,
      *      aType: Type,
      *      dType: DependencyType) {
      *      processDependency(id, sourceElementID(aType), dType)
      * }
      * }}}.
      */
    protected def processDependency(id: Int, aType: Type, dType: DependencyType) {

        def process = processDependency(id, sourceElementID(aType), dType)

        aType match {
            case t: ObjectType                   ⇒ process
            case ArrayElementType(t: ObjectType) ⇒ process
            case _                               ⇒ /*Nothing to do.*/
        }
    }
}
