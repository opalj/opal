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
package bugs;

/**
 * Demo code for the issue:
 * "A non-seriablizable class has a serializable inner class". This situation is
 * problematic, because the serialization of the inner class would require – due
 * to the link to its outer class – always the serialization of the outer class
 * which will, however, fail.
 * 
 * @author Michael Eichberg
 */
public class InnerSerializableClass implements java.io.Serializable {

  private static final long serialVersionUID = -1182351106716239966L;

  class SomeInnerClass {

    class InnerInnerClass implements java.io.Serializable {

      private static final long serialVersionUID = 1l;

      public String toString() {

        return InnerSerializableClass.this.toString() + SomeInnerClass.this.toString()
            + this.toString();
      }

    }

    public String toString() {
      return "InnerSerializableClass.InnerClass" + InnerSerializableClass.this.hashCode();
    }

  }

}

class OuterClass {

  static class SomeStaticInnerClass implements java.io.Serializable {
    private static final long serialVersionUID = 2l;

  }

}
