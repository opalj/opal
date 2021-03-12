package org.opalj.fpcf.fixtures.immutability.sandbox36;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

public class Test2 {
  //TODO  @DeepImmutableField("")
    private Object object1 = new Object();
    @ShallowImmutableField("")
    private Object object2 = new Object();
    @ShallowImmutableField("")
    private Mut object3 = new Mut();
    @ShallowImmutableField("")
    private Mut object4 = new Mut();

    @ShallowImmutableField("")
    private Mut mut5 = new Mut();

    @ShallowImmutableField("")
    private Mut mut6 = new Mut();


    public Test2(Mut o){
        this.object2 = o;
    }

    public Mut get3(){
        return object3;
    }

    private Mut get4(){
        return object4;
    }

private Mut get5() {
        return (Mut) id(mut5);
}

private void get6() {
        id(mut6);
}


  private Object id(Object o){
        return o;
  }

}

class Mut{
    /*java.util.concurrent.atomic.AtomicReference.unsafe
    java.util.Locale.GERMANY
    java.util.zip.ZipEntry.name
    java.util.zip.ZipFile.name
    java.util.logging.LogRecord.threadIds
    java.util.concurrent.locks.AbstractQueuedSynchronizer.unsafe
    java.util.WeakHashMap.NULL_KEY
    java.util.TreeSet.PRESENT
    java.util.EnumMap.NULL
    java.util.concurrent.Exchanger.CANCEL
    java.util.logging.LogRecord.threadIds
    java.util.zip.InflaterInputStream.singleByteBuf
    java.util.Currency.mainTable
    java.util.Scanner.boolPattern
    java.util.zip.ZipFile.name
    java.util.regex.Pattern $Loop.body
    ClassFile(
                    public /*SUPER*/ /*sun.util.CoreResourceBundleControl
	extends java.util.ResourceBundle $Control
            [version=49.0]
            )
*/
    public int n = 5;
}