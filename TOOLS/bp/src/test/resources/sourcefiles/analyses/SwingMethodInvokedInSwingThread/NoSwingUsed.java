/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SwingMethodInvokedInSwingThread;

/**
 * A class that does not use javax/swing functionality at all should not cause any issues
 * for the analysis.
 * 
 * @author Roberts Kolosovs
 */
public class NoSwingUsed {

    public int randomInt;

    private String randomString;

    public void setRandomString(String v) {
        randomString = v;
    }

    public String getRandomString() {
        return randomString;
    }
}
