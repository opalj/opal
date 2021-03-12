package org.opalj.fpcf.fixtures.immutability.sandbox21;


import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;

public class Test {

    @LazyInitializedNotThreadSafeFieldReference("")
private Object o = null;
    public void getO() {
        if (this.o == null) {
            this.o = new Object();
        }
        //javax.swing.text.DefaultEditorKit.defaultActions
        //java.net.InetAddress.serialPersistentFields
        //java.beans.PropertyChangeSupport.serialPersistentFields
        //com.sun.corba.se.impl.io.ObjectStreamClass.noArgsList
        //com.sun.java.swing.plaf.gtk.GTKColorType.HLS_COLORS
        //com.sun.xml.internal.bind.v2.ClassFactory.emptyClass
        //com.sun.beans.editors.EnumEditor.tags
        //com.sun.beans.finder.InstanceFinder.EMPTY
        //com.sun.crypto.provider.AESCrypt.S
        /*com.sun.beans.finder.InstanceFinder.EMPTY
        com.sun.jmx.mbeanserver.ClassLoaderRepositorySupport.EMPTY_LOADER_ARRAY
        com.sun.jndi.ldap.LdapDnsProviderService.LOCK
        sun.text.normalizer.UBiDiProps.jgArray
        sun.util.calendar.ZoneInfoFile.ruleArray
        sun.util.calendar.ZoneInfoFile.regions
        sun.nio.cs.ext.SJIS_0213 $Decoder.cc
        com.sun.media.sound.SoftChannel.controller */
    }
}
