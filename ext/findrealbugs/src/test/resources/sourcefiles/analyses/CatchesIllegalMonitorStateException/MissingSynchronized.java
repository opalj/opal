/* License (BSD Style License):
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
