/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import java.io.File
import java.net.URL

import play.api.libs.json.Writes
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject

import org.opalj.br.LocalVariable
import org.opalj.br.Type
import org.opalj.br.BaseType
import org.opalj.br.VoidType
import org.opalj.br.BooleanType
import org.opalj.br.ObjectType
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.methodAccessFlagsToString
import org.opalj.br.CTIntType
import org.opalj.value.IsIntegerValue

/**
 * Defines implicit conversions to wrap some types of analyses such that they generate
 * results of type [[org.opalj.br.analyses.ReportableAnalysisResult]].
 *
 * @author Michael Eichberg
 */
package object issues {

    implicit object IssueDetailsWrites extends Writes[IssueDetails] {
        def writes(issueDetails: IssueDetails): JsValue = issueDetails.toIDL
    }

    implicit object IssueLocationWrites extends Writes[IssueLocation] {
        def writes(issueLocation: IssueLocation): JsValue = issueLocation.toIDL
    }

    implicit object RelevanceWrites extends Writes[Relevance] {
        def writes(relevance: Relevance): JsValue = relevance.toIDL
    }

    /**
     * Shortens an absolute path to one relative to the current working directory.
     */
    def absoluteToRelative(path: String): String = {
        path.stripPrefix(System.getProperty("user.dir") + System.getProperty("file.separator"))
    }

    /**
     * Turns the jar URL format into a string better suited for the console reports.
     */
    def prettifyJarUrl(jarurl: String): String = {
        // Extract the paths of jar and class files.
        // jar URL format: jar:file:<jar path>!/<inner class file path>
        val split = jarurl.stripPrefix("jar:file:").split("!/")

        val jar = absoluteToRelative(split.head)
        val file = split.last

        jar+"!/"+Console.BOLD + file + Console.RESET
    }

    /**
     * Converts a URL into a string, intended to be displayed as part of console reports.
     *
     * Absolute file names are shortened to be relative to the current directory,
     * to avoid using up too much screen space in the console.
     */
    def urlToLocationIdentifier(url: URL): String = {
        url.getProtocol() match {
            case "file" => absoluteToRelative(url.getPath())
            case "jar"  => prettifyJarUrl(url.toExternalForm())
            case _      => url.toExternalForm()
        }
    }

    def fileToLocationIdentifier(file: File): String = file.getAbsolutePath()

    /**
     * Given a `LocalVariable` object and its current value a human readable `String`
     * is created.
     */
    def localVariableToString(localVariable: LocalVariable, value: AnyRef): String = {
        if ((localVariable.fieldType eq BooleanType) &&
            // SPECIAL HANDLING IF THE VALUE IS AN INTEGER RANGE VALUE
            value.isInstanceOf[IsIntegerValue]) {
            val range = value.asInstanceOf[IsIntegerValue]
            if ( /*range.lowerBound == 0 &&*/ range.upperBound == 0)
                "false"
            else if (range.lowerBound == 1 /* && range.upperBound == 1*/ )
                "true"
            else
                "true or false"
        } else
            value.toString
    }

    def typeToIDL(t: Type): JsValue = {
        t match {
            case bt: BaseType => Json.obj("bt" -> bt.toJava)
            case CTIntType    => Json.obj("bt" -> "<Computational Type Int>")

            case ot: ObjectType =>
                Json.obj("ot" -> ot.toJava, "simpleName" -> ot.simpleName)
            case at: ArrayType =>
                Json.obj("at" -> typeToIDL(at.elementType), "dimensions" -> at.dimensions)

            case VoidType => Json.obj("vt" -> "void")

        }
    }

    def methodToIDL(
        accessFlags: Int,
        name:        String,
        descriptor:  MethodDescriptor
    ): JsObject = {
        Json.obj(
            "accessFlags" -> methodAccessFlagsToString(accessFlags),
            "name" -> name,
            "returnType" -> typeToIDL(descriptor.returnType),
            "parameters" -> descriptor.parameterTypes.map[JsValue](typeToIDL)
        )
    }

}
