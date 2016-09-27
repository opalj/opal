/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj
package fpcf
package properties

//sealed trait FieldTypePropertyMetaInformation extends PropertyMetaInformation {
//
//    final type Self = FieldType
//
//}
//
///**
// * Specifies the type of the values stored in a field. In the worst case the type is
// * equivalent to the declared type. 
// * 
// * @author Michael Eichberg
// */
//sealed trait FieldType        extends OrderedProperty        with FieldTypePropertyMetaInformation {
//
//    final def key = FieldType.key
//}
///**
// * Common constants use by all [[FieldType]] properties associated with methods.
// */
//object FieldType extends FieldTypePropertyMetaInformation {
//
//    final val key: PropertyKey[FieldType] = PropertyKey.create(
//        "FieldType",
//    )
//}
//
//case object TheDeclaredType extends FieldType {
//    final val isRefineable = true
//
//    def isValidSuccessorOf(other: OrderedProperty): Option[String] = {
//        if (other == UnknownObjectImmutability)
//            None
//        else
//            Some(s"impossible refinement of $other to $this")
//    }
//
//    def isMutable: Answer = Unknown
//}
