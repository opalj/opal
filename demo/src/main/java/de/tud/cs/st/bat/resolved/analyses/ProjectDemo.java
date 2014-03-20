/* License (BSD Style License):
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
package de.tud.cs.st.bat.resolved.analyses;

import java.io.File;

import scala.collection.JavaConversions;
import de.tud.cs.st.bat.resolved.ClassFile;
import de.tud.cs.st.bat.resolved.Method;
import de.tud.cs.st.bat.resolved.ai.AIResult;
import de.tud.cs.st.bat.resolved.ai.BaseAI;
import de.tud.cs.st.bat.resolved.ai.debug.XHTML;
import de.tud.cs.st.bat.resolved.ai.domain.l0.BaseDomain;

/**
 * Demonstrates how to create and access a <code>Project</code> using Java.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("unchecked")
public class ProjectDemo {

  public static void main(String[] args) {
    // Load a project
    ProjectLike<java.net.URL> project = ProjectLike.createProject(new File(args[0]));

    // Convert the project into a simple Map
    // Map<ObjectType, ClassFile> project = projectLike.toJavaMap();

    // Create an abstract interpreter (can be reused)
    BaseAI ai = new BaseAI();

    // Do something with it...
    System.out.println("The project contains:");
    for (ClassFile classFile : JavaConversions.asJavaIterable(project.classFiles())) {
      System.out.println(" - " + classFile.thisType().toJava());

      Iterable<Method> methods = JavaConversions
          .asJavaIterable((scala.collection.Iterable<Method>) classFile.methods());
      for (Method method : methods) {
        if (method.body().isDefined()) {
          AIResult result = ai.apply(classFile, method, new BaseDomain());
          System.out.println(XHTML.dump(classFile, method, result, "Abstract Interpretation Succeeded"));
        }
      }
    }
  }
}
