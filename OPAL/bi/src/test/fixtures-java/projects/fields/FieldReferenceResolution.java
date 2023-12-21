/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package fields;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
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
