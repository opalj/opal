/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Signature_attribute(
        attribute_name_index: Constant_Pool_Index,
        signature_index:      Constant_Pool_Index
) extends Attribute {

    /**
     * The value of the attribute_length item is fixed; it is always 2.
     */
    final override def attribute_length = 2

    def signature(implicit cp: Constant_Pool): String = cp(signature_index).toString

    def signatureSpan(implicit cp: Constant_Pool): Node = {
        <span class="signature">{ cp(signature_index).toString }</span>
    }

    // Primarily implemented to handle the case if the signature attribute is not
    // found in an expected case.
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details><summary class="attribute_name">Signature</summary>{ signature }</details>
    }

}
/* Functionality, which may be useful when creating a better representation of type signatures.
+    def decipher(sig: String): Node = {
+        val fts = SignatureParser.parseFieldTypeSignature(sig)
Add a comment to this line
+        typeSigToXHTML(fts)
+    }
+
+    def typeSigToXHTML(ts: TypeSignature): Node = ts match {
+        case _: BooleanType =>
+            <span>boolean</span>
+        case _: ByteType =>
+            <span>byte</span>
+        case _: CharType =>
+            <span>char</span>
+        case _: DoubleType =>
+            <span>double</span>
+        case _: FloatType =>
+            <span>float</span>
+        case _: IntegerType =>
+            <span>int</span>
+        case _: LongType =>
+            <span>long</span>
+        case _: ShortType =>
+            <span>short</span>
+
+        case scts: SimpleClassTypeSignature =>
+            <span>
+                <span class="fqn">{ scts.simpleName }</span>
+                &lt;{ scts.typeArguments.map(typeArgToXHTML) }
+                &gt;
+            </span>
+        case ats: ArrayTypeSignature =>
+            <span>[{ typeSigToXHTML(ats.typeSignature) }</span>
+        case tvs: TypeVariableSignature =>
+            <span>T{ tvs.identifier };&nbsp;</span>
+        case cts: ClassTypeSignature =>
+            <span>
+                L
+                {
+                    if (cts.packageIdentifier.isDefined)
+                        <span class="fqn">
+                            { cts.packageIdentifier.get + cts.simpleClassTypeSignature.simpleName }
+                        </span>
+                    else
+                        <span>
+                            { cts.simpleClassTypeSignature.simpleName }
+                        </span>
+                }
+                &lt;
+                { cts.simpleClassTypeSignature.typeArguments.map { typeArgToXHTML(_) } }
+                &gt;
+                {
+                    cts.classTypeSignatureSuffix.map {
+                        e => e.simpleName + e.typeArguments.map { typeArgToXHTML(_) }
+                    }
+                }
+                ;&nbsp;
+            </span>
+        case _ => <span>{ ts.toJVMSignature }</span>
+    }
+
+    def typeArgToXHTML(ta: TypeArgument): Node = {
+        ta match {
+            case w: Wildcard =>
+                <span>*</span>
+            case prop: ProperTypeArgument =>
+                <span>
+                    { if (prop.varianceIndicator.isDefined) prop.varianceIndicator.get.toJVMSignature }
+                    { typeSigToXHTML(prop.fieldTypeSignature) }
+                </span>
+            case _ =>
+                // this is a complete list of TypeArugments at the time of writing
+                throw new IllegalArgumentException("unknown TypeArugment")
+        }
+    }
 */
