/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import scala.xml.Node

import org.opalj.io.writeAndOpen
import org.opalj.br.Code
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.FieldAccess
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadString
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.ai.Domain
import org.opalj.ai.Update
import org.opalj.ai.AIResult
import org.opalj.ai.AITracer
import org.opalj.ai.Locals
import org.opalj.ai.Operands
import org.opalj.ai.common.XHTML.dumpLocals
import org.opalj.ai.common.XHTML.dumpStack
import org.opalj.collection.mutable.IntArrayStack

case class FlowEntity(
        pc:          Int,
        instruction: Instruction,
        operands:    Operands[_ >: Null <: Domain#DomainValue],
        locals:      Locals[_ >: Null <: Domain#DomainValue],
        properties:  Option[String]
) {
    val flowId = FlowEntity.nextFlowId
}

object FlowEntity {
    private var flowId = -1
    private def nextFlowId = { flowId += 1; flowId }
    def lastFlowId: Int = flowId - 1
}

/**
 * A tracer that generates an HTML document.
 *
 * @author Michael Eichberg
 */
trait XHTMLTracer extends AITracer {

    private[this] var flow: List[List[FlowEntity]] = List(List.empty)
    private[this] def newBranch(): List[List[FlowEntity]] = {
        flow = List.empty[FlowEntity] :: flow
        flow
    }
    private[this] def addFlowEntity(flowEntity: FlowEntity): Unit = {
        if (flow.head.exists(_.pc == flowEntity.pc))
            newBranch()

        flow = (flowEntity :: flow.head) :: flow.tail
    }

    private def instructionToNode(
        flowId:      Int,
        pc:          Int,
        instruction: Instruction
    ): xml.Node = {
        val openDialog = "$( \"#dialog"+flowId+"\" ).dialog(\"open\");"
        val instructionAsString =
            instruction match {
                case NEW(objectType) =>
                    "new …"+objectType.simpleName;
                case CHECKCAST(referenceType) =>
                    "checkcast "+referenceType.toJava;
                case LoadString(s) if s.size < 5 =>
                    "Load \""+s+"\"";
                case LoadString(s) =>
                    "Load \""+s.substring(0, 4)+"…\""
                case fieldAccess: FieldAccess =>
                    fieldAccess.mnemonic+" "+fieldAccess.name
                case invoke: NonVirtualMethodInvocationInstruction =>
                    val declaringClass = invoke.declaringClass.toJava
                    "…"+declaringClass.substring(declaringClass.lastIndexOf('.') + 1)+" "+
                        invoke.name+"(…)"
                case _ => instruction.toString(pc)
            }

        <span onclick={ openDialog } title={ instruction.toString(pc) }>
            { instructionAsString }
        </span>
    }

    def dumpXHTML(title: String): scala.xml.Node = {
        import scala.collection.immutable.SortedMap
        import scala.collection.immutable.SortedSet

        val inOrderFlow = flow.map(_.reverse).reverse
        var pathsCount = 0
        var pcs = SortedSet.empty[Int /*PC*/ ]
        for (path <- flow) {
            pathsCount += 1
            for (entity <- path) {
                pcs += entity.pc
            }
        }
        val pcsToRowIndex = SortedMap.empty[Int, Int] ++ pcs.zipWithIndex
        val ids = new java.util.IdentityHashMap[AnyRef, Integer]
        var nextId = 1
        val idsLookup = (value: AnyRef) => {
            var id = ids.get(value)
            if (id == null) {
                id = nextId
                nextId += 1
                ids.put(value, id)
            }
            id.intValue()
        }
        val dialogSetup =
            (for {
                path <- inOrderFlow
                entity <- path
            } yield {
                xml.Unparsed("$(function() { $( \"#dialog"+entity.flowId+"\" ).dialog({autoOpen:false}); });\n")
            })
        val dialogs: Iterable[Node] =
            (for {
                (path, index) <- inOrderFlow.zipWithIndex
                flowEntity <- path
            } yield {
                val dialogId = "dialog"+flowEntity.flowId
                <div id={ dialogId } title={ s"${(index + 1)} - ${flowEntity.pc} (${flowEntity.instruction.mnemonic})" }>
                    <b>Stack</b><br/>
                    { dumpStack(flowEntity.operands)(Some(idsLookup)) }
                    <b>Locals</b><br/>
                    { dumpLocals(flowEntity.locals)(Some(idsLookup)) }
                </div>
            })
        def row(pc: Int) =
            (for (path <- inOrderFlow) yield {
                val flowEntity = path.find(_.pc == pc)
                <td>
                    {
                        flowEntity.
                            map(fe => instructionToNode(fe.flowId, pc, fe.instruction)).
                            getOrElse(xml.Text(" "))
                    }
                </td>
            })
        val cfJoins = code.cfJoins
        val flowTable =
            for ((pc, rowIndex) <- pcsToRowIndex) yield {
                <tr>
                    <td>{ if (cfJoins.contains(pc)) "⇶ " else "" } <b>{ pc }</b></td>
                    { row(pc) }
                </tr>
            }

        <html lang="en">
            <head>
                <meta charset="utf-8"/>
                <title>{ title+" (Paths: "+pathsCount+"; Flow Nodes: "+FlowEntity.lastFlowId+")" }</title>
                <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css"/>
                <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
                <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
                <script>
                    { dialogSetup }
                </script>
                <style>
                    table {{
            width:100%;
            font-size: 12px;
            font-family: Tahoma;
            margin: 0px;
            padding: 0px;
            border: 1px solid gray;
            border-collapse: collapse;
        }}
        tr {{
            margin: 0px;
            padding: 0px;
            border: 0px:
        }}
        td {{
            margin: 0px;
            padding: 0px;
            border: 0px;
        }}
        td ~ td {{
            border-right: 1px solid #999;
        }}
        td.hovered {{
            background-color: lightblue;
            color: #666;
        }}
        /*
        ui-dialog: The outer container of the dialog.
        ui-dialog-titlebar: The title bar containing the dialog's title and close button.
        ui-dialog-title: The container around the textual title of the dialog.
        ui-dialog-titlebar-close: The dialog's close button.
        ui-dialog-content: The container around the dialog's content. This is also the element the widget was instantiated with.
        ui-dialog-buttonpane: The pane that contains the dialog's buttons. This will only be present if the buttons option is set.
        ui-dialog-buttonset: The container around the buttons themselves.
        */
        .ui-dialog-content {{
            font-family: Tahoma,Arial;
            font-size: 11px;
        }}
                </style>
            </head>
            <body style="font-family:Tahoma;font-size:8px;">
                <label for="filter">Filter</label>
                <input type="text" name="filter" value="" id="filter" title="Use a RegExp to filter(remove) elements. E.g.,'DUP|ASTORE|ALOAD'"/>
                <table>
                    <thead><tr>
                               <td>PC&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                               { (1 to inOrderFlow.size).map(index => <td>{ index }</td>) }
                           </tr></thead>
                    <tbody>
                        { flowTable }
                    </tbody>
                </table>
                { dialogs }
                <script>
                    $('tbody tr').hover(function(){{
            $(this).find('td').addClass('hovered');
        }}, function(){{
            $(this).find('td').removeClass('hovered');
        }});
        function filter(selector, query) {{
            $(selector).each(function() {{
                ($(this).text().search(new RegExp(query, 'i')){ xml.Unparsed("<") }
                    0) ? $(this).show().addClass('visible') : $(this).hide().removeClass('visible');
            }});
        }};
        //default each row to visible
        $('tbody tr').addClass('visible');

        $('#filter').keyup(function(event) {{
                //if esc is pressed or nothing is entered
                if (event.keyCode == 27 || $(this).val() == '') {{
                    //if esc is pressed we want to clear the value of search box
                    $(this).val('');
                    //we want each row to be visible because if nothing
                    //is entered then all rows are matched.
                    $('tbody tr').removeClass('visible').show().addClass('visible');
                }}
                //if there is text, lets filter
                else {{
                    filter('tbody tr', $(this).val());
                }}
        }});
                </script>
            </body>
        </html>
    }

    private var code: Code = null

    override def initialLocals(domain: Domain)(locals: domain.Locals): Unit = { /*EMPTY*/ }

    override def continuingInterpretation(
        code:   Code,
        domain: Domain
    )(
        initialWorkList:                  List[Int /*PC*/ ],
        alreadyEvaluated:                 IntArrayStack,
        operandsArray:                    domain.OperandsArray,
        localsArray:                      domain.LocalsArray,
        memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , domain.OperandsArray, domain.LocalsArray)]
    ): Unit = {
        if ((this.code eq code) || (this.code == null))
            this.code = code
        else
            throw new IllegalStateException("this XHTMLtracer is already used; create a new one")

    }

    private[this] var continuingWithBranch = true

    override def flow(
        domain: Domain
    )(
        currentPC:                Int,
        successorPC:              Int,
        isExceptionalControlFlow: Boolean
    ): Unit = {
        continuingWithBranch = currentPC < successorPC
    }

    override def deadLocalVariable(domain: Domain)(pc: Int, lvIndex: Int): Unit = { /*EMPTY*/ }

    override def noFlow(domain: Domain)(currentPC: Int, targetPC: Int): Unit = { /*EMPTY*/ }

    override def rescheduled(
        domain: Domain
    )(
        sourcePC:                 Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        worklist:                 List[Int /*PC*/ ]
    ): Unit = {
        /*ignored for now*/
    }

    override def instructionEvalution(
        domain: Domain
    )(
        pc:          Int,
        instruction: Instruction,
        operands:    domain.Operands,
        locals:      domain.Locals
    ): Unit = {
        if (!continuingWithBranch)
            newBranch()

        addFlowEntity(FlowEntity(pc, instruction, operands, locals, domain.properties(pc)))
        // if we have a call to instruction evaluation without an intermediate
        // flow call, we are continuing the evaluation with a branch
        continuingWithBranch = false
    }

    override def join(
        domain: Domain
    )(
        pc:            Int,
        thisOperands:  domain.Operands,
        thisLocals:    domain.Locals,
        otherOperands: domain.Operands,
        otherLocals:   domain.Locals,
        result:        Update[(domain.Operands, domain.Locals)]
    ): Unit = { /*ignored*/ }

    override def establishedConstraint(
        domain: Domain
    )(
        pc:          Int,
        effectivePC: Int,
        operands:    domain.Operands,
        locals:      domain.Locals,
        newOperands: domain.Operands,
        newLocals:   domain.Locals
    ): Unit = { /*ignored*/ }

    override def abruptMethodExecution(
        domain: Domain
    )(
        pc:        Int,
        exception: domain.ExceptionValue
    ): Unit = { /*ignored*/ }

    override def jumpToSubroutine(
        domain: Domain
    )(
        pc: Int, target: Int, nestingLevel: Int
    ): Unit = { /* ignored */ }

    override def returnFromSubroutine(
        domain: Domain
    )(
        pc:            Int,
        returnAddress: Int,
        subroutinePCs: List[Int /*PC*/ ]
    ): Unit = { /*ignored*/ }

    override def abruptSubroutineTermination(
        domain: Domain
    )(
        details:  String,
        sourcePC: Int, targetPC: Int, jumpToSubroutineId: Int,
        terminatedSubroutinesCount: Int,
        forceScheduling:            Boolean,
        oldWorklist:                List[Int /*PC*/ ],
        newWorklist:                List[Int /*PC*/ ]
    ): Unit = { /*ignored*/ }

    /**
     * Called when a ret instruction is encountered.
     */
    override def ret(
        domain: Domain
    )(
        pc:            Int,
        returnAddress: Int,
        oldWorklist:   List[Int /*PC*/ ],
        newWorklist:   List[Int /*PC*/ ]
    ): Unit = { /*ignored*/ }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int], message: => String
    ): Unit = { /*EMPTY*/ }

    def result(result: AIResult): Unit = {
        writeAndOpen(dumpXHTML((new java.util.Date).toString()), "AITrace", ".html")
    }

}
