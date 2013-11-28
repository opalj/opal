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

import collection.Seq

/**
 * A method descriptor represents the parameters that the method takes and
 * the value that it returns.
 *
 * @author Michael Eichberg
 */
sealed abstract class MethodDescriptor extends BootstrapArgument {

    def parameterTypes: IndexedSeq[FieldType]

    def parametersCount: Int

    def returnType: Type

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toJava(methodName: String): String =
        returnType.toJava+" "+
            methodName+
            "("+
            parameterTypes.map(_.toJava).mkString(",")+
            ")"

    def toUMLNotation: String =
        "("+{
            if (parameterTypes.size == 0)
                ""
            else
                (parameterTypes.head.toJava /: parameterTypes.tail)(_+", "+_.toJava)
        }+") : "+returnType.toJava

    def toString: String = toUMLNotation
}

// 
// To optimize the overall memory consumption and to facilitate the storage of 
// method descriptors in sets, we have specialized the MethodDescriptor
// (Done after a study of the heap memory usage)
//

private final object NoArgumentAndNoReturnValueMethodDescriptor
        extends MethodDescriptor {

    def returnType = VoidType

    def parameterTypes = IndexedSeq.empty

    def parametersCount: Int = 0

    // the default equals and hashCode implementations are a perfect fit
}

private final class NoArgumentMethodDescriptor(
    val returnType: Type)
        extends MethodDescriptor {

    def parameterTypes = IndexedSeq.empty

    def parametersCount: Int = 0

    override def hashCode: Int = returnType.hashCode()

    override def equals(other: Any): Boolean = {
        other match {
            case that: NoArgumentMethodDescriptor ⇒
                (that.returnType eq this.returnType)
            case _ ⇒
                false
        }
    }
}

private final class SingleArgumentMethodDescriptor(
    val parameterType: FieldType,
    val returnType: Type)
        extends MethodDescriptor {

    def parameterTypes = IndexedSeq(parameterType)

    def parametersCount: Int = 1

    override val hashCode: Int = (returnType.hashCode() * 61) + parameterType.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: SingleArgumentMethodDescriptor ⇒
                (that.parameterType eq this.parameterType) && (that.returnType eq this.returnType)
            case _ ⇒
                false
        }
    }
}

private final class TwoArgumentsMethodDescriptor(
    val firstParameterType: FieldType,
    val secondParameterType: FieldType,
    val returnType: Type)
        extends MethodDescriptor {

    def parameterTypes = IndexedSeq(firstParameterType, secondParameterType)

    def parametersCount: Int = 2

    override val hashCode: Int =
        ((returnType.hashCode() * 61) +
            firstParameterType.hashCode) * 13 +
            secondParameterType.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: TwoArgumentsMethodDescriptor ⇒
                (that.firstParameterType eq this.firstParameterType) &&
                    (that.secondParameterType eq this.secondParameterType) &&
                    (that.returnType eq this.returnType)
            case _ ⇒
                false
        }
    }
}

private final class MultiArgumentsMethodDescriptor(
    val parameterTypes: IndexedSeq[FieldType],
    val returnType: Type)
        extends MethodDescriptor {

    def parametersCount: Int = parameterTypes.size

    final override val hashCode: Int =
        (returnType.hashCode() * 13) + parameterTypes.hashCode

    final override def equals(other: Any): Boolean = {
        other match {
            case that: MethodDescriptor ⇒
                this.parametersCount == that.parametersCount &&
                    (this.returnType eq that.returnType) &&
                    {
                        var i = parametersCount
                        while (i > 0) {
                            i = i - 1
                            if (this.parameterTypes(i) ne that.parameterTypes(i))
                                return false
                        }
                        true
                    }
            case _ ⇒
                false
        }
    }
}

object HasNoArgsAndReturnsVoid {

    def unapply(md: MethodDescriptor): Boolean =
        md match {
            case NoArgumentAndNoReturnValueMethodDescriptor ⇒ true
            case _ ⇒ false
        }
}

/**
 * Defines extractor and factory methods for MethodDescriptors.
 *
 * @author Michael Eichberg
 */
object MethodDescriptor {

    def NoArgsAndReturnVoid: MethodDescriptor = NoArgumentAndNoReturnValueMethodDescriptor

    final val SignaturePolymorphicMethod: MethodDescriptor =
        new SingleArgumentMethodDescriptor(ArrayType.ArrayOfObjects, ObjectType.Object)

    def unapply(md: MethodDescriptor) = Some(md.parameterTypes, md.returnType)

    def apply(
        parameterTypes: IndexedSeq[FieldType],
        returnType: Type): MethodDescriptor = {
        if (parameterTypes.isEmpty) {
            if (returnType == VoidType)
                NoArgumentAndNoReturnValueMethodDescriptor
            else
                new NoArgumentMethodDescriptor(returnType)
        } else if (parameterTypes.size == 1) {
            new SingleArgumentMethodDescriptor(parameterTypes(0), returnType)
        } else if (parameterTypes.size == 2) {
            new TwoArgumentsMethodDescriptor(parameterTypes(0), parameterTypes(1), returnType)
        } else {
            new MultiArgumentsMethodDescriptor(parameterTypes, returnType)
        }
    }

    def apply(md: String): MethodDescriptor = {
        var index = 1 // we are not interested in the leading '('
        var parameterTypes: IndexedSeq[FieldType] = IndexedSeq.empty
        while (md.charAt(index) != ')') {
            val (ft, nextIndex) = parseParameterType(md, index)
            parameterTypes = parameterTypes :+ ft
            index = nextIndex
        }

        val returnType = ReturnType(md.substring(index + 1))

        apply(parameterTypes, returnType)
    }

    private[this] def parseParameterType(md: String, startIndex: Int): (FieldType, Int) = {
        val td = md.charAt(startIndex)
        (td: @scala.annotation.switch) match {
            case 'L' ⇒
                val endIndex = md.indexOf(';', startIndex + 1)
                ( // this is the return tuple
                    ObjectType(md.substring(startIndex + 1, endIndex)),
                    endIndex + 1
                )
            case '[' ⇒
                val (ft, index) = parseParameterType(md, startIndex + 1)
                ( // this is the return tuple
                    ArrayType(ft),
                    index
                )
            case _ ⇒
                ( // this is the return tuple
                    FieldType(td.toString),
                    startIndex + 1
                )
        }
    }
}







