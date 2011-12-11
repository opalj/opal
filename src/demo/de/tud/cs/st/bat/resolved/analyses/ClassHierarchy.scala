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
 * Represents the project's class hierarchy.
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

    def update(classFiles: Traversable[ClassFile]) {
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
     */
    def subclasses(objectType: ObjectType): immutable.Set[ObjectType] =
        subclasses.apply(objectType).toSet

    /**
     * The classes and interfaces from which the given type directly inherits.
     */
    def superclasses(objectType: ObjectType): immutable.Set[ObjectType] =
        superclasses.apply(objectType).toSet

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

