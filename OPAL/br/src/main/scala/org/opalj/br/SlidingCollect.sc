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
import scala.collection.parallel.ParSeq

import org.opalj._

import org.opalj.util.PerformanceEvaluation._

import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework.ClassFiles

object SlidingCollect {

    val project = ClassFiles(new java.io.File("/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre/lib"))
                                                  //> project  : Seq[(org.opalj.br.reader.Java8Framework.ClassFile, java.net.URL)
                                                  //| ] = ArraySeq((ClassFile(
                                                  //| 	public [SUPER] sun.awt.motif.X11KSC5601
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11KSC5601.class), (ClassFile(
                                                  //| 	[SUPER] sun.awt.motif.X11GBK$Encoder
                                                  //| 	extends sun.nio.cs.ext.DoubleByte$Encoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11GBK$Encoder.class), (ClassFile(
                                                  //| 	[SUPER] sun.awt.motif.X11GB2312$Encoder
                                                  //| 	extends java.nio.charset.CharsetEncoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11GB2312$Encoder.class), (ClassFile(
                                                  //| 	[SUPER] sun.awt.motif.X11KSC5601$Encoder
                                                  //| 	extends java.nio.charset.CharsetEncoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11KSC5601$Encoder.class), (ClassFile(
                                                  //| 	[SUPER] sun.awt.motif.X11KSC5601$Decoder
                                                  //| 	extends java.nio.charset.CharsetDecoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11KSC5601$Decoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.awt.motif.X11GB2312
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11GB2312.class), (ClassFile(
                                                  //| 	[SUPER] sun.awt.motif.X11GB2312$Decoder
                                                  //| 	extends java.nio.charset.CharsetDecoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11GB2312$Decoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.awt.motif.X11GBK
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/motif/X11GBK.class), (ClassFile(
                                                  //| 	public [SUPER] sun.awt.HKSCS
                                                  //| 	extends sun.nio.cs.ext.MS950_HKSCS_XP
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/awt/HKSCS.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.TIS_620
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/TIS_620.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.SJIS_0213
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SJIS_0213.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.SJIS_0213$Encoder
                                                  //| 	extends java.nio.charset.CharsetEncoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SJIS_0213$Encoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.SJIS_0213$Decoder
                                                  //| 	extends java.nio.charset.CharsetDecoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SJIS_0213$Decoder.class), (ClassFile(
                                                  //| 	final [SUPER] sun.nio.cs.ext.SJIS_0213$1
                                                  //| 	extends java.lang.Object
                                                  //| 	ObjectType(java/security/PrivilegedAction)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SJIS_0213$1.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.SJIS
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SJIS.class), (ClassFile(
                                                  //| 	public [SUPER] abstract sun.nio.cs.ext.SimpleEUCEncoder
                                                  //| 	extends java.nio.charset.CharsetEncoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/SimpleEUCEncoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.PCK
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/PCK.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MSISO2022JP
                                                  //| 	extends sun.nio.cs.ext.ISO2022_JP
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MSISO2022JP.class), (ClassFile(
                                                  //| 	[SUPER] sun.nio.cs.ext.MSISO2022JP$CoderHolder
                                                  //| 	extends java.lang.Object
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MSISO2022JP$CoderHolder.class), (ClassFil
                                                  //| e(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS950_HKSCS_XP
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS_XP.class), (ClassFile(
                                                  //| 	[SUPER] sun.nio.cs.ext.MS950_HKSCS_XP$Encoder
                                                  //| 	extends sun.nio.cs.ext.HKSCS$Encoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS_XP$Encoder.class), (ClassFile
                                                  //| (
                                                  //| 	[SUPER] sun.nio.cs.ext.MS950_HKSCS_XP$Decoder
                                                  //| 	extends sun.nio.cs.ext.HKSCS$Decoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS_XP$Decoder.class), (ClassFile
                                                  //| (
                                                  //| 	[SUPER] [SYNTHETIC] sun.nio.cs.ext.MS950_HKSCS_XP$1
                                                  //| 	extends java.lang.Object
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS_XP$1.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS950_HKSCS
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS.class), (ClassFile(
                                                  //| 	[SUPER] sun.nio.cs.ext.MS950_HKSCS$Encoder
                                                  //| 	extends sun.nio.cs.ext.HKSCS$Encoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS$Encoder.class), (ClassFile(
                                                  //| 	[SUPER] sun.nio.cs.ext.MS950_HKSCS$Decoder
                                                  //| 	extends sun.nio.cs.ext.HKSCS$Decoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS$Decoder.class), (ClassFile(
                                                  //| 	[SUPER] [SYNTHETIC] sun.nio.cs.ext.MS950_HKSCS$1
                                                  //| 	extends java.lang.Object
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950_HKSCS$1.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS950
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS950.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS949
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS949.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS936
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS936.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS932_0213
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS932_0213.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS932_0213$Encoder
                                                  //| 	extends sun.nio.cs.ext.SJIS_0213$Encoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS932_0213$Encoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS932_0213$Decoder
                                                  //| 	extends sun.nio.cs.ext.SJIS_0213$Decoder
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS932_0213$Decoder.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS932
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS932.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS874
                                                  //| 	extends java.nio.charset.Charset
                                                  //| 	ObjectType(sun/nio/cs/HistoricallyNamedCharset)
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre
                                                  //| /lib/charsets.jar!/sun/nio/cs/ext/MS874.class), (ClassFile(
                                                  //| 	public [SUPER] sun.nio.cs.ext.MS50221
                                                  //| 	extends sun.nio.cs.ext.MS50220
                                                  //| 	{version=52.0}
                                                  //| ),jar:file:/Library/Java/JavaVirtualMac
                                                  //| Output exceeds cutoff limit.
    project.size                                  //> res0: Int = 32472

    /*
    def pcsBeforePullRequest =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                Seq((_, INVOKESPECIAL(receiver1, _, MethodDescriptor(Seq(paramType), _))),
                    (pc, INVOKEVIRTUAL(receiver2, name, MethodDescriptor(Seq(), returnType)))) ← body.associateWithIndex.sliding(2)
                if (!paramType.isReferenceType &&
                    receiver1.asObjectType.fqn.startsWith("java/lang") &&
                    receiver1 == receiver2 &&
                    name.endsWith("Value") &&
                    returnType != paramType // coercion to another type performed
                )
            } yield ((classFile.fqn, method.toJava, pc))
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsBeforePullRequest

    
    def pcsAfterPullRequest =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2)({
                    case (pc,
                        Seq(INVOKESPECIAL(receiver1, _, MethodDescriptor(Seq(paramType), _)),
                            INVOKEVIRTUAL(receiver2, name, MethodDescriptor(Seq(), returnType)))) if (
                        !paramType.isReferenceType &&
                        receiver1.asObjectType.fqn.startsWith("java/lang") &&
                        receiver1 == receiver2 &&
                        name.endsWith("Value") &&
                        returnType != paramType // coercion to another type performed
                    ) ⇒ pc
                })
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsAfterPullRequest

    def pcsWithNewMethodDescriptorMatcher =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2) {
                    case (
                        pc,
                        Seq(INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType: BaseType, _))),
                            INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType)))) if (
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        receiver1.asObjectType.fqn.startsWith("java/lang/") &&
                        name.endsWith("Value")
                    ) ⇒ pc
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodDescriptorMatcher
    */

    /*
    def pcsWithNewMethodDescriptorMatcherAndSet =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                "java/lang/Boolean",
                "java/lang/Byte",
                "java/lang/Character",
                "java/lang/Short",
                "java/lang/Integer",
                "java/lang/Long",
                "java/lang/Float",
                "java/lang/Double")
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2) {
                    case (
                        pc,
                        Seq(INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType, _))),
                            INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType)))) if (
                        paramType.isBaseType && // implicitly: returnType.isBaseType &&
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        theTypes.contains(receiver1.fqn) &&
                        theMethods.contains(name)
                    ) ⇒ pc
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodDescriptorMatcherAndSet

    def pcsWithNewMethodDescriptorMatcherAndSetAndFindSequence =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                "java/lang/Boolean",
                "java/lang/Byte",
                "java/lang/Character",
                "java/lang/Short",
                "java/lang/Integer",
                "java/lang/Long",
                "java/lang/Float",
                "java/lang/Double")
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                (pc, _) ← body.findSequence(2) {
                    case (
                        Seq(
                            INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType: BaseType, _))),
                            INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType)))
                        ) if (
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        theTypes.contains(receiver1.fqn) &&
                        theMethods.contains(name)
                    ) ⇒ ()
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodDescriptorMatcherAndSetAndFindSequence

    def pcsWithNewMethodDescriptorMatcherAndSetAndFindPair =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                "java/lang/Boolean",
                "java/lang/Byte",
                "java/lang/Character",
                "java/lang/Short",
                "java/lang/Integer",
                "java/lang/Long",
                "java/lang/Float",
                "java/lang/Double")
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                (pc, _) ← body.collectPair {
                    case (
                        INVOKESPECIAL(receiver1, _, TheArgument(paramType: BaseType)),
                        INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
                        ) if (
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        theTypes.contains(receiver1.fqn) &&
                        theMethods.contains(name)) ⇒ ()
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodDescriptorMatcherAndSetAndFindPair
*/
    def pcsWithNewMethodDescriptorMatcherAndSetAndMatchPair =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                ObjectType("java/lang/Boolean"),
                ObjectType("java/lang/Byte"),
                ObjectType("java/lang/Character"),
                ObjectType("java/lang/Short"),
                ObjectType("java/lang/Integer"),
                ObjectType("java/lang/Long"),
                ObjectType("java/lang/Float"),
                ObjectType("java/lang/Double"))
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")

            for {
                classFile ← project.par.map(_._1)
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.matchPair({
                    case (
                        INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)),
                        INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
                        ) ⇒ {
                        (receiver1 eq receiver2) &&
                            (returnType ne parameterType) && // coercion to another type performed
                            theTypes.contains(receiver1) &&
                            theMethods.contains(name)
                    }
                    case _ ⇒ false
                })
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }                                         //> pcsWithNewMethodDescriptorMatcherAndSetAndMatchPair: => scala.collection.p
                                                  //| arallel.ParSeq[(String, String, org.opalj.PC)]
    pcsWithNewMethodDescriptorMatcherAndSetAndMatchPair
                                                  //> Avg: 0,0597; T: 0,0597; Ts: 0,0597
                                                  //| Avg: 0,0500; T: 0,0500; Ts: 0,0500
                                                  //| Avg: 0,0476; T: 0,0476; Ts: 0,0476
                                                  //| Avg: 0,0476; T: 0,0653; Ts: 0,0476
                                                  //| Avg: 0,0480; T: 0,0484; Ts: 0,0484, 0,0476
                                                  //| Avg: 0,0450; T: 0,0450; Ts: 0,0450
                                                  //| Avg: 0,0442; T: 0,0435; Ts: 0,0435, 0,0450
                                                  //| Avg: 0,0428; T: 0,0421; Ts: 0,0421, 0,0435
                                                  //| Avg: 0,0428; T: 0,0453; Ts: 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0431; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0446; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0482; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,1298; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0521; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0452; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0429; T: 0,0470; Ts: 0,0431, 0,0421, 0,0435
                                                  //| Avg: 0,0414; T: 0,0407; Ts: 0,0407, 0,0421
                                                  //| Avg: 0,0414; T: 0,0445; Ts: 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0420; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0480; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0488; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0439; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0437; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0416; T: 0,0518; Ts: 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0419; T: 0,0427; Ts: 0,0427, 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0419; T: 0,0770; Ts: 0,0427, 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0419; T: 0,0446; Ts: 0,0427, 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0421; T: 0,0431; Ts: 0,0431, 0,0427, 0,0420, 0,0407, 0,0421
                                                  //| Avg: 0,0411; T: 0,0405; Ts: 0,0405, 0,0420, 0,0407
                                                  //| Avg: 0,0411; T: 0,0486; Ts: 0,0405, 0,0420, 0,0407
                                                  //| Avg: 0,0411; T: 0,0500; Ts: 0,0405, 0,0420, 0,0407
                                                  //| Avg: 0,0404; T: 0,0400; Ts: 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0404; T: 0,0431; Ts: 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0404; T: 0,0536; Ts: 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0407; T: 0,0418; Ts: 0,0418, 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0407; T: 0,0462; Ts: 0,0418, 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0407; T: 0,0442; Ts: 0,0418, 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0407; T: 0,0759; Ts: 0,0418, 0,0400, 0,0405, 0,0407
                                                  //| Avg: 0,0409; T: 0,0416; Ts: 0,0416, 0,0418, 0,0400, 0,0405, 0,0407
                                                  //| res1: scala.collection.parallel.ParSeq[(String, String, org.opalj.PC)] = P
                                                  //| arArray((com/sun/org/apache/xalan/internal/lib/ExsltMath,double constant(j
                                                  //| ava.lang.String,double),120))

    /*
    def pcsWithNewMethodDescriptorMatcherAndSetAndMatchPairManual =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                ObjectType("java/lang/Boolean"),
                ObjectType("java/lang/Byte"),
                ObjectType("java/lang/Character"),
                ObjectType("java/lang/Short"),
                ObjectType("java/lang/Integer"),
                ObjectType("java/lang/Long"),
                ObjectType("java/lang/Float"),
                ObjectType("java/lang/Double"))
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")

            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.matchPair { (i1, i2) ⇒
                    if (i1.opcode == INVOKESPECIAL.opcode && i2.opcode == INVOKEVIRTUAL.opcode) {
                        val ispecial = i1.asInstanceOf[INVOKESPECIAL]
                        val ivirtual = i2.asInstanceOf[INVOKEVIRTUAL]
                        val receiver1 = ispecial.declaringClass
                        val receiver2 = ivirtual.declaringClass

                        receiver1 == receiver2 &&
                            ispecial.methodDescriptor.returnType == VoidType &&
                            ispecial.methodDescriptor.parametersCount == 1 &&
                            ispecial.methodDescriptor.parameterType(0).isBaseType &&
                            ivirtual.methodDescriptor.returnType.isBaseType &&
                            ispecial.methodDescriptor.parameterType(0) != ivirtual.methodDescriptor.returnType &&
                            ivirtual.methodDescriptor.parametersCount == 0 &&
                            theTypes.contains(receiver1) &&
                            theMethods.contains(ivirtual.name)

                    } else {
                        false
                    }

                    /*
                    case (
                        INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)),
                        INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
                        ) ⇒ {
                        (receiver1 eq receiver2) &&
                            (returnType ne parameterType) && // coercion to another type performed
                            theTypes.contains(receiver1) &&
                            theMethods.contains(name)
                    }
                    case _ ⇒ false
                    */
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodDescriptorMatcherAndSetAndMatchPairManual
    */

    def pcsWithNewMethodDescriptorMatcherAndUnrolled =
        time(2, 4, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                ObjectType("java/lang/Boolean"),
                ObjectType("java/lang/Byte"),
                ObjectType("java/lang/Character"),
                ObjectType("java/lang/Short"),
                ObjectType("java/lang/Integer"),
                ObjectType("java/lang/Long"),
                ObjectType("java/lang/Float"),
                ObjectType("java/lang/Double"))
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue")

            var result: List[(String, String, UShort)] = List.empty
            for {
                classFile ← project.par.map(_._1)
                method @ MethodWithBody(body) ← classFile.methods
            } {
                val instructions = body.instructions
                val max_pc = body.instructions.length

                var pc = 0
                var next_pc = body.pcOfNextInstruction(pc)

                while (next_pc < max_pc) {
                    if (pc + 3 == next_pc) {
                        instructions(pc) match {
                            case INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)) ⇒
                                instructions(next_pc) match {
                                    case INVOKEVIRTUAL(`receiver1`, name, NoArgumentMethodDescriptor(returnType: BaseType)) if (returnType ne parameterType) &&
                                        (theTypes.contains(receiver1) &&
                                            theMethods.contains(name)) ⇒ {
                                        result = (classFile.fqn, method.toJava, pc) :: result
                                        // we have matched the sequence
                                        pc = body.pcOfNextInstruction(next_pc)
                                    }
                                    case _ ⇒
                                        pc = next_pc
                                        next_pc = body.pcOfNextInstruction(pc)

                                }
                            case _ ⇒
                                pc = next_pc
                                next_pc = body.pcOfNextInstruction(pc)
                        }
                    } else {
                        pc = next_pc
                        next_pc = body.pcOfNextInstruction(pc)
                    }
                }
            }
            result
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }                                         //> pcsWithNewMethodDescriptorMatcherAndUnrolled: => List[(String, String, org
                                                  //| .opalj.UShort)]
    pcsWithNewMethodDescriptorMatcherAndUnrolled  //> Avg: 0,1133; T: 0,1133; Ts: 0,1133
                                                  //| Avg: 0,0385; T: 0,0385; Ts: 0,0385
                                                  //| Avg: 0,0385; T: 0,0411; Ts: 0,0385
                                                  //| Avg: 0,0324; T: 0,0324; Ts: 0,0324
                                                  //| Avg: 0,0325; T: 0,0327; Ts: 0,0327, 0,0324
                                                  //| Avg: 0,0325; T: 0,0402; Ts: 0,0327, 0,0324
                                                  //| Avg: 0,0308; T: 0,0308; Ts: 0,0308
                                                  //| Avg: 0,0308; T: 0,0346; Ts: 0,0308
                                                  //| Avg: 0,0308; T: 0,0398; Ts: 0,0308
                                                  //| Avg: 0,0288; T: 0,0288; Ts: 0,0288
                                                  //| Avg: 0,0277; T: 0,0277; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0289; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0294; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0310; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0424; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0367; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0325; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0292; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0320; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0296; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0302; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0296; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0291; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0367; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0339; Ts: 0,0277
                                                  //| Avg: 0,0277; T: 0,0337; Ts: 0,0277
                                                  //| Avg: 0,0280; T: 0,0282; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0298; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0318; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0463; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0352; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0293; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0293; Ts: 0,0282, 0,0277
                                                  //| Avg: 0,0281; T: 0,0285; Ts: 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0283; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0293; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0303; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0309; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0353; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0282; T: 0,0333; Ts: 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| Avg: 0,0280; T: 0,0275; Ts: 0,0275, 0,0283, 0,0285, 0,0282, 0,0277
                                                  //| res2: List[(String, String, org.opalj.UShort)] = List((com/sun/org/apache/
                                                  //| xalan/internal/lib/ExsltMath,double constant(java.lang.String,double),120)
                                                  //| )
}