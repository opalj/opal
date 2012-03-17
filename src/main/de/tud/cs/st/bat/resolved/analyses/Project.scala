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

import util.graphs.{ Node, toDot }

import reader.Java6Framework

/**
 * Represents some software project; i.e., all class files of a project.
 *
 * ==Usage==
 * To create a representation of a project use the ++ and + method.
 *
 * @author Michael Eichberg
 */
class Project(
        val classes: Map[ObjectType, ClassFile] = Map(),
        val classHierarchy: ClassHierarchy = new ClassHierarchy()) {

    def ++(classFiles: Traversable[ClassFile]): Project = (this /: classFiles)(_ + _)

    def +(classFile: ClassFile): Project = {
        new Project(classes + ((classFile.thisClass, classFile)), classHierarchy + classFile)
    }

    /**
     * Looks up the method declaration; i.e., the class/interface that
     * declares the method. In most cases this will be the receiver's class.
     * In some cases – however – it might be one (or more) superclasses. In the latter
     * case the declaration of the method by a superclass has precendence over a
     * declaration by an interface.
     *
     * @return Some((ClassFile,Method)) if the method is found. None if the method is not
     * 	found. This can happen under two circumstances. First, not all class files
     * 	referred to/used by the project are (yet) analyzed; i.e., we do not have the
     * 	complete view on all class files belonging to the project. Second, the analyzed
     * 	class files do not belong together (they either belong to different projects or
     * 	to incompatible versions of the same project.)
     */
    def lookupMethod(
        caller: ObjectType,
        receiver: ObjectType, methodName: String, methodDescriptor: MethodDescriptor): Option[(ClassFile, Method)] = {
        for (clazz <- classes.get(receiver).toList;
        	method @ Method(_,`methodName`,`methodDescriptor`,_) <- clazz.methods) {
        }
        None

        throw new UnsupportedOperationException("Will be implemented soon!");
    }

}

