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
 * Represents the visible part of a project's class hierarchy.
 *
 * To construct the class hierarchy use the update method.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy {

    import dependency.SourceElementIDs.{ sourceElementID ⇒ id }

    import scala.collection._
    import mutable.Map
    import mutable.Set

    private[this] var superclasses: Map[ObjectType, Set[ObjectType]] = Map()
    private[this] var subclasses: Map[ObjectType, Set[ObjectType]] = Map()

    final def update(classFiles: Traversable[ClassFile]) {
        classFiles.foreach(update(_))
    }

    def update(classFile: ClassFile) {
        val thisType = classFile.thisClass
        val thisTypesSuperclasses = superclasses.getOrElseUpdate(thisType, Set.empty)

        subclasses.getOrElseUpdate(thisType, Set.empty) // we want to make sure that this type is seen

        thisTypesSuperclasses ++= classFile.interfaces
        for (supertype ← classFile.interfaces) {
            subclasses.getOrElseUpdate(supertype, Set.empty) += thisType
        }
        classFile.superClass.foreach(supertype ⇒ {
            thisTypesSuperclasses += supertype
            subclasses.getOrElseUpdate(supertype, Set.empty) += thisType
        })
    }

    /**
     * The classes (and interfaces if the given type is an interface type)
     * that directly inherit from the given type.
     *
     * If we have not (yet) analyzed the class file implementing the given
     * type (i.e., the class file was not yet passed to "update"), None is
     * returned. I.e., we know nothing about the respective type. If we have
     * seen the class file, but did not see (so far) any class files that
     * implements a type that directly inherits from the given type, an empty
     * set is returned. I.e., if you have passed all class files of a project
     * to update and then ask for the subclasses of a specific type and an
     * empty set is returned, then you have the guarantee no class in the
     * project inherits form the given type.
     *
     * @return The direct subtypes of the given type.
     */
    def subclasses(objectType: ObjectType): Option[immutable.Set[ObjectType]] =
        subclasses.get(objectType).map(_.toSet)

    /**
     * The classes and interfaces from which the given type directly inherits.
     *
     * If we have not (yet) seen the class file of the given type – i.e., the
     * update method was not yet called with the class file that implements the
     * given type as a parameter –  None is returned. Hence, None indicates
     * that we know nothing about the superclasses of the given type. This is
     * in particular the case if you analyze a project's class files but
     * do not also analyze all used libraries.
     *
     * The empty set will only be returned, if the class file of "java.lang.Object"
     * was analyzed, and the given object type represents "java.lang.Object".
     * Recall, that interfaces always (implicitly) inherit from java.lang.Object.
     *
     * @return The direct supertypes of the given type.
     */
    def superclasses(objectType: ObjectType): Option[immutable.Set[ObjectType]] =
        superclasses.get(objectType).map(_.toSet)

    /**
     * The classes and interfaces from which the given types directly inherit.
     */
    def superclasses(objectTypes: Traversable[ObjectType]): immutable.Set[ObjectType] = {
        var allSuperclasses = immutable.Set.empty[ObjectType]
        for (objectType ← objectTypes; superclass ← superclasses.apply(objectType)) {
            allSuperclasses = allSuperclasses + superclass
        }
        allSuperclasses
    }

    /**
     * Returns a view of the class hierarchy as a graph.
     */
    def toGraph: Node = new Node {

        private val nodes: Map[ObjectType, Node] = Map() ++ subclasses.keys.map(t ⇒ {
            val entry: (ObjectType, Node) = (
                t,
                new Node {
                    def uniqueId = id(t)
                    def toHRR: Option[String] = Some(t.className)
                    def foreachSuccessor(f: Node ⇒ _) {
                        subclasses.apply(t).foreach(st ⇒ {
                            f(nodes(st))
                        })
                    }
                }
            )
            entry
        })

        // a virtual root node
        def uniqueId = -1
        def toHRR = None
        def foreachSuccessor(f: Node ⇒ _) {
            /**
             * We may not see the class files of all classes that are referred
             * to in the class files that we did see. Hence, we have to be able
             * to handle partial class hierarchies.
             */
            val rootTypes = nodes.filterNot({ case (t, _) ⇒ superclasses.isDefinedAt(t) })
            rootTypes.values.foreach(f)
        }
    }
}
object ClassHierarchyVisualizer {

    def main(args: Array[String]) {

        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".zip") || arg.endsWith(".jar"))) {
            println("Usage: java …ClassHierarchy <ZIP or JAR file containing class files>+")
            println("(c) 2011 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
            sys.exit(1)
        }

        val classHierarchy = new ClassHierarchy

        for (arg ← args; classFile ← Java6Framework.ClassFiles(arg)) classHierarchy.update(classFile)

        println(toDot.generateDot(Set(classHierarchy.toGraph)))
    }
}

