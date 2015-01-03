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
import org.opalj.br._
import org.opalj.br.analyses._

object ClassHierarchy {

    val ArrayListType = ObjectType("java/util/ArrayList")
    val project = Project(new java.io.File("/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre/lib/rt.jar"))

    val ch = project.classHierarchy

    /*
    val ownMethods = project.classFile(ArrayListType).get.methods
    val superMethods = ch.supertypes(ArrayListType).map(ch.allMethods(project, _)).flatten
    val allMethods = ch.allMethods(project, ArrayListType, (m) ⇒ !m.isPrivate && !m.isAbstract)
    val inheritedAndNotOverriddenMethods = allMethods.filter(am ⇒ !ownMethods.exists(om ⇒ om.hasSameSignature(am)))
    inheritedAndNotOverriddenMethods.size
    inheritedAndNotOverriddenMethods.map(_.toJava).mkString("\n")

    val overriddenMethods = superMethods.filter(sm ⇒ ownMethods.exists(_.hasSameSignature(sm)))
    overriddenMethods.map(_.toJava).mkString("\n")
    */
}