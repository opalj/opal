/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.collection.mutable.AnyRefMap

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.Field

/**
 * This analysis performs a simple abstract interpretation of all methods of a class
 * to identify fields that are always assigned
 * an object that is a subtype of the field's declared type.
 *
 * @note
 * WE IGNORE THOSE FIELDS WHICH SEEMS TO BE ALWAYS NULL.
 *
 * THESE FIELDS ARE OFTEN INITIALZED - AT RUNTIME - BY SOME CODE OUTSIDE
 * THE SCOPE OF "PURE" JAVA BASED ANALYSES.
 *
 * E.G., WE IGNORE THE FOLLOWING FIELDS FROM JAVA 8:
 *  - [BY NAME] java.util.concurrent.FutureTask{ runner:null // Originaltype: java.lang.Thread }
 *  - [BY NAME] java.nio.channels.SelectionKey{ attachment:null // Originaltype: java.lang.Object }
 *  - [BY SETTER] java.lang.System{ err:null // Originaltype: java.io.PrintStream }
 *  - [BY SETTER] java.lang.System{ in:null // Originaltype: java.io.InputStream }
 *  - [BY SETTER] java.lang.System{ out:null // Originaltype: java.io.PrintStream }
 *  - [BY CONSTRUCTOR] java.net.InterfaceAddress{ address:null // Originaltype: java.net.InetAddress }
 *
 * '''[UPDATE BY NATIVE CODE...] sun.nio.ch.sctp.ResultContainer{ value:null // Originaltype: java.lang.Object }'''
 *
 * THE FOLLOWING FIELDS ARE "REALLY" NULL in JAVA 8 (1.8.0 - 1.8.0_25):
 *  - [OK] java.util.TimeZone{ NO_TIMEZONE:null // Originaltype: java.util.TimeZone }
 *  - [OK - DEPRECATED] java.security.SecureRandom{ digest:null // Originaltype: java.security.MessageDigest }
 *
 * The reason/purpose is not 100% clear:
 *  - [OK] javax.swing.JList.AccessibleJList.AccessibleJListChild{ accessibleContext:null }
 *  - [OK] javax.swing.JList.AccessibleJList.AccessibleJListChild{ component:null }
 *  - [OK] com.sun.corba.se.impl.io.IIOPInputStream{ abortIOException:null }
 *  - [OK] com.sun.corba.se.impl.orb.ORBImpl{ codeBaseIOR:null }
 *  - [OK - ACCIDENTIALLY CREATED?] com.sun.org.apache.xpath.internal.jaxp.XPathImpl{ d:null }
 *  - [OK - LEGACY CODE?] javax.swing.JPopupMenu{ margin:null }
 *  - [OK - LEGACY CODE?] sun.audio.AudioDevice{ mixer:null }
 *  - [OK - RESERVED FOR FUTURE USAGE] com.sun.corba.se.impl.corba.ServerRequestImpl{ _ctx:null }
 *  - [OK - LEGACY CODE?] com.sun.java.swing.plaf.motif.MotifPopupMenuUI{ border:null }
 *  - [OK - LEGACY CODE?] com.sun.media.sound.SoftSynthesizer{ testline:null }
 *  - [OK - LEGACY CODE?] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader{ fSymbolTable:null }
 *  - [OK - LEGACY CODE] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl{ _piParams:null }
 *  - [OK - RSERVED FOR FUTURE USAGE?] com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase{ m_namespaceLists:null }
 *  - [OK - LEGACY CODE?] sun.awt.motif.MFontConfiguration{ fontConfig:null }
 *  - [OK - LEGACY CODE?] sun.print.PSStreamPrintJob{ reader:null }
 *
 * @author Michael Eichberg
 */
object FieldValuesAnalysis {

    def doAnalyze(
        theProject:    SomeProject,
        createDomain:  (SomeProject, ClassFile) ⇒ BaseFieldValuesAnalysisDomain,
        isInterrupted: () ⇒ Boolean
    ): FieldValueInformation = {

        val results = new ConcurrentHashMap[Field, Domain#DomainValue]

        theProject.parForeachProjectClassFile(isInterrupted) { classFile ⇒
            // this analysis does not support parallelization at a more
            // fined-grained level, because we reuse the same domain instance
            // to perform an abstract interpretation of all methods of the
            // same class file
            if (!isInterrupted()) {
                val domain = createDomain(theProject, classFile)
                if (domain.hasCandidateFields) {

                    classFile.methods.foreach { method ⇒
                        if (method.body.isDefined) {
                            domain.setMethodContext(method)
                            BaseAI(method, domain)
                        }
                    }

                    val fieldsWithRefinedValues = domain.fieldsWithRefinedValues
                    if (fieldsWithRefinedValues.nonEmpty) {
                        results.putAll(fieldsWithRefinedValues.toMap.asJava)
                    }
                }
            }
        }

        AnyRefMap.empty[Field, Domain#DomainValue] ++ results.asScala
    }

}
