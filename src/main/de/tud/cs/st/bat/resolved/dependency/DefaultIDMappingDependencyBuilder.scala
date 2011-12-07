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

/**
 * If you want to use the <code>SourceElementIDsMap</code> for <code>DependencyExtractor</code>'s
 * ID generation and if you also want to have dependencies to the underlying base type instead of
 * dependencies to array types, this trait can be mixed in.
 * All <code>getID</code> calls will be redirected to the corresponding <code>sourceElementID</code>
 * method implemented in <code>SourceElementIDsMap</code>.
 *
 * The only remaining abstract method is the <code>addDependency</code> method defined in
 * <code>DependencyBuilder</code>.
 *
 * @author Thomas Schlosser
 */
trait DefaultIDMappingDependencyBuilder extends DependencyBuilder with SourceElementIDsMap with UseIDOfBaseTypeForArrayTypes {

    /**
     * Gets a unique numerical identifier for the given class file.
     *
     * This method only calls the <code>sourceElementID(ClassFile)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.ClassFile)
     */
    def getID(classFile: ClassFile): Int =
        sourceElementID(classFile)

    /**
     * Gets a unique numerical identifier for the given type.
     *
     * This method only calls the <code>sourceElementID(Type)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.Type)
     */
    def getID(t: Type): Int =
        sourceElementID(t)

    /**
     * Gets a unique numerical identifier for the given pair of type and field.
     *
     * This method only calls the <code>sourceElementID(ObjectType, Field)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.ObjectType, de.tud.cs.st.bat.resolved.Field)
     */
    def getID(definingObjectType: ObjectType, field: Field): Int =
        sourceElementID(definingObjectType, field)

    /**
     * Gets a unique numerical identifier for the given pair of type and field name.
     *
     * This method only calls the <code>sourceElementID(ObjectType, String)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.ObjectType, java.lang.String)
     */
    def getID(definingObjectType: ObjectType, fieldName: String): Int =
        sourceElementID(definingObjectType, fieldName)

    /**
     * Gets a unique numerical identifier for the given pair of type and method.
     *
     * This method only calls the <code>sourceElementID(ObjectType, Method)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.ObjectType, de.tud.cs.st.bat.resolved.Method)
     */
    def getID(definingObjectType: ObjectType, method: Method): Int =
        sourceElementID(definingObjectType, method)

    /**
     * Gets a unique numerical identifier for the given triple of type, method name and method descriptor.
     *
     * This method only calls the <code>sourceElementID(ObjectType, String, MethodDescriptor)</code>
     * method implemented in <code>SourceElementIDsMap</code>.
     *
     * @see de.tud.cs.st.bat.resolved.dependency.DependencyBuilder#getID(de.tud.cs.st.bat.resolved.ObjectType, java.lang.String, de.tud.cs.st.bat.resolved.MethodDescriptor)
     */
    def getID(definingObjectType: ObjectType, methodName: String, methodDescriptor: MethodDescriptor): Int =
        sourceElementID(definingObjectType, methodName, methodDescriptor)

}