package org.opalj.fpcf.fixtures.immutability.sandbox26;

import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;

final class DependentClassWithGenericFieldWithOneLeftGenericParameter<T> {

    @DependentImmutableField(value = "has one left generic parameter T and no shallow or mutable types",
            analyses = L3FieldImmutabilityAnalysis.class)
    private SimpleGenericClass<T, FinalEmptyClass,FinalEmptyClass> sgc;

    public DependentClassWithGenericFieldWithOneLeftGenericParameter(T t) {
        sgc = new SimpleGenericClass<>(t, new FinalEmptyClass(), new FinalEmptyClass());
    }

}

final class SimpleGenericClass<T1,T2,T3> {

    private T1 t1;

    private T2 t2;

    private T3 t3;

    public SimpleGenericClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }
    /*com.oracle.webservices.internal.api.databinding.DatabindingModeFeature
    com.oracle.webservices.internal.api.databinding.ExternalMetadataFeature
    com.oracle.webservices.internal.api.message.ContentType
    com.oracle.xmlns.internal.webservices.jaxws_databinding.XmlWebServiceRef.lookup
    com.sun.activation.registries.MailcapTokenizer.currentToken
    com.oracle.webservices.internal.api.message.BasePropertySet $PropertyMap.cachedEntries
    com.sun.imageio.plugins.bmp.BMPImageWriter
    com.sun.java.swing.plaf.windows.WindowsFileChooserUI
    com.sun.java.swing.plaf.windows.WindowsIconFactory
    com.sun.nio.zipfs.ZipPath
    com.sun.corba.se.impl.corba.AnyImplHelper.__typeCode
    com.sun.corba.se.impl.corba.TypeCodeImplHelper.__typeCode
    com.sun.corba.se.impl.encoding.CachedCodeBase.bases
    // com.sun.corba.se.impl.encoding.CachedCodeBase.
            com.sun.corba.se.impl.encoding.CachedCodeBase.implementations
    com.sun.corba.se.impl.naming.namingutil.INSURLHandler.insURLHandler
    com.sun.corba.se.impl.oa.toa.TOAFactory.toa
    com.sun.corba.se.impl.orb.ORBSingleton.fullORB
    com.sun.corba.se.impl.protocol.giopmsgheaders.TargetAddressHelper.__typeCode
    com.sun.corba.se.spi.activation.ActivatorHelper.__typeCode
    jdk.nashorn.internal.objects.Global.builtinArrayBuffer
    sun.util.locale.provider.LocaleServiceProviderPool.availableLocales
    sun.util.locale.provider.JRELocaleProviderAdapter.localeData
    sun.text.normalizer.UnicodeSet.INCLUSIONS
    sun.text.normalizer.UBiDiProps.gBdpDummy
    sun.nio.fs.UnixDirectoryStream
    sun.nio.fs.UnixFileAttributes
    com.oracle.net.Sdp $1.val$o
    com.sun.activation.registries.MimeTypeEntry.type
    com.sun.activation.registries.MailcapTokenizer.START_TOKEN*/
    /*javax.management.monitor.CounterMonitor.notifsInfo
    javax.management.monitor.GaugeMonitor.notifsInfo
    javax.management.monitor.StringMonitor.notifsInfo
    com.sun.xml.internal.bind.v2.runtime.property.SingleMapNodeProperty $1.map
    javax.management.monitor.CounterMonitor.notifsInfo
    javax.management.monitor.GaugeMonitor.notifsInfo
    javax.management.monitor.StringMonitor.notifsInfo
    sun.awt.ExtendedKeyCodes.extendedKeyCodesSet
    com.sun.jndi.ldap.Connection.pauseLock
    com.sun.corba.se.impl.util.Utility.CACHE_MISS
    com.sun.xml.internal.bind.v2.model.impl.ElementInfoImpl.adapter
    com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter.SEEN_ELEMENT
    com.sun.xml.internal.ws.resources.PolicyMessages.messageFactory
    com.sun.xml.internal.ws.wsdl.writer.WSDLPatcher.SCHEMA_REDEFINE_QNAME
    com.sun.xml.internal.bind.v2.runtime.NameBuilder.attributeQNameIndexMap
    com.sun.xml.internal.bind.v2.runtime.NameBuilder.attributeQNameIndexMap
    sun.awt.ExtendedKeyCodes.regularKeyCodesMap
    com.sun.xml.internal.bind.v2.model.impl.ElementInfoImpl.adapter
    com.sun.xml.internal.bind.v2.model.impl.ElementInfoImpl.adapter
    com.sun.beans.finder.PropertyEditorFinder.registry
*/
}



final class FinalEmptyClass{}