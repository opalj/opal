/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.io.Source
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Unparsed
import org.opalj.io.process
import org.opalj.bi.AccessFlags
import org.opalj.bi.reader.Constant_PoolAbstractions
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_SUPER

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class ClassFile(
        constant_pool: Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags:  Int                 = ACC_PUBLIC.mask | ACC_SUPER.mask,
        this_class:    Constant_Pool_Index,
        super_class:   Constant_Pool_Index,
        interfaces:    Interfaces          = NoInterfaces,
        fields:        Fields              = NoFields,
        methods:       Methods             = NoMethods,
        attributes:    Attributes          = NoAttributes
) {

    assert({
        val cp0 = constant_pool(0)
        (cp0 eq null) || cp0.isInstanceOf[Constant_PoolAbstractions#DeferredActionsStore]
    })

    /**
     * Size of the class file in bytes.
     */
    def size: Int = {
        4 + // magic
            2 + // minor_version
            2 + // major_version
            2 + // constant_pool_count
            {
                val cpIt = constant_pool.iterator
                cpIt.next() // the first entry is always empty in the class file
                cpIt.
                    filter(_ ne null /*handles the case of Constant_Long and Constant_Double*/ ).
                    map(_.size).
                    sum
            } +
            2 + // access_flags
            2 + // this_class
            2 + // super_class
            2 + // interfaces count
            interfaces.length * 2 + // interfaces[interfaces_count]
            2 + // fields_count
            fields.view.map(_.size).sum +
            2 + // methods_count
            methods.view.map(_.size).sum +
            2 + // attributes_count
            attributes.view.map(_.size).sum
    }

    def jdkVersion: String = org.opalj.bi.jdkVersion(major_version)

    private[this] implicit val cp = constant_pool

    /**
     * The fully qualified name of this class in Java notation (i.e., using dots
     * to seperate packages.)
     */
    final val thisType: ObjectTypeInfo = cp(this_class).asConstantClass.asJavaClassOrInterfaceType

    final val superTypes = {
        {
            if (super_class != 0)
                Seq(
                    Text("extends "),
                    asJavaObjectType(cp(super_class).toString).asSpan("extends"),
                    Text(" ")
                )
            else
                NodeSeq.Empty
        } ++ {
            if (interfaces.nonEmpty)
                Seq(
                    Text("implements "),
                    asJavaObjectType(cp(interfaces.head).toString).asSpan("implements"),
                    interfaces.tail.map { i =>
                        Seq(Text(", "), asJavaObjectType(cp(i).toString).asSpan("implements"))
                    }
                )
            else
                NodeSeq.Empty
        }
    }

    /**
     * Converts the constant pool to (x)HTML5.
     */
    def cpToXHTML: Node = {
        val cpEntries =
            for {
                cpIndex <- 1 until constant_pool.length
                cpNode = cp(cpIndex)
                if cpNode != null /* <= need for constant_double/_long entries */
            } yield {
                <li value={ cpIndex.toString }>{ cpNode.asCPNode }</li>
            }

        <ol class="cp_entries">{ cpEntries }</ol>
    }

    def attributeToXHTML(attribute: Attribute): Node = {
        attribute match {
            case ica: InnerClasses_attribute => ica.toXHTML(thisType)
            case _                           => attribute.toXHTML(cp)
        }
    }

    def fieldsToXHTML: Iterator[Node] = fields.iterator.map { field => field.toXHTML }

    def methodsToXHTML: Iterator[Node] = {
        methods.iterator.zipWithIndex.map { mi => val (method, index) = mi; method.toXHTML(index) }
    }

    protected def accessFlags: Node = {
        <span class="access_flags">{ AccessFlags.classFlagsToJava(access_flags) }</span>
    }

    protected def filter: Node = {
        <details class="filter_settings" open="true">
            <summary>Filter</summary>
            <fieldset>
                <input type="radio" id="access_flag_private" name="visibility" value="private" onclick="toogleFilter();"></input><label for="access_flag_private">private</label>
                <input type="radio" id="access_flag_default" name="visibility" value="default" onclick="toogleFilter();"></input><label for="access_flag_default">&lt;default&gt;</label>
                <input type="radio" id="access_flag_protected" name="visibility" value="protected" onclick="toogleFilter();"></input><label for="access_flag_protected">protected</label>
                <input type="radio" id="access_flag_public" name="visibility" value="public" onclick="toogleFilter();"></input><label for="access_flag_public">public</label>
            </fieldset>
            <fieldset>
                <input type="radio" id="access_flag_final" name="final_or_abstract" value="final" onclick="toogleFilter();"></input><label for="access_flag_final">final</label>
                <input type="radio" id="access_flag_abstract" name="final_or_abstract" value="abstract" onclick="toogleFilter();"></input><label for="access_flag_abstract">abstract</label>
            </fieldset>
            <div class="java_flags">
                <input type="checkbox" id="access_flag_static" name="static" value="static" onclick="toogleFilter();"></input><label for="access_flag_static">static</label>
                <input type="checkbox" id="access_flag_strict" value="strict" onclick="toogleFilter();"></input><label for="access_flag_strict">strict</label>
                <input type="checkbox" id="access_flag_native" value="native" onclick="toogleFilter();"></input><label for="access_flag_native">native</label>
                <input type="checkbox" id="access_flag_synchronized" value="synchronized" onclick="toogleFilter();"></input><label for="access_flag_synchronized">synchronized</label>
            </div>
            <div class="jvm_flags">
                <input type="checkbox" id="jvm_modifier_bridge" value="bridge" onclick="toogleFilter();"></input><label for="jvm_modifier_bridge">bridge</label>
                <input type="checkbox" id="jvm_modifier_varargs" value="varargs" onclick="toogleFilter();"></input><label for="jvm_modifier_varargs">varargs</label>
            </div>
            <div class="name_filter">
                <label for="filter_by_method_name">Name:</label><input id="filter_by_method_name" type="text" title='filter by method name' onkeyup="toogleFilter();"></input>
            </div>
            <button value="clear" onclick="clearFilter();">clear</button>
        </details>
    }

    /**
     * Creates an XHTML representation of the ClassFile.
     *
     * @param embeddedCSS A string which contains a CSS.
     * @param cssFile Reference to a(nother) CSS file.
     * @param jsFile Reference to a JavaScript file.
     * @return The generatd HTML.
     */
    def toXHTML(
        source:            Option[AnyRef],
        embeddedCSS:       Option[String] = Some(ClassFile.TheCSS),
        cssFile:           Option[String] = None,
        jsFile:            Option[String] = None,
        withMethodsFilter: Boolean        = true
    ): Node =
        <html>
            <head>
                <title>Java Bytecode of { thisType.asJava }</title>
                <style type="text/css">{ scala.xml.Unparsed(ClassFile.ResetCSS) }</style>
                {
                    if (embeddedCSS.isDefined)
                        <style type="text/css">{ scala.xml.Unparsed(embeddedCSS.get) }</style>
                }
                {
                    if (cssFile.isDefined)
                        <link rel="stylesheet" href={ cssFile.get }></link>
                }{
                    if (withMethodsFilter)
                        <script>{ scala.xml.Unparsed(ClassFile.FilterJS) }</script>
                }
                {
                    if (jsFile.isDefined)
                        <script type="text/javascript" src={ jsFile.get }></script>
                }
            </head>
            <body>
                { classFileToXHTML(source, withMethodsFilter) }
            </body>
        </html>

    def classFileToXHTML(source: Option[AnyRef]): Node = classFileToXHTML(source, false)

    // this file is private to ensure that no meaningless html files are generated
    // (i.e. with the fields for the filter, but without the necessary logic)
    private[this] def classFileToXHTML(source: Option[AnyRef], withMethodsFilter: Boolean): Node = {

        val (sourceFileAttributes, attributes0) = partitionByType(attributes, classOf[SourceFile_attribute])
        val (signatureAttributes, attributes1) = partitionByType(attributes0, classOf[Signature_attribute])

        <div class="class_file">
            { if (source.isDefined) <div id="source">{ source.get }</div> }
            <div id="class_file_header">
                { accessFlags }
                <span id="defined_class">{ thisType.asJava }</span>
                { superTypes }
                {
                    if (signatureAttributes.nonEmpty) {
                        Seq(<br/>, signatureAttributes.head.signatureSpan)
                    }
                }
                <br/>
                {
                    sourceFileAttributes.headOption.map { a =>
                        Seq(
                            Text("Source file: "),
                            <span class="source_file">{ a.sourceFile } </span>,
                            Unparsed("&nbsp; &mdash; &nbsp;")
                        )
                    }.getOrElse(NodeSeq.Empty)
                }
                <span id="class_file_version">Version:&nbsp;{ s"$major_version.$minor_version ($jdkVersion)" }</span>
                &nbsp; &mdash; &nbsp;
                <span>Size:&nbsp;{ size }bytes</span>
            </div>
            <div class="constant_pool">
                <details>
                    <summary>Constant Pool</summary>
                    { cpToXHTML }
                </details>
            </div>
            <div class="members">
                {
                    if (attributes1.nonEmpty)
                        <div class="attributes">
                            <details>
                                <summary>Attributes</summary>
                                { attributes1.map[Node](attributeToXHTML) }
                            </details>
                        </div>
                }{
                    if (fields.nonEmpty) {
                        <div class="fields">
                            <details open="">
                                <summary>Fields</summary>{ fieldsToXHTML }
                            </details>
                        </div>
                    }
                }
                {
                    if (methods.nonEmpty) {
                        <div class="methods">
                            <details open="">
                                <summary>Methods</summary>
                                { if (withMethodsFilter) filter else NodeSeq.Empty }
                                { methodsToXHTML }
                            </details>
                        </div>
                    }
                }
            </div>
        </div>
    }

}

object ClassFile {

    private def loadResource(js: String): String = {
        process(this.getClass.getResourceAsStream(js))(Source.fromInputStream(_).mkString)
    }

    final val ResetCSS: String = loadResource("reset.css")
    final val TheCSS: String = loadResource("style.css")
    final val FilterJS: String = loadResource("filter.js")

}
