/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CatchesIllegalMonitorStateException;

/**
 * This is an example class which attempts to communicate with a thread using `wait()` and
 * `notifyAll()`, but without using `synchronized()` blocks.
 * 
 * This causes `IllegalMonitorStateException`s to be thrown at runtime, which are handled
 * in try/catch blocks here to prevent the program from terminating which is what someone
 * who does not know about this issue might do. Instead of doing this, the code that leads
 * to this exception being thrown should be fixed.
 * 
 * @author Daniel Klauer
 */
public class MissingSynchronized {

    private String lock = "some object used as lock";

    Thread thread = new Thread(new Runnable() {

        public void run() {
            System.out.println("hello from thread! waking up parent...");
            try {
                // Without the proper synchronized(lock) block, this will throw a
                // IllegalMonitorStateException.
                lock.notifyAll();
            } catch (IllegalMonitorStateException e) {
                // Wrongly catch the IllegalMonitorStateException, and potentially even
                // ignore it, instead of fixing the bug above.
                System.out.println("IllegalMonitorStateException caught...");
            }
        }
    });

    void test() {
        System.out.println("starting thread...");
        thread.start();

        System.out.println("waiting for thread to start...");
        try {
            // Without the proper synchronized(lock) block, this will throw a
            // IllegalMonitorStateException.
            lock.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalMonitorStateException e) {
            // Wrongly catch the IllegalMonitorStateException, and potentially even
            // ignore it, instead of fixing the bug above.
            System.out.println("IllegalMonitorStateException caught...");
        }

        System.out.println("waiting for thread to exit...");
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MissingSynchronized().test();
    }
}
