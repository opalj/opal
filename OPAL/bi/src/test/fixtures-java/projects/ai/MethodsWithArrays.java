/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Methods that create, initialize and update arrays.
 *
 * @author Michael Eichberg
 * @author Christos Votskos
 */
public class MethodsWithArrays {

    public static byte[] simpleByteArrayInitializationWithLength4() {
        byte newByteArray4Elements[] = { 1, 2, 3, 4 };

        return newByteArray4Elements;
    }

    public static short[] simpleShortArrayInitializationWithLength4() {
        short newShortArray4Elements[] = { 1, 2, 3, 4 };

        return newShortArray4Elements;
    }

    public static int[] simpleIntArrayInitializationWithLength4() {
        int newIntArray4Elements[] = { 1, 2, 3, 4 };

        return newIntArray4Elements;
    }

    public static long[] simpleLongArrayInitializationWithLength4() {
        long newLongArray4Elements[] = { 1l, 2l, 3l, 4l };

        return newLongArray4Elements;
    }

    public static float[] simpleFloatArrayInitializationWithLength4() {
        float newFloatArray4Elements[] = { 1.0f, 2.0f, 3.0f, 4.0f };

        return newFloatArray4Elements;
    }

    public static double[] simpleDoubleArrayInitializationWithLength4() {
        double newDoubleArray4Elements[] = { 1.0d, 2.0d, 3.0d, 4.0d };

        return newDoubleArray4Elements;
    }

    public static boolean[] simpleBooleanArrayInitializationWithLength4() {
        boolean newBooleanArray4Elements[] = { true, false, true, false };

        return newBooleanArray4Elements;
    }

    public static char[] simpleCharArrayInitializationWithLength4() {
        char newCharArray4Elements[] = { 'A', 'B', 'C', 'D' };

        return newCharArray4Elements;
    }

    public static String[] simpleStringArrayInitializationWithLength4() {
        String newStringArray4Elements[] = { "A1", "B2", "C3", "D4" };

        return newStringArray4Elements;
    }

    public static Object[] simpleObjectArrayInitializationWithLength4() {
        Object newObjectArray4Elements[] = { new Object(), new Object(), new Object(),
                new Object() };

        return newObjectArray4Elements;
    }

    public static Object[] differentTypesInOneArrayInitialization() {
        Object newDifferentTypesInArray[] = { 1, 2.0f, 3.0d, true, 'A' };

        return newDifferentTypesInArray;
    }

    public static Object[] setArrayNull(Object array[]) {
        if (array != null) {
            array = null;
        }

        return array;
    }

    public static AbstractBase[] branchInits(boolean initAsPrimitiveInt) {
        AbstractBase array[] = null;

        if (initAsPrimitiveInt) {
            array = new ConcreteBase[4];
            array[0] = new ConcreteBase("initAsPrimitiveInt_true", 1);
            array[1] = new ConcreteBase("initAsPrimitiveInt_true", 2);
            array[2] = new ConcreteBase("initAsPrimitiveInt_true", 3);
            array[3] = new ConcreteBase("initAsPrimitiveInt_true", 4);
        } else {
            array = new ConcreteBase[2];
            array[0] = new ConcreteBase("initAsPrimitiveInt_true", 1);
            array[1] = new ConcreteBase("initAsPrimitiveInt_true", 1);
        }

        return array;
    }

    public static AbstractBase[] branchInitsWithSwitch(char v) {
        AbstractBase array[] = null;

        switch (v) {
        case 'A':
            array = new ConcreteBase[3];
            array[0] = new ConcreteBase("A", 1);
            array[1] = new ConcreteBase("A", 10);
            ;
            array[2] = new ConcreteBase("A", 100);
            ;
            break;

        case 'B':
            int arrayLength = 4;
            array = new ConcreteBase[arrayLength];
            for (byte i = 0; i < arrayLength; i++) {
                array[i] = new ConcreteBase("B", i);
                ;
            }
            break;
        case 'C':
            array = new ConcreteBase[2];
            array[0] = new ConcreteBase("C", 1451456);
            array[1] = array[0];
            break;

        case 'E':
            array = new ConcreteBase[1];
            array[0] = new ConcreteBase("E", 4);
            break;

        default:
            array = new ConcreteBase[0];
            break;
        }

        return array;
    }

    public static int[] setValInBranch(boolean v) {
        int array[] = new int[2];

        if (v) {
            array[1] = 1;
        } else {
            array[1] = 2;
        }

        return array;
    }

    public static int[] setValInForAndBranch(boolean v) {
        byte arraylength = 4;
        int array[] = new int[arraylength];

        for (byte i = 0; i < arraylength; i++) {
            if (v) {
                int mod = i % 2;
                array[mod] = 4;
            } else {
                array[i] = i;
            }
        }

        return array;
    }

    public static byte byteArrays(byte[] values) {
        int length = values.length;
        values[length / 2] = 10;
        return values[length - 1];
    }

    public static boolean booleanArrays(boolean[] values) {
        int length = values.length;
        values[length / 2] = false;
        return values[length - 1];
    }

    public static Object covariantArrays(boolean b) {
        Object[] foo = null;

        if (b)
            foo = new java.util.List[1];
        else
            foo = new java.util.Set[2];

        Object o = foo[0];

        return o;
    }

    public static Object referenceToMultipleArrays(boolean b) {
        Object[] foo = null;
        Object[] bar = null;

        if (b){
            foo = new Object[1];
            foo[0] = new Object();
            bar = new Object[5];
            bar[0] = foo;
        }else {
            foo = new Object[1];
            foo[0] = "Text";
            bar = new Object[1];
            bar[0] = foo;
        }

        Object o = foo[0];
        processIt(bar[0]); // no exception
        processIt(bar[1]); // potentially an exception
        processIt(bar[10]); // definitively an exception

        return o; // dead...
    }

    public static Object integerArraysFrenzy(int id, Object data) {
        // Inspiration: java/awt/image/DirectColorModel Object
        // getDataElements(int,Object)
        int intpixel[] = null;
        if (id == 3 && data != null) {
            intpixel = (int[]) data;
        } else {
            intpixel = new int[1];
        }

        switch (id) {
        case 1: {
            byte bdata[];
            if (data == null) {
                bdata = new byte[1];
            } else {
                bdata = (byte[]) data;
            }
            bdata[0] = (byte) (/* 0xff & */intpixel[0]);
            return bdata;
        }
        case 3:
            return intpixel;
        }

        throw new UnsupportedOperationException("What the heck is B?");

    }

    //
    // COMPARISON

    public static Object[] wrap(java.io.Serializable o) {
        if (o == null)
            return new Object[0];
        else
            return new java.io.Serializable[] { o };
    }

    public static boolean instanceofAndArrays(java.io.Serializable o) {
        Object result = wrap(o);
        if (result instanceof java.io.Serializable[]) {
            return true;
        }
        if (result instanceof Object[]) {
            return false;
        } else {
            throw new RuntimeException();
        }
    }

    public static int[] zeroToNine() {
        int[] a = new int[10];
        int i = 0;
        while (i < 10) {
            a[i] = i;
            i = i + 1;
        }
        return a;
    }

    public static int[] oneElement() {
        int[] a = new int[1];
        for (int i = 0; i < 1; i++)
            a[i] = i + 1;
        return a;
    }

    public static int[] threeElements() {
        int[] a = new int[3];
        for (int i = 0; i < 3; i++)
            a[i] = i + 1;
        return a;
    }

    public static int[] arrayIndexOutOfBounds(int i) {
        int[] a = new int[3];
        a[4] = i + 1;
        return a;
    }

    public static int[] arrayIndexOutOfBoundsInLoop() {
        int[] a = new int[3];
        for (int i = 0; i < 4; i++)
            a[i] = i + 1;
        return a;
    }

    public static int[] simpleSelectiveInitialization(int initial) {
        int[] a = new int[1];
        if (initial <= 5) {
            a[0] = initial;
        } else {
            a[0] = -1;
        }

        return a;
    }

    public static int[] selectiveInitialization(int initial) {
        if (initial < 0)
            throw new IllegalArgumentException();

        int[] a = new int[4];
        if (initial <= 2) {
            int i = 0;
            while (i < 2) {
                a[i] = i;
                i = i + 1;
            }
        } else {
            int i = 2;
            while (i < 4) {
                a[i] = i;
                i = i + 1;
            }
        }

        return a;
    }

    public static Object[] pathsConverge(Object[] a) {
        Object[] theArray = a;

        if (theArray == null)
            theArray = new java.io.Serializable[1];
        else if (!(theArray instanceof java.io.Serializable[]))
            theArray = new java.io.Serializable[1];
        return a;
    }

    public static void main(String[] args) {
        final java.io.PrintStream out = System.out;

        Object o = new java.util.ArrayList[0];
        Object s = new java.io.Serializable[10][5];

        out.println(o instanceof java.io.Serializable);
        out.println(o instanceof java.lang.Cloneable);

        out.println(o instanceof Object[]); // true
        out.println(o instanceof java.io.Serializable[]); // true
        out.println(o instanceof java.util.List[]); // true

        out.println(s instanceof Object[]);
        out.println(s instanceof java.io.Serializable[]);
        out.println(s instanceof java.lang.Cloneable[]);

        out.println(o instanceof java.util.Set[]); // false
        out.println(o instanceof int[]);// false
    }

    private static void processIt(Object o) {
        /* just a pseudo method */
    }

    public static void a3DimensionalArray(boolean b) {
        int[][][] is = new int[2][2][2];
        if (b) {
            is[1] = new int[3][]; // now we just know that the outer array is of size 2
            processIt(is); // PC: 20
        } else {
            is[0] = new int[2][2]; // the basic structure remains intact
            processIt(is); // PC: 36
        }
        processIt(is); // PC: 40
    }

    public static void a3DimensionalArrayWithPotentialExceptions(boolean b, int index) {
        int[][][] is = new int[2][2][2];
        if (b) {
            try {
                is[index] = new int[3][]; // now we just know that the outer array is of
                                          // size
                // 2
                processIt(is);
            } catch (Throwable t) {
                processIt(is); // "is" is unchanged!
            } finally {
                processIt(is); // "is" is unchanged if we are on an exception path
            }
        } else {
            is[0] = new int[2][2]; // the basic structure remains intact
            processIt(is);
        }
        processIt(is);
    }

    public static void a2DimensionalArray(boolean b) {
        int[][] is = new int[2][2];
        if (b) {
            is[1] = new int[3]; // now the array is no longer a 2x2 array
            processIt(is);
        } else {
            is[0] = new int[2]; // the basic structure remains intact
            processIt(is);
        }
        processIt(is);
    }

    public static void a4DimensionalArray(boolean b) {
        int[][][][] is = new int[2][3][4][5];
        if (b) {
            is[1] = new int[3][6][9]; // now the array is no longer a 2x3x4x5 array
            processIt(is);
        } else {
            is[0] = new int[3][4][5]; // the basic structure remains intact
            processIt(is);
        }
        processIt(is);
    }

    public static void joinOf2DimensionalArrays(boolean b) {
        int[][] start = new int[2][2];
        int[][] is = null;
        if (b) {
            is = start;
        } else {
            is = new int[2][2]; // the basic structure remains intact
        }
        is[1] = new int[4]; // this may effect both: "start" or the array created in
                            // the else branch
        processIt(start); // we "just" know that start's first dimension is "2"
        processIt(is); // we "just" know that is's first dimension is "2"
    }

    public static Object[] arrayStoreException(boolean v) {
        Object[] array = new Cloneable[1];

        if (v) {
            array[0] = new java.io.Serializable() {

                private static final long serialVersionUID = 1L;
            };
        }

        return array;
    }

}
