package org.opalj.fpcf.fixtures.immutability.sandbox40;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;

public class NativeFunctionCalls {

    @MutableField("concrete class type is deep immutable")
    @AssignableFieldReference("field is final")
    private Object finalObjectField = new Object();

    native void hulului();
/*
    com.sun.media.sound.RIFFReader.fourcc
    javax.imageio.stream.FileImageInputStream.disposerReferent

    javax.crypto.SecretKeyFactory.lock

    sun.nio.ch.AsynchronousSocketChannelImpl.writeLock

    com.sun.corba.se.impl.orbutil.threadpool.ThreadPoolImpl.workersLock

    com.sun.xml.internal.ws.server.WSEndpointImpl.managedObjectManagerLock*/
}