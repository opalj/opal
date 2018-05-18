package org.opalj.fpcf.properties.purity;

public class DomainSpecific {
    // WHEN CHANGING THESE MAKE SURE THE ANNOTED TEST CLASSES ARE RECOMPILED, THIS DOES NOT HAPPEN
    // AUTOMATICALLY!
    public final static String RaisesExceptions = "raises exceptions";
    public final static String UsesSystemOutOrErr = "uses System.out or System.err";
    public final static String UsesLogging = "uses logging";
}
