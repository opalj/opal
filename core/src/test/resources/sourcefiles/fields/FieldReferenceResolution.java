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
package fields;

@SuppressWarnings("all")
public class FieldReferenceResolution {

  static class Super {

    public int x = 0;

    public int y = 0;

    public int z = 0;

    public String toString() {
      return String.valueOf(x);
    }
  }

  static interface I {
    int y = -1;
  }

  static class Sub extends Super implements I {

    public int x = 1;
    
    /*
      public java.lang.String toString(); [FILTERED]
     
      4  ldc <String "super.x="> [24]
      6  invokespecial java.lang.StringBuilder(java.lang.String) [26]
      9  aload_0 [this]
     10  invokespecial fields.FieldReferenceResolution$Super.toString() : java.lang.String [29]
     
     21  ldc <String "sub.x="> [37]
     26  aload_0 [this]
     27  getfield fields.FieldReferenceResolution$Sub.x : int [14]
     
     38  ldc <String "((Super)this).y="> [42]
     43  aload_0 [this]
     44  getfield fields.FieldReferenceResolution$Super.y : int [44]
     
     55  ldc <String "super.y="> [47]
     60  aload_0 [this]
     61  getfield fields.FieldReferenceResolution$Super.y : int [44]
     
     72  ldc <String "((I)this).y="> [49]
     77  iconst_m1
     
     86  ldc <String "this.z="> [51]
     91  aload_0 [this]
     92  getfield fields.FieldReferenceResolution$Sub.z : int [53] // <= HERE, we need to resolve the reference!
     */
    public String toString() {
      return "super.x=" + super.toString()/* super.x */+ "; " +
          "sub.x=" + this.x + "; " + // => super.x=0; sub.x=1
          "((Super)this).y=" + ((Super) this).y + "; " +
          "super.y=" + super.y + "; " +
          "((I)this).y=" + ((I) this).y + "; " +
          "this.z=" + this.z;
      // <=> super.x=0; sub.x=1; ((Super)this).y=0; super.y=0; ((I)this).y=-1; this.z=0
    }
  }

  public static void main(String[] args) {
    Sub s = new FieldReferenceResolution.Sub();
    System.out.println(s.toString());
  }

}
