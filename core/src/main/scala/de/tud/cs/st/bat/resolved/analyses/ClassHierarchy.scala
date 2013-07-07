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
package analyses

import util.graphs.{ Node, toDot }
import reader.Java7Framework

/**
 * Represents the visible part of a project's class hierarchy. The visible part of a project's
 * class hierarchy consists of all classes defined in the analyzed class files and all
 * boundary class/interfaces. I.e., those classes which are seen when analyzing a specific
 * class file, but for which the respective class file is not seen.
 *
 * ==Usage==
 * To build the class hierarchy use the `++` and `+` method.
 *
 * ==Thread safety==
 * This class is immutable. Hence, concurrent access to the class hierarchy is supported.
 *
 * However, this also means that an update of the class hierarchy results in a new
 * class hierarchy object and, therefore, some external synchronization
 * is needed to make sure that the complete class hierarchy is constructed.
 * This decision was made to avoid any need for synchronization
 * once the class hierarchy is completely constructed.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy(
        protected val superclasses: Map[ObjectType, Set[ObjectType]] = Map(),
        protected val subclasses: Map[ObjectType, Set[ObjectType]] = Map()) {

    def ++(classFiles: Traversable[ClassFile]): ClassHierarchy = (this /: classFiles)(_ + _)

    def +(classFile: ClassFile): ClassHierarchy = {
        this.+(classFile.thisClass, classFile.superClass.toSeq ++ classFile.interfaces)
    }

    def +(aType: ObjectType, aTypesSupertypes: Seq[ObjectType]): ClassHierarchy = {
        var thisTypesSuperclasses = superclasses.getOrElse(aType, Set.empty) ++ aTypesSupertypes
        val newSuperclasses = superclasses.updated(aType, thisTypesSuperclasses)

        // we want to make sure that this type is seen even if there are no subtypes
        var newSubclasses = subclasses.updated(aType, subclasses.getOrElse(aType, Set.empty))
        newSubclasses =
            (newSubclasses /: aTypesSupertypes)((sc, supertype) ⇒ {
                sc.updated(supertype, sc.getOrElse(supertype, Set.empty) + aType)
            })

        new ClassHierarchy(newSuperclasses, newSubclasses)
    }

    /**
     * Calculates this project's root types. The set of a Java project's root types in general consists of
     * the single class `java.lang.Object`. However, if an analysis only analyzes a subset of all classes
     * of an application then it may be possible that multiple root types exist. E.g., if you define a
     * class (not interface!) that inherits from some not-analyzed library class then it will be considered
     * as a root type.
     *
     * ==Note==
     * This set contains all types seem by the class hierarchy analysis, but it is not necessarily the case
     * that the defining class file is available (`Project.classes("SOME ROOT TYPE")`). Imagine that you
     * just analyze an application's class files. In this case it is extremely likely that you will have
     * seen the type `java.lang.Object`, however the class file will not be available.
     */
    def rootTypes: Iterable[ObjectType] = {
        superclasses.view.filter((_: (ObjectType, Set[ObjectType]))._2.isEmpty).map(_._1)
    }

    /**
     * The classes (and interfaces if the given type is an interface type)
     * that **directly** inherit from the given type.
     *
     * If we have not (yet) analyzed the class file implementing the given
     * type (i.e., the class file was not yet passed to `+`) or if
     * we have not yet seen any direct subtype of the given type, `scala.None` is
     * returned. I.e., we know nothing about the respective type. If we have
     * seen the class file, but did not see (so far) any class files that
     * implements a type that directly inherits from the given type, an empty
     * set is returned. I.e., if you analyzed all class files of a project
     * and then ask for the subclasses of a specific type and an
     * empty set is returned, then you have the guarantee that no class in the
     * project **directly** inherits form the given type.
     *
     * @return The direct subtypes of the given type or `None` if the analysis
     *      is not conclusive.
     */
    def subclasses(objectType: ObjectType): Option[Set[ObjectType]] = subclasses.get(objectType)

    /**
     * The set of all classes (and interfaces) that(directly or indirectly)
     * inherit from the given type.
     *
     * @see subclasses(ObjectType) For general remarks about the precision of
     *      the analysis.
     * @return The set of all direct and indirect subtypes of the given type.
     */
    def subtypes(objectType: ObjectType): Option[Set[ObjectType]] = {
        val theSubclasses = subclasses.get(objectType)
        theSubclasses.map(t ⇒
            for {
                subclass ← theSubclasses.get
                subtype ← subtypes(subclass).getOrElse(Set()) + subclass
            } yield subtype
        )
    }

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
    def superclasses(objectType: ObjectType): Option[Set[ObjectType]] =
        superclasses.get(objectType)

    /**
     * Determines if `currentType` is a subtype of `supertype`.
     *
     * @return Some(true) if currentType is a subtype of supertype. Some(false)
     *      if currentType is not a subtype of supertype and None if the analysis is
     *      not conclusive. The latter can happen if the class hierarchy is not
     *      completely available and hence precise information about a type's supertypes
     *      is not available.
     */
    def isSubtypeOf(currentType: ObjectType, supertype: ObjectType): Option[Boolean] = {
        if (currentType == supertype) {
            Some(true);
        } else {
            // If we don't have the complete hierarchy available and we
            // are not able to identify that the current type is actually
            // a subtype of the given type (supertype) and if we find a
            // type for which we have not seen the class file, the
            // analysis is considered to be not conclusive.
            var nonConclusive = false;
            for {
                superclasses ← superclasses.get(currentType).toList
                superclass ← superclasses
            } {
                isSubtypeOf(superclass, supertype) match {
                    case Some(false)        ⇒ /* let's continue the search */ ;
                    case found @ Some(true) ⇒ return found;
                    case None ⇒
                        // It is still possible that we are able to determine that currentType 
                        // is a subtype of supertype. 
                        nonConclusive = true;
                }
            }
            if (nonConclusive) {
                None
            } else {
                Some(false)
            }
        }
    }

    /**
     * Calculates the set of classes and interfaces from which the given types
     * directly inherit.
     *
     * If the class file of a given object type was not previously analyzed
     * an error will be returned.
     */
    def superclasses(objectTypes: Traversable[ObjectType]): Set[ObjectType] =
        (Set.empty[ObjectType] /: objectTypes)(_ ++ superclasses.apply(_))

    /**
     * Returns a view of the class hierarchy as a graph.
     */
    def toGraph: Node = new Node {

        val sourceElementIDs = new SourceElementIDsMap
        import sourceElementIDs.{ sourceElementID ⇒ id }

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

object ClassHierarchy {

    /**
     * Creates a new empty class hierarchy.
     */
    def Empty = new ClassHierarchy()

    /**
     * Creates a new ClassHierarchy object that predefines the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions. See the file
     * ClassHierarchyJVMExceptions.ths (text file) for further details.
     */
    def createPreInitializedClassHierarchy(): ClassHierarchy = {
        import scala.io.BufferedSource
        import util.ControlAbstractions._

        withResource(this.getClass().getResourceAsStream("ClassHierarchyJVMExceptions.ths")) { in ⇒
            var ch = Empty
            val Spec = """(\S+)\s*>\s*(.+)""".r
            val specLines = new BufferedSource(in).getLines.map(_.trim).filterNot((l) ⇒ l.startsWith("#") || l.length == 0)
            for {
                Spec(superclass, subclasses) ← specLines
                superclasses = List(ObjectType(superclass))
                subclass ← subclasses.split(",").map(_.trim)
            } {
                ch += (ObjectType(subclass), superclasses)
            }
            ch
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

        val classHierarchy = (new ClassHierarchy /: args)(_ ++ Java7Framework.ClassFiles(_))

        println(toDot.generateDot(Set(classHierarchy.toGraph)))
    }
}

