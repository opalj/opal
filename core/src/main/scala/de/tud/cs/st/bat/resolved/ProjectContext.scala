/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

/**
 * The `ProjectContext` represents a Java project - that is, an
 * application/library/framework and the libraries and frameworks used by the it.
 *
 * @author Michael Eichberg
 */
class ProjectContext {

    import java.util.concurrent.atomic.AtomicInteger

    //
    // ObjectType Ids
    //

    private[this] final val nextObjectTypeId = new AtomicInteger(0)

    private[resolved] def getNextObjectTypeId(): Int = nextObjectTypeId.getAndIncrement()

    def getMaxObjectTypeId(): Int = nextObjectTypeId.get()

    //
    // ArrayType Ids
    //

    private[this] final val nextArrayTypeId = new AtomicInteger(0)

    private[resolved] def getNextArrayTypeId(): Int = nextArrayTypeId.getAndIncrement()

    def getMaxArrayTypeId(): Int = nextArrayTypeId.get()

    //
    // Method Ids
    //

    private[this] final val nextMethodId = new AtomicInteger(0)

    private[resolved] def getNextMethodId(): Int = nextMethodId.getAndIncrement

    def getMaxMethodId(): Int = nextMethodId.get

    //
    // Field Ids
    //

    private[this] final val nextFieldId = new AtomicInteger(0)

    private[resolved] def getNextFieldId(): Int = nextFieldId.getAndIncrement

    def getMaxFieldId(): Int = nextFieldId.get

    //

    final val Object = ObjectType("java/lang/Object")(this)
    final val String = ObjectType("java/lang/String")(this)
    final val Class = ObjectType("java/lang/Class")(this)
    final val Throwable = ObjectType("java/lang/Throwable")(this)
    final val Error = ObjectType("java/lang/Error")(this)
    final val Exception = ObjectType("java/lang/Exception")(this)
    final val RuntimeException = ObjectType("java/lang/RuntimeException")(this)
    final val IndexOutOfBoundsException = ObjectType("java/lang/IndexOutOfBoundsException")(this)

    final val MethodHandle = ObjectType("java/lang/invoke/MethodHandle")(this)
    final val MethodType = ObjectType("java/lang/invoke/MethodType")(this)

    // Exceptions and errors that may be throw by the JVM (i.e., instances of these 
    // exceptions may be created at runtime by the JVM)
    final val ExceptionInInitializerError = ObjectType("java/lang/ExceptionInInitializerError")(this)
    final val BootstrapMethodError = ObjectType("java/lang/BootstrapMethodError")(this)
    final val OutOfMemoryError = ObjectType("java/lang/OutOfMemoryError")(this)

    final val NullPointerException = ObjectType("java/lang/NullPointerException")(this)
    final val ArrayIndexOutOfBoundsException = ObjectType("java/lang/ArrayIndexOutOfBoundsException")(this)
    final val ArrayStoreException = ObjectType("java/lang/ArrayStoreException")(this)
    final val NegativeArraySizeException = ObjectType("java/lang/NegativeArraySizeException")(this)
    final val IllegalMonitorStateException = ObjectType("java/lang/IllegalMonitorStateException")(this)
    final val ClassCastException = ObjectType("java/lang/ClassCastException")(this)
    final val ArithmeticException = ObjectType("java/lang/ArithmeticException")(this)

    // the following types are relevant when checking the subtype relation between
    // two reference types where the subtype is an array type 
    final val Serializable = ObjectType("java/io/Serializable")(this)
    final val Cloneable = ObjectType("java/lang/Cloneable")(this)

    final val ArrayOfObjects = ArrayType(ObjectType.Object)(this)
}