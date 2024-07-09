/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SyncSetUnsyncGet;

/**
 * A class with several setter/getter pairs. Some don't use synchronized at all, some use
 * synchronized on both setter & getter, and some use synchronized only on the setter, but
 * not the getter.
 * 
 * @author Daniel Klauer
 */
public class VariousSettersAndGetters {

    /**
     * s1 with synchronized setter, but unsynchronized getter (dangerous, this is an issue
     * that FindRealBugs should report)
     */
    private String a = "";

    public synchronized void setA(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }

    /**
     * Same as above, but with different data type.
     */
    private int b;

    public synchronized void setB(int b) {
        this.b = b;
    }

    public int getB() {
        return b;
    }

    /**
     * Here, both setter and getter are synchronized, so there's no issue.
     */
    private int c;

    public synchronized void setC(int c) {
        this.c = c;
    }

    public synchronized int getC() {
        return c;
    }

    /**
     * Here, neither the setter nor the getter is synchronized. This should not be
     * reported.
     */
    private int d;

    public void setD(int d) {
        this.d = d;
    }

    public int getD() {
        return d;
    }

    /**
     * Setter/getter pair where only the getter is synchronized. Currently this won't be
     * reported.
     */
    private int e;

    public void setE(int e) {
        this.e = e;
    }

    public synchronized int getE() {
        return e;
    }
}
