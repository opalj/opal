/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat

/**
 * A class, field or method declaration's access flag. An access flag is
 * basically just a unique bit vector that can be combined with other
 * access flags to create an integer based bit vector that represents all
 * flags defined for a class, method or field declaration.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlag {

    /**
     * The Java (source code) name of the access flag if it exists. E.g.,
     * "public", "native", etc.
     */
    def javaName: Option[String]

    /**
     * The int mask of this access flag as defined by the JVM spec.
     */
    def mask: Int

    /**
     * Determines if this access flag is set in the given access_flags bit vector.
     * E.g., to determine if a method's static modifier is set it is sufficient
     * to call {{{ACC_STATIC ∈ method.access_flags}}}.
     */
    def element_of(access_flags: Int): Boolean = (access_flags & mask) != 0

    final def ∈(access_flags: Int): Boolean = element_of(access_flags)

    def toXML: scala.xml.Node
}

case object ACC_PUBLIC extends AccessFlag {
    val javaName: Option[String] = Some("public")
    val mask = 0x0001
    lazy val toXML = scala.xml.Elem(null, "public", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_PRIVATE extends AccessFlag {
    val javaName: Option[String] = Some("private")
    val mask = 0x0002
    lazy val toXML = scala.xml.Elem(null, "private", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_PROTECTED extends AccessFlag {
    val javaName: Option[String] = Some("protected")
    val mask = 0x0004
    lazy val toXML = scala.xml.Elem(null, "protected", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_STATIC extends AccessFlag {
    val javaName: Option[String] = Some("static")
    val mask = 0x0008
    lazy val toXML = scala.xml.Elem(null, "static", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_FINAL extends AccessFlag {
    val javaName: Option[String] = Some("final")
    val mask = 0x0010
    lazy val toXML = scala.xml.Elem(null, "final", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_SUPER extends AccessFlag {
    val javaName: Option[String] = None
    val mask = 0x0020
    lazy val toXML = scala.xml.Elem(null, "super", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_SYNCHRONIZED extends AccessFlag {
    val javaName: Option[String] = Some("synchronized")
    val mask = 0x0020
    lazy val toXML = scala.xml.Elem(null, "synchronized", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_VOLATILE extends AccessFlag {
    val javaName: Option[String] = Some("volatile")
    val mask = 0x0040
    lazy val toXML = scala.xml.Elem(null, "volatile", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_BRIDGE extends AccessFlag {
    val javaName: Option[String] = None
    val mask = 0x0040
    lazy val toXML = scala.xml.Elem(null, "bridge", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_TRANSIENT extends AccessFlag {
    val javaName: Option[String] = Some("transient")
    val mask = 0x0080
    lazy val toXML = scala.xml.Elem(null, "transient", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_VARARGS extends AccessFlag {
    val javaName: Option[String] = None
    val mask = 0x0080
    lazy val toXML = scala.xml.Elem(null, "varargs", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_NATIVE extends AccessFlag {
    val javaName: Option[String] = Some("native")
    val mask = 0x0100
    lazy val toXML = scala.xml.Elem(null, "native", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_INTERFACE extends AccessFlag {
    val javaName: Option[String] = None // this flag modifies the semantics of a class, but it is not an additional flag
    val mask = 0x0200
    lazy val toXML = scala.xml.Elem(null, "interface", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_ABSTRACT extends AccessFlag {
    val javaName: Option[String] = Some("abstract")
    val mask = 0x0400
    lazy val toXML = scala.xml.Elem(null, "abstract", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_STRICT extends AccessFlag {
    val javaName: Option[String] = Some("strictfp")
    val mask = 0x0800
    lazy val toXML = scala.xml.Elem(null, "strictfp", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_SYNTHETIC extends AccessFlag {
    val javaName: Option[String] = None
    val mask = 0x1000
    lazy val toXML = scala.xml.Elem(null, "synthetic", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_ANNOTATION extends AccessFlag {

    val javaName: Option[String] = None
    val mask = 0x2000
    lazy val toXML = scala.xml.Elem(null, "annotation", scala.xml.Null, scala.xml.TopScope)
}

case object ACC_ENUM extends AccessFlag {
    val javaName: Option[String] = None
    val mask = 0x4000
    lazy val toXML = scala.xml.Elem(null, "enum", scala.xml.Null, scala.xml.TopScope)
}

