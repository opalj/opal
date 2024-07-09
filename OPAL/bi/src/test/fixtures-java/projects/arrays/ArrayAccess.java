/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package arrays;

/**
 * This class performs a large number of array operations.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public class ArrayAccess {

  /** Returns a new array with length 0. */
  static Object[] getArray0() {
    return new Object[0];
  }

  /** Returns an empty array of length 100. */
  static Object[] getArray100() {
    return new Object[100];
  }

  /** Returns a new array of length one which stores a single object. */
  static Object[] getArray1WithObject() {
    return new Object[]{new Object()};
  }

  /** Returns a new array of length one which stores a reference to itself in the first element. */
  static Object[] getArray1WithSelfReference() {
    Object[] as = new Object[1];
    as[0] = as;
    return as;
  }

  /** Initializes an int array using the index of the element using one assignment per element. */
  static int[] getArray3DirectlyInitialized() {
    int[] as = new int[3];
    as[0] = 0;
    as[1] = 1;
    as[2] = 2;
    return as;
  }

  /** Initializes an int array using a for loop from 0 to array.length. */
  static int[] getArray10InitializedByLoop() {
    int[] as = new int[10];
    for (int i = 0 ; i < 10 ; i++) {
      as[i] = i;
    };
    return as;
  }

  /** Initializes an int array using a for loop from array.length down to 0. */
  static int[] getArray10InitializedByLoopStartingWithLastIndex() {
    int[] as = new int[10];
    for (int i = 9 ; i >= 0 ; i--) {
      as[i] = i;
    };
    return as;
  }

  /** Initializes a String array using an initializer statement. */
  static String[] getStringArray() {
    return new String[]{"A","B","C","D","E","F","G","H"};
  }


  /** An array where the second element is updated multiple times. */
  static String[] getUpdatedStringArray() {
    String[] as = new String[]{"A","B","C","D","E","F","G","H"};
    as[1] = as[1]+as[1];
    as[1] = as[1]+as[1];
    return as;
  }

  /** A two dimensional array where not all array have the same size. */
  static int[][] get2DIntArray() {
    int[][] as = new int[3][3];
    as[1] = new int[1]; // now the array as is no longer a 3x3 dimensional array...
    return as;
  }

  /** Returns a simple array where an unknown element is updated. */
  static int[] updateArrayValid(int index) {
    if(index < 0 || index > 2) return null;

    int[] as = new int[3];
    as[index] = index;
    return as;
  }

  /** Returns a simple array where an unknown element is updated. */
  static int[] updateArrayValidBasedOnLength(int index,int[] as) {
    if(index >= 0 && index < as.length) {
    as[index] = index;
  }
    return as;
  }

  /** Potentially failing update. */
  static int[] updateArray(int index,int[] as) {
    if(index < 0 ) {
      throw new IllegalArgumentException();
    }
    as[index] = index;
    return as;
  }

  static Object selfReferencingArrays() {
      Object o[] = new Object[1];
      o[0] = o; // the first field of the array has a (cyclic) reference to itself.
      return o;
  }

}
