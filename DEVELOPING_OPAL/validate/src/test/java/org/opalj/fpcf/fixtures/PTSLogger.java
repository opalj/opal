package org.opalj.fpcf.fixtures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class PTSLogger {
    static PrintWriter pw;

    static {
        try {
            pw = new PrintWriter(new FileOutputStream("trace.xml", true));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logDefsiteInstance(Object instance, int methodId, int pc) {
        String log = null;
        if (instance == null) {
            log = String.format("<methodId=\"%s\" pc=\"%d\" instanceId=\"null\"/>\n", methodId, pc);
        } else {
            log = String.format("<methodId=\"%s\" pc=\"%d\" instanceId=\"%d\" class=\"%s\"/>\n", methodId, pc, System.identityHashCode(instance), instance.getClass().getName());
        }
        System.out.print(log);
        pw.printf(log);
        pw.flush();

    }
}
