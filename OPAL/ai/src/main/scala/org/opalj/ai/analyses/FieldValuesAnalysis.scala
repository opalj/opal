/* BSD 2-Clause License:
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
package org.opalj
package ai
package analyses

import org.opalj.concurrent.OPALExecutionContextTaskSupport
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile

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
        theProject: SomeProject,
        createDomain: (SomeProject, ClassFile) ⇒ BaseFieldValuesAnalysisDomain,
        isInterrupted: () ⇒ Boolean) = {
        import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

        val refinedFieldTypes = {
            val parClassFiles = theProject.classFiles.par
            parClassFiles.tasksupport = OPALExecutionContextTaskSupport
            for {
                classFile ← parClassFiles
                if !isInterrupted()
                // this analysis does not support parallelization at a more
                // fined-grained level, because we reuse the same domain instance
                // to perform an abstract interpretation of all methods of the
                // same class file
                domain = createDomain(theProject, classFile)
                if domain.hasCandidateFields
            } yield {
                classFile.methods.foreach { method ⇒
                    if (method.body.isDefined) {
                        domain.setMethodContext(method)
                        BaseAI(classFile, method, domain)
                    }
                }
                domain.fieldsWithRefinedValues
            }
        }
        refinedFieldTypes.flatten.seq.toMap
    }

}

