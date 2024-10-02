/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.threads;

import org.opalj.fpcf.properties.callgraph.DirectCall;
import org.opalj.fpcf.properties.callgraph.VMReachable;

public class CreateThreadGroup {
    @DirectCall(
            name = "run",
            line = 15,
            resolvedTargets = {"Lorg/opalj/fpcf/fixtures/threads/MyRunnable;"})
    public static void main(String[] args) {
        ThreadGroup myThreadGroup = new MyThreadGroup();
        Thread testThread = new Thread(myThreadGroup, new MyRunnable());
        testThread.start();
    }
}

class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Hello");
    }
}

class MyThreadGroup extends ThreadGroup {

    public MyThreadGroup() {
        super("mythreadgroup");
    }

    @VMReachable
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        super.uncaughtException(t, e);
    }
}