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

/**
 * Writes out (a subset of) the class hierarchy in the format used by the
 * [[de.tud.cs.st.bat.resolved.analyses.ClassHierarchy]] to create the pre-initialized
 * class hierarchy.
 *
 * The format is:
 * <pre>
 * SUPERTYPE " &gt; " SUBTYPE [", " SUBTYPE]*
 * </pre>
 *
 * @author Michael Eichberg
 */
object ClassHierarchyExtractor {

    def main(args: Array[String]) {

        import reader.Java7Framework.ClassFiles

        if (args.length < 3 || !args.drop(2).forall(_.endsWith(".jar"))) {
            Console.err.println("Usage: java …ClassHierarchy supertype filterprefix <JAR file>+")
            Console.err.println("(c) 2013 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
            sys.exit(-1)
        }

        val supertypeName = args(0)
        val filterPrefix = args(1)
        val jars = args.drop(2)

        val classHierarchy = (new ClassHierarchy /: jars)(_ ++ ClassFiles(_).map(_._1))
        val supertype = ObjectType(supertypeName)
        if (classHierarchy.isUnknown(supertype)) {
            Console.err.println(
                "The specified supertype: "+
                    supertypeName+
                    " is not defined in the specified jar(s).")
            sys.exit(-2)
        }
        val subtypes = classHierarchy.subtypes(supertype)
        println(
            "# Class hierarchy for: "+
                supertypeName+
                " limited to subclasses that start with: "+
                filterPrefix)

        var worklist = List((supertype, subtypes))
        while (worklist.nonEmpty) {
            val (supertype, allSubtypes) = worklist.head
            worklist = worklist.tail
            val subtypes = allSubtypes.filter(_.className.startsWith(filterPrefix))
            if (subtypes.nonEmpty) {
                println(supertype.className+" > "+subtypes.map(_.className).mkString(", "))
                for (subtype ← subtypes) {
                    worklist = (subtype, classHierarchy.subtypes(subtype)) :: worklist
                }
            }
        }
        println("# Generated for: "+jars.mkString(", "))
    }
}

