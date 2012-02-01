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
package checking

import reader.Java6Framework
import analyses.ClassHierarchy

/**
 * Represents a configuration of a project's allowed/expected dependencies.
 * First define the ensembles, then the rules and at last specify the
 * class files that should be analyzed. The rules will then be automatically
 * evaluated.
 *
 * @author Michael Eichberg
 */
class Specification extends SourceElementIDsMap with ReverseMapping with UseIDOfBaseTypeForArrayTypes {

    type SourceElementID = Int

    trait SourceElementsMatcher { left ⇒

        def extension(): Set[SourceElementID]

        def ||(right: SourceElementsMatcher): SourceElementsMatcher = {
            return new SourceElementsMatcher {
                def extension() = {
                    left.extension ++ (right.extension)
                }

                override def toString() = {
                    "OrSourceElementsMatcher("+left.toString+","+right.toString+")"
                }
            };
        }
    }

    /**
     * @param specifiedPackageName The name of a package in binary notation. (I.e., "/" are used to separate
     * a package name's segments; e.g., "java/lang/Object").
     *
     * @author Michael Eichberg
     */
    case class PackageNameBasedMatcher(val packageName: String, val matchSubpackages: Boolean = false)
            extends SourceElementsMatcher {

        require(packageName.indexOf('*') == -1)
        require(packageName.indexOf('.') == -1)

        def extension(): Set[SourceElementID] = {
            {
                for (
                    classFile ← classFiles.values if {
                        val thisClassPackageName = classFile.thisClass.packageName
                        thisClassPackageName.startsWith(packageName) && (
                            matchSubpackages ||
                            thisClassPackageName.length() == packageName.length()
                        )
                    }
                ) yield {
                    val classFileID = sourceElementID(classFile)
//                    println(classFile.thisClass.className +"=>"+ classFileID+" =>" +sourceElementIDtoString(classFileID))
                    val methodIDs = classFile.methods.map(sourceElementID(classFile, _))
                    val fieldIDs = classFile.fields.map(sourceElementID(classFile, _))
                    var r = fieldIDs.toSet ++ methodIDs + classFileID
//                    println(this.toString+" => "+classFile.thisClass.className + " : " +r.mkString(","))
                    r
                }

            }.flatten.toSet
        }

        override def toString = {
            var s = "Packages("+packageName.replace('/', '.')+".*"
            if (matchSubpackages)
                s += "*"
            s += ")"
            s
        }
    }

    case class ClassMatcher(val className: String) extends SourceElementsMatcher {
        def extension(): Set[SourceElementID] = {
            for (classFile ← classFiles.values if className == classFile.thisClass.className)
                yield Set(sourceElementID(classFile)) union
                classFile.methods.map(sourceElementID(classFile, _)).toSet union
                classFile.fields.map(sourceElementID(classFile, _)).toSet
        }.flatten.toSet
    }

    case object Nothing extends SourceElementsMatcher {
        def extension(): Set[SourceElementID] = Set();
    }

    val dependencyExtractor = new DependencyExtractor(Specification.this) with NoSourceElementsVisitor {

        val outgoing = scala.collection.mutable.Map[SourceElementID, scala.collection.mutable.Set[(SourceElementID, DependencyType)]]()
        val incoming = scala.collection.mutable.Map[SourceElementID, scala.collection.mutable.Set[(SourceElementID, DependencyType)]]()

        def processDependency(sourceID: SourceElementID, targetID: SourceElementID, dType: DependencyType) {
            outgoing.
                getOrElseUpdate(sourceID, { scala.collection.mutable.Set() }).
                add((targetID, dType))
            incoming.
                getOrElseUpdate(targetID, { scala.collection.mutable.Set() }).
                add((sourceID, dType))
        }
    }

    val classHierarchy = new ClassHierarchy {}

    val classFiles = scala.collection.mutable.Map[ObjectType, ClassFile]()

    val ensembles = scala.collection.mutable.Map[Symbol, (SourceElementsMatcher, Set[SourceElementID])]()

    def ensemble(ensembleName: Symbol)(sourceElementMatcher: SourceElementsMatcher) {
        if (ensembles.contains(ensembleName))
            throw new IllegalArgumentException("Ensemble is already defined: "+ensembleName)

        ensembles.put(ensembleName, (sourceElementMatcher, Set()))
    }

    implicit def StringToPackageNameBasedMatcher(matcher: String): SourceElementsMatcher = {
        if (matcher endsWith ".*")
            return new PackageNameBasedMatcher(matcher.substring(0, matcher.length() - 2).replace('.', '/'))
        if (matcher endsWith ".**")
            return new PackageNameBasedMatcher(matcher.substring(0, matcher.length() - 3).replace('.', '/'), true)
        if (matcher.indexOf('*') == -1)
            return new ClassMatcher(matcher.replace('.', '/'))

        throw new IllegalArgumentException("Unsupported pattern: "+matcher);
    }

    case class Violation(source: SourceElementID, target: SourceElementID, dependencyType: DependencyType, description: String) {

        override def toString(): String = {
            description+": "+sourceElementIDtoString(source)+" "+dependencyType+" of "+sourceElementIDtoString(target)
        }

    }

    trait DependencyChecker {
        def violations(): Set[Violation]
    }

    var dependencyCheckers: List[DependencyChecker] = Nil

    case class GlobalIncomingConstraint(sourceEnsemble: Symbol, targetEnsemble: Symbol) extends DependencyChecker {
        def violations() = {
            val (_, sourceEnsembleElements) = ensembles(sourceEnsemble)
            val (_, targetEnsembleElements) = ensembles(targetEnsemble)
            for (
                targetEnsembleElement ← targetEnsembleElements if dependencyExtractor.incoming.contains(targetEnsembleElement);
                (incomingElement, dependencyType) ← dependencyExtractor.incoming(targetEnsembleElement) if !(sourceEnsembleElements.contains(incomingElement) || targetEnsembleElements.contains(incomingElement))
            ) yield Violation(incomingElement, targetEnsembleElement, dependencyType, "violation of a global incoming constraint ")
        }

        override def toString =
            "Constraint: only "+sourceEnsemble+" is allowed to depend on "+targetEnsemble
    }

    case class only(sourceEnsemble: Symbol) {
        def is_allowed_to_depend_on(targetEnsemble: Symbol) {
            dependencyCheckers = GlobalIncomingConstraint(sourceEnsemble, targetEnsemble) :: dependencyCheckers
        }
    }

    def analyze(classFileProviders: Traversable[Traversable[ClassFile]]) {
        // 1. create and update the support data structures
        for (classFileProvider ← classFileProviders; classFile ← classFileProvider) {
            classHierarchy.update(classFile)
            classFiles.put(classFile.thisClass, classFile)
            dependencyExtractor.process(classFile)
        }

        // 2. calculate the extension of the ensembles
        for ((ensembleName, (sourceElementMatcher, _)) ← ensembles) {
            val extension = sourceElementMatcher.extension()
//            println(
//                ensembleName+
//                    " : "+
//                    sourceElementMatcher+
//                    " => "+
//                    {
//                        if (extension.isEmpty)
//                            "NO ELEMENTS"
//                        else {
//                            val ex = extension.toList
//                            (("\n\t"+extension.head.toString+":"+sourceElementIDtoString(extension.head)) /: extension.tail)((s,id) => s+"\n\t"+id+":"+sourceElementIDtoString(id))
//                        }
//                    }
//            )
            ensembles.update(ensembleName, (sourceElementMatcher, extension))
        }

        // 3. check all rules
        for (dependencyChecker ← dependencyCheckers) {
            println("Checking: "+dependencyChecker)
            for (violation ← dependencyChecker.violations) println(violation)
        }
    }

    implicit def FileToClassFileProvider(file: java.io.File): Traversable[ClassFile] = {
        if (!file.exists())
            throw new IllegalArgumentException("the given file: "+file+" does not exist");

        if (file.isFile()) {
            return List(Java6Framework.ClassFile(() ⇒ new java.io.FileInputStream(file)))
        }

        // file.isDirectory
        var classFiles: List[ClassFile] = Nil
        var directories = List(file)
        while (directories.nonEmpty) {
            val directory = directories.head
            println("reading class files in directory: "+directory);
            directories = directories.tail
            for (file ← directory.listFiles()) {
                if (file.isDirectory()) {
                    directories = file :: directories
                }
                if (file.getName().endsWith(".class")) {
                    classFiles = Java6Framework.ClassFile(() ⇒ new java.io.FileInputStream(file)) :: classFiles
                }
            }
        }

        return classFiles;
    }

}