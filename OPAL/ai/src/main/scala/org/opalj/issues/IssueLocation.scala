/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import java.lang.Comparable

import scala.xml.Node
import scala.xml.Text
import scala.xml.Group
import scala.xml.UnprefixedAttribute

import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber

import org.opalj.br.PC
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.Field
import org.opalj.br.methodToXHTML
import org.opalj.br.typeToXHTML
import org.opalj.br.classAccessFlagsToString
import org.opalj.br.classAccessFlagsToXHTML
import org.opalj.br.analyses.SomeProject

/**
 * The location of an issue.
 *
 * @author Michael Eichberg
 */
sealed abstract class IssueLocation extends IssueRepresentations with Comparable[IssueLocation] {

    /**
     * The description of the issue with respect to the given location.
     */
    def description: Option[String]

    def compareTo(other: IssueLocation): Int
}

abstract class ProjectLocation(
        val description: Option[String],
        val theProject:  SomeProject,
        val details:     Seq[IssueDetails] = List.empty
) extends IssueLocation {

    def compareTo(other: IssueLocation): Int = {
        other match {
            case _: InstructionLocation => 1
            case _: MethodLocation      => 1
            case _: FieldLocation       => 1
            case _: ClassLocation       => 1
            case _: PackageLocation     => 1
            case that: ProjectLocation =>
                that.theProject.hashCode() compare this.theProject.hashCode() match {
                    case 0 =>
                        (this.description, that.description) match {
                            case (None, None)       => 0
                            case (Some(_), None)    => -1
                            case (None, Some(_))    => 1
                            case (Some(x), Some(y)) => x compare y
                        }
                    case x => x
                }

        }
    }

    override def toAnsiColoredString: String = {
        description.map(_.replace('\n', ';')).getOrElse("")
    }

    override def toEclipseConsoleString: String = {
        description.map(_.replace('\n', ';')).getOrElse("")
    }
}

class PackageLocation(
        description:    Option[String],
        theProject:     SomeProject,
        val thePackage: String,
        details:        Seq[IssueDetails] = List.empty
) extends ProjectLocation(description, theProject, details) {

    def locationAsInlineXHTML(basicInfoOnly: Boolean): List[Node] = {
        List(<span class="package">{ thePackage.replace('/', '.') }</span>)
    }

    def descriptionAsXHTML: List[Node] = {
        if (description.isDefined)
            List(<br/>, Text(description.get))
        else
            List.empty[Node]
    }

    def detailsAsXHTML(basicInfoOnly: Boolean): List[Node] = {
        details.map(d => <div>{ d.toXHTML(basicInfoOnly) }</div>).toList
    }

    final override def toXHTML(basicInfoOnly: Boolean): Node = {
        Group(List(
            <dt>location</dt>,
            <dd>
                { locationAsInlineXHTML(basicInfoOnly) }
                { descriptionAsXHTML }
                { detailsAsXHTML(basicInfoOnly) }
            </dd>
        ))
    }

    def locationAsIDL: JsObject = Json.obj("package" -> thePackage.replace('/', '.'))

    def detailsAsIDL: JsValue = Json.toJson(details)

    final override def toIDL: JsValue = {
        Json.obj(
            "description" -> description,
            "location" -> locationAsIDL,
            "details" -> detailsAsIDL
        )
    }
}

class ClassLocation(
        description:   Option[String],
        theProject:    SomeProject,
        val classFile: ClassFile,
        details:       Seq[IssueDetails] = List.empty
) extends PackageLocation(description, theProject, classFile.thisType.packageName, details)
    with ClassComprehension {

    override def locationAsInlineXHTML(basicInfoOnly: Boolean): List[Node] = {
        val locationAsInlineXHTML = super.locationAsInlineXHTML(basicInfoOnly) ++
            List(
                Text("."),
                <span class="declaring_class" data-class={ classFile.fqn }>
                    { typeToXHTML(classFile.thisType, true) }
                </span>
            )
        if (basicInfoOnly || classFile.accessFlags == 0)
            locationAsInlineXHTML
        else
            classAccessFlagsToXHTML(classFile.accessFlags) :: Text(" ") :: locationAsInlineXHTML
    }

    override def toAnsiColoredString: String = {
        theProject.source(classFile.thisType).map(_.toString).getOrElse("<No Source>")+":"
    }

    override def locationAsIDL: JsObject = {
        super.locationAsIDL + (
            (
                "class",
                Json.obj(
                    "fqn" -> classFile.fqn,
                    "type" -> typeToIDL(classFile.thisType),
                    "accessFlags" -> {
                        classFile.accessFlags match {
                            case 0 => JsNull
                            case _ => classAccessFlagsToString(classFile.accessFlags)
                        }
                    }
                )
            )
        )
    }
}

class MethodLocation(
        description: Option[String],
        theProject:  SomeProject,
        val method:  Method,
        details:     Seq[IssueDetails] = List.empty
) extends ClassLocation(description, theProject, method.classFile, details)
    with MethodComprehension {

    val firstLineOfMethod: Option[String] = {
        method.body.flatMap(_.firstLineNumber.map(ln => (if (ln > 2) (ln - 2) else 0).toString))
    }

    override def locationAsInlineXHTML(basicInfoOnly: Boolean): List[Node] = {
        var methodNode =
            <span class="method" data-method={ methodJVMSignature }>
                {
                    if (basicInfoOnly)
                        methodToXHTML(method.name, method.descriptor, true)
                    else
                        methodToXHTML(method.accessFlags, method.name, method.descriptor, true)

                }
            </span>
        if (firstLineOfMethod.isDefined) {
            val firstLine = firstLineOfMethod.get.toString
            methodNode %= new UnprefixedAttribute("data-line", firstLine, scala.xml.Null)
        }

        super.locationAsInlineXHTML(basicInfoOnly) ++ List(Text("{ "), methodNode, Text(" }"))
    }

    override def locationAsIDL: JsObject = {
        super.locationAsIDL + (
            "method" -> (
                methodToIDL(method.accessFlags, method.name, method.descriptor) +
                ("signature" -> JsString(methodJVMSignature)) +
                ("firstLine" -> Json.toJson(firstLineOfMethod))
            )
        )
    }
}

class InstructionLocation(
        description: Option[String],
        theProject:  SomeProject,
        method:      Method,
        val pc:      PC,
        details:     Seq[IssueDetails] = List.empty
) extends MethodLocation(description, theProject, method, details) with PCLineComprehension {

    assert(method.body.isDefined)

    def code: Code = method.body.get

    override def locationAsInlineXHTML(basicInfoOnly: Boolean): List[Node] = {
        val superLocationAsInlineXHTML = super.locationAsInlineXHTML(basicInfoOnly)
        superLocationAsInlineXHTML.init ++
            List(Text("["), pcNode, lineNode, Text("]"), superLocationAsInlineXHTML.last)
    }

    override def locationAsIDL: JsObject = {
        var instructionLocation = Json.obj("pc" -> pc)
        if (line.isDefined) instructionLocation += (("line", JsNumber(line.get)))

        super.locationAsIDL + (("instruction", instructionLocation))
    }

    override def toEclipseConsoleString: String = {
        val source = classFile.thisType.toJava.split('$').head
        val line = this.line.map(line => s":$line").getOrElse("")
        "("+source+".java"+line+") "
    }

    override def toAnsiColoredString: String = {
        theProject.source(classFile.thisType).map(_.toString).getOrElse("<No Source>")+":"+
            line.map(line => s"$line: ").getOrElse(" ")
    }

}

class FieldLocation(
        description: Option[String],
        theProject:  SomeProject,
        classFile:   ClassFile,
        val field:   Field,
        details:     Seq[IssueDetails] = List.empty
) extends ClassLocation(description, theProject, classFile, details)
