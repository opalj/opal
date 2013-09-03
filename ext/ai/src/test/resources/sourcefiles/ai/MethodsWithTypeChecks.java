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
package ai;

import java.io.File;
import java.io.IOException;

/**
 * Just a very large number of methods that does something related to type
 * checking.
 * 
 * <h2>NOTE</h2> This class is not meant to be (automatically) recompiled; it
 * just serves documentation purposes. The compiled class that is used by the
 * tests is found in the test-classfiles directory.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithTypeChecks {

  public static java.util.List<Integer> get() {
    if (System.currentTimeMillis() > 0)
      return null;
    else
      return new java.util.ArrayList<Integer>();
  }

  public static void main(String[] args) {

    java.util.List<Integer> l = get(); // <= effectively always "null"

    System.out.println(l instanceof java.util.List);
    System.out.println(null instanceof Object);
    System.out.println(l instanceof Object);
    System.out.println(l instanceof java.util.Set<?>);
    System.out.println(l instanceof File);

    Object o = l;

    IOException ioe = (IOException) o;
    System.out.println(ioe);
    System.out.println(ioe instanceof IOException);

    System.out.println("End of type frenzy.");
  }
}
