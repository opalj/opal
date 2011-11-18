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
package de.tud.cs.st.bat.reader

/**
 * <p>
 * Framework to align all readers to one specific representations per class file entity. E.g.
 * all readers use the same type for Constant_Pool_Entry, Attribute, etc.. However, this class
 * does not prescribe the concrete representation e.g. of a field or method.
 * </p>
 * <p>
 * We do want to have a concrete framework for reading class files that can be tailored
 * w.r.t. the OO representation that is generated while parsing the class files. <br />
 *
 * To enable this flexibility the class (traits) to read in various parts of a Java class file call
 * factory methods to create objects that
 * represent each "significant (read: all entities described in the JVM Spec.)" class file entry.
 * Since each reader of a class file entity should be independently reusable, abstract types and factory methods are used to
 * specify dependencies of a reader on entities read by other readers; readers do not have direct
 * dependencies on other readers or the created classes.
 * However, some readers do make some (minimal) assumptions about the constant pool and, hence,
 * specify this dependency by referring to an interface that represents the constant_pool and
 * which declares the required method.
 * </p>
 *
 * @author Michael Eichberg
 */
trait Java2Reader
        extends Constant_PoolReader
        with ClassFileReader
        with InterfacesReader
        with FieldsReader
        with MethodsReader
        with AttributesReader // the attributes reader has to come before the attributes!
        //	the Unknown_attributeReader is not specified here to make it possible to use the "SkipUnkn..." or the "Unknow.." reader.
        with ConstantValue_attributeReader
        with Deprecated_attributeReader
        with EnclosingMethod_attributeReader
        with Exceptions_attributeReader
        with InnerClasses_attributeReader
        with LineNumberTable_attributeReader
        with SourceFile_attributeReader
        with Synthetic_attributeReader
        with LocalVariableTable_attributeReader
        with LocalVariableTypeTable_attributeReader
        with Code_attributeReader
        with CodeReader {

}
