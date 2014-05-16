package apps.flashcards.util;

import java.lang.reflect.Array;

/**
 * A collection of methods related to the manipulation and <i>analysis</i> of arrays.
 * 
 * @version $Revision: 1.2 $ $Date: 2007-06-27 12:11:15 $
 * @author Michael Eichberg
 */
public final class Arrays {

    public static final int[] EMPTY_INT_ARRAY = new int[0];

    private Arrays() {

        super();
    }

    /**
     * Searches for the element <i>t</i> in the unsorted array <code>ts</code>.<i> If
     * <code>ts</code> is sorted use java.util.Arrays.binarySearch instead.</i>
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            the base array in which the given element will be searched for; must not be
     *            <tt>null</tt>.
     * @param t
     *            the element to be searched for (a direct reference comparison is used!)
     * @return the index of the specified element <tt>t</tt> in the array <tt>ts</tt>. If the item
     *         is not found <tt>-1</tt> is returned.
     */
    public static <T> int indexOf(T[] ts, T t) {

        final int MAX = ts.length;
        for (int i = 0; i < MAX; i++) {
            if (ts[i] == t) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Creates a new array with references to all elements of <tt>ts</tt> except of the element
     * <tt>t</tt>.
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            the array which is the basis.
     * @param t
     *            the element which should be removed.
     * @param zeroElementArray
     *            array which will be returned if the resulting array has length zero; can be
     *            <code>Null</code>.
     * @return an new array which contains all elements of <tt>ts</tt> except of the element
     *         <tt>t</tt>.
     * @throws IllegalArgumentException
     *             if <tt>t</tt> is not found in <tt>ts</tt>.
     */
    public static <T> T[] remove(T[] ts, T t, T[] zeroElementArray)
            throws IllegalArgumentException {

        int index = indexOf(ts, t);
        if (index == -1)
            throw new IllegalArgumentException();

        return remove(ts, index, zeroElementArray);
    }

    /**
     * @return true, if the specified array contains the specified element.
     */
    public static <T> boolean hasElement(T[] ts, T t) {

        return indexOf(ts, t) >= 0;
    }

    /**
     * Returns a new array where the item with the specified index is removed from the array
     * <tt>ts</tt>.
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            the base array for the creation of a new array; this array is not changed.
     * @param index
     *            the index; must be valid.
     * @param zeroElementArray
     *            array that will be returned if the size of the newly created array is zero; can be
     *            <tt>null</tt>.
     * @return the newly created array which contains all elements of <tt>ts</tt> without the
     *         element at the given index. If the newly created array has length zero the given
     *         <tt>zeroElementArray</tt> is returned.
     */
    public static <T> T[] remove(T[] ts, int index, T[] zeroElementArray)
            throws ArrayIndexOutOfBoundsException {

        if (ts.length == 1) {
            if (index != 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return zeroElementArray;
        }

        @SuppressWarnings("unchecked")
        T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(),
                ts.length - 1);

        System.arraycopy(ts, 0, newts, 0, index);
        if (index < newts.length) {
            System.arraycopy(ts, index + 1, newts, index, newts.length - index);
        }

        return newts;
    }

    /**
     * Creates a new array consisting of all elements of both arrays. If one array is empty the
     * other array is returned unchanged.
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            array one.
     * @param tn
     *            array two.
     * @return a new array of <code>size = ts.length+tn.length</code> with all elements defined in
     *         <tt>ts</tt> and <tt>tn</tt>.
     */
    public static <T> T[] add(T[] ts, T[] tn) {

        if (ts.length == 0) {
            return tn;
        }
        if (tn.length == 0) {
            return ts;
        }

        @SuppressWarnings("unchecked")
        T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(), ts.length
                + tn.length);

        System.arraycopy(ts, 0, newts, 0, ts.length);
        System.arraycopy(tn, 0, newts, ts.length, tn.length);
        return newts;
    }

    /**
     * Creates a new array with all elements defined in ts and the additional element t.
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            the source array.
     * @param t
     *            the source element.
     */
    public static <T> T[] add(T[] ts, T t) {

        if (ts.length == 0) {
            @SuppressWarnings("unchecked")
            T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(), 1);
            newts[0] = t;
            return newts;
        }

        @SuppressWarnings("unchecked")
        T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(),
                ts.length + 1);

        System.arraycopy(ts, 0, newts, 0, ts.length);
        newts[ts.length] = t;
        return newts;
    }

    /**
     * Creates a new array with all elements of the old array and where the new element is added at
     * the specified index; the index must be in the range [0,ts.length] (inclusively). This method
     * does not check whether the array already contains a reference to the new element or not.
     * 
     * @param <T>
     *            the component type of the array.
     * @param ts
     *            the source array.
     * @param t
     *            the source element.
     * @param index
     *            the index where the element (t) should be added.
     * @return the new array with the new element added at the specified position.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] add(T[] ts, T t, int index) {

        if (index < 0 || index > ts.length)
            throw new ArrayIndexOutOfBoundsException();

        if (ts.length == 0) {
            T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(), 1);
            newts[0] = t;
            return newts;
        }

        T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(),
                ts.length + 1);

        System.arraycopy(ts, 0, newts, 0, index);
        newts[index] = t;
        if (index < ts.length) {
            System.arraycopy(ts, index, newts, index + 1, ts.length - index);
        }
        return newts;
    }

    /**
     * Creates a new array that contains all values of the array ts and the additional value t.
     * 
     * @return An int array with length ts.length+1 where the values [0..ts.length) correspond to
     *         the values in ts and where the value with the index ts.length is t.
     */
    public static <T> T[] append(T[] ts, T t) {

        @SuppressWarnings("unchecked")
        T[] newts = (T[]) Array.newInstance(ts.getClass().getComponentType(),
                ts.length + 1);
        System.arraycopy(ts, 0, newts, 0, ts.length);
        newts[ts.length] = t;
        return newts;
    }

    public static int[] add(int[] ts, int t, int index) {

        if (index < 0 || index > ts.length)
            throw new ArrayIndexOutOfBoundsException(index);

        if (ts.length == 0) {
            int[] newts = new int[1];
            newts[0] = t;
            return newts;
        } else {
            int[] newts = new int[ts.length + 1];

            System.arraycopy(ts, 0, newts, 0, index);
            newts[index] = t;
            if (index < ts.length) {
                System.arraycopy(ts, index, newts, index + 1, ts.length - index);
            }
            return newts;
        }
    }

    /**
     * Creates a new array that contains all values of the array ts and the additional value t.
     * 
     * @return An int array with length ts.length+1 where the values [0..ts.length) correspond to
     *         the values in ts and where the value with the index ts.length is t.
     */
    public static int[] append(int[] ts, int t) {

        int[] newts = java.util.Arrays.copyOf(ts, ts.length + 1);
        System.arraycopy(ts, 0, newts, 0, ts.length);
        newts[ts.length] = t;
        return newts;
    }

    /**
     * Returns a new array where the item with the specified index is removed from the array
     * <tt>ts</tt>.
     * 
     * @param ts
     *            the base array for the creation of a new array; this array is not changed.
     * @param index
     *            the index; must be valid.
     * @return the newly created array which contains all elements of <tt>ts</tt> without the
     *         element at the given index. If the newly created array has length zero the
     *         <tt>EMPTY_INT_ARRAY</tt> is returned.
     */
    public static int[] remove(int[] ts, int index) throws ArrayIndexOutOfBoundsException {

        if (ts.length == 1) {
            if (index != 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return EMPTY_INT_ARRAY;
        }

        int[] newts = new int[ts.length - 1];

        System.arraycopy(ts, 0, newts, 0, index);
        if (index < newts.length) {
            System.arraycopy(ts, index + 1, newts, index, newts.length - index);
        }

        return newts;
    }
}
