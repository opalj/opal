/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CatchesIllegalMonitorStateException;

/**
 * Fixed version of MissingSynchronized. The `wait()` and `notifyAll()` calls are done
 * from inside `synchronized()` blocks on the proper object. No reports should be
 * generated here.
 * 
 * @author Daniel Klauer
 */
public class ProperSynchronized {

    private String lock = "some object used as lock";

    Thread thread = new Thread(new Runnable() {

        public void run() {
            synchronized (lock) {
                System.out.println("hello from thread! waking up parent...");
                lock.notifyAll();
            }
        }
    });

    void test() {
        synchronized (lock) {
            System.out.println("starting thread...");
            thread.start();

            System.out.println("waiting for thread to start...");
            try {
                // Without the proper synchronized(lock) block, this will throw a
                // IllegalMonitorStateException.
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("waiting for thread to exit...");
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ProperSynchronized().test();
    }
}
