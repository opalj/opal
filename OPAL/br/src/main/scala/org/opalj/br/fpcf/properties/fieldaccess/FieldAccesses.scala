/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package fieldaccess

import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.fpcf.properties.Context
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.value.ValueInformation

import scala.collection.immutable.IntMap

/**
 * A convenience class for field access collection. Manages direct, indirect and incomplete
 * field access sites and allows the analyses to retrieve the required [[org.opalj.fpcf.PartialResult]]s for
 * [[FieldReadAccessInformation]], [[FieldWriteAccessInformation]], [[MethodFieldReadAccessInformation]] and
 * [[MethodFieldWriteAccessInformation]].
 *
 * @author Maximilian Rüsch
 */
sealed trait FieldAccesses {
    final def partialResults(accessContext: Context): IterableOnce[PartialResult[_, _ >: Null <: Property]] =
        if (containsNoMethodBasedAccessInformation)
            partialResultsForFieldBasedFieldAccesses
        else
            Iterator(
                partialResultForReadFields(accessContext),
                partialResultForWriteFields(accessContext)
            ) ++ partialResultsForFieldBasedFieldAccesses

    private[this] def containsNoMethodBasedAccessInformation =
        directAccessedFields.isEmpty && indirectAccessedFields.isEmpty && incompleteAccessSites.isEmpty

    protected def directAccessedFields: IntMap[IntTrieSet] = IntMap.empty
    protected def indirectAccessedFields: IntMap[IntTrieSet] = IntMap.empty
    protected def incompleteAccessSites: PCs = IntTrieSet.empty

    protected def parameters: IntMap[IntMap[AccessParameter]] = IntMap.empty

    protected def readReceivers: IntMap[IntMap[AccessReceiver]] = IntMap.empty
    protected def writeReceivers: IntMap[IntMap[AccessReceiver]] = IntMap.empty

    private[this] def partialResultForAccessedFields[S >: Null <: MethodFieldAccessInformation[S]](
        accessContext:             Context,
        propertyKey:               PropertyKey[S],
        noFieldAccessesValue:      S,
        fieldAccessesValueUpdater: S => S
    ): PartialResult[Method, S] = {
        val method = accessContext.method.definedMethod

        PartialResult[Method, S](method, propertyKey, {
            case InterimUBP(_) if containsNoMethodBasedAccessInformation =>
                None

            case InterimUBP(ub) =>
                Some(InterimEUBP(method, fieldAccessesValueUpdater(ub)))

            case _: EPK[_, _] if containsNoMethodBasedAccessInformation =>
                Some(InterimEUBP(method, noFieldAccessesValue))

            case _: EPK[_, _] =>
                Some(InterimEUBP(method, fieldAccessesValueUpdater(noFieldAccessesValue)))

            case r =>
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }

    private[this] def partialResultForReadFields(accessContext: Context): PartialResult[Method, MethodFieldReadAccessInformation] = {
        partialResultForAccessedFields(
            accessContext,
            MethodFieldReadAccessInformation.key,
            NoMethodFieldReadAccessInformation,
            previousFRA => previousFRA.updateWithFieldAccesses(
                accessContext,
                directAccessedFields,
                incompleteAccessSites,
                readReceivers
            )
        )
    }

    private[this] def partialResultForWriteFields(accessContext: Context): PartialResult[Method, MethodFieldWriteAccessInformation] = {
        partialResultForAccessedFields(
            accessContext,
            MethodFieldWriteAccessInformation.key,
            NoMethodFieldWriteAccessInformation,
            previousFWA => previousFWA.updateWithFieldAccesses(
                accessContext,
                directAccessedFields,
                incompleteAccessSites,
                writeReceivers,
                parameters
            )
        )
    }

    protected def partialResultsForFieldBasedFieldAccesses: IterableOnce[PartialResult[Field, _ >: Null <: FieldAccessInformation[_]]] =
        Iterator.empty
}

trait IncompleteFieldAccesses extends FieldAccesses {
    private[this] var _incompleteAccessSites = IntTrieSet.empty
    override protected def incompleteAccessSites: IntTrieSet = _incompleteAccessSites

    def addIncompleteAccessSite(pc: Int): Unit = _incompleteAccessSites += pc
}

trait CompleteFieldAccesses extends FieldAccesses {

    private def createFieldPartialResultForContext[S <: FieldAccessInformation[S]](
        field:       DeclaredField,
        propertyKey: PropertyKey[S],
        property:    S
    ): PartialResult[Field, _ >: Null <: FieldAccessInformation[_]] = {
        PartialResult[Field, FieldAccessInformation[S]](field.definedField, propertyKey, {
            case InterimUBP(ub) =>
                Some(InterimEUBP(field.definedField, ub.included(property)))

            case _: EPK[_, _] =>
                Some(InterimEUBP(field.definedField, property))

            case r =>
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }

    protected var _accessedFields: IntMap[IntTrieSet] = IntMap.empty

    private[this] var _partialResultsForFieldBasedFieldAccesses: List[PartialResult[Field, _ >: Null <: Property with FieldAccessInformation[_]]] = List.empty

    protected def addFieldAccess[S <: FieldAccessInformation[S]](
        pc:              Int,
        field:           DeclaredField,
        propertyKey:     PropertyKey[S],
        propertyFactory: () => S
    ): Unit = {
        val oldFieldsAtPCOpt = _accessedFields.get(pc)
        if (oldFieldsAtPCOpt.isEmpty) {
            _accessedFields = _accessedFields.updated(pc, IntTrieSet(field.id))
            _partialResultsForFieldBasedFieldAccesses ::= createFieldPartialResultForContext(field, propertyKey, propertyFactory())
        } else {
            val oldFieldsAtPC = oldFieldsAtPCOpt.get
            val newFieldsAtPC = oldFieldsAtPC + field.id

            // here we assert that IntSet returns the identity if the element is already contained
            if (newFieldsAtPC ne oldFieldsAtPC) {
                _accessedFields = _accessedFields.updated(pc, newFieldsAtPC)
                _partialResultsForFieldBasedFieldAccesses ::= createFieldPartialResultForContext(field, propertyKey, propertyFactory())
            }
        }
    }

    override protected def partialResultsForFieldBasedFieldAccesses: IterableOnce[PartialResult[Field, _ >: Null <: FieldAccessInformation[_]]] =
        _partialResultsForFieldBasedFieldAccesses.iterator ++ super.partialResultsForFieldBasedFieldAccesses
}

trait DirectFieldAccessesBase extends CompleteFieldAccesses {

    override protected def directAccessedFields: IntMap[IntTrieSet] = _accessedFields

    def addFieldRead(accessContext: Context, pc: Int, field: DeclaredField): Unit = {
        addFieldAccess(pc, field, FieldReadAccessInformation.key,
            () => FieldReadAccessInformation(Set((accessContext.method.asDefinedMethod, IntTrieSet(pc)))))
    }

    def addFieldWrite(accessContext: Context, pc: Int, field: DeclaredField): Unit = {
        addFieldAccess(pc, field, FieldWriteAccessInformation.key,
            () => FieldWriteAccessInformation(Set((accessContext.method.asDefinedMethod, IntTrieSet(pc)))))
    }
}

trait IndirectFieldAccessesBase extends CompleteFieldAccesses {

    private[this] var _parameters: IntMap[IntMap[AccessParameter]] = IntMap.empty
    override protected def parameters: IntMap[IntMap[AccessParameter]] = _parameters

    private[this] var _readReceivers: IntMap[IntMap[AccessReceiver]] = IntMap.empty
    override protected def readReceivers: IntMap[IntMap[AccessReceiver]] = _readReceivers
    private[this] var _writeReceivers: IntMap[IntMap[AccessReceiver]] = IntMap.empty
    override protected def writeReceivers: IntMap[IntMap[AccessReceiver]] = _writeReceivers

    override protected def indirectAccessedFields: IntMap[IntTrieSet] = _accessedFields

    @inline private[this] def pcFieldMapNestedUpdate[T](nestedMap: IntMap[IntMap[T]], pc: Int, fieldId: Int, value: T): IntMap[IntMap[T]] = {
        nestedMap.updated(pc, nestedMap.getOrElse(pc, IntMap.empty).updated(fieldId, value))
    }

    def addFieldRead(
        accessContext: Context,
        pc:            Int,
        field:         DeclaredField,
        receiver:      Option[(ValueInformation, IntTrieSet)]
    ): Unit = {
        addFieldAccess(pc, field, FieldReadAccessInformation.key,
            () => FieldReadAccessInformation(
                Set.empty[(DefinedMethod, PCs)],
                IntMap((accessContext.id, IntMap((pc, receiver))))
            ))
        _readReceivers = pcFieldMapNestedUpdate(_readReceivers, pc, field.id, receiver)
    }

    def addFieldWrite(
        accessContext: Context,
        pc:            Int,
        field:         DeclaredField,
        receiver:      Option[(ValueInformation, IntTrieSet)],
        param:         Option[(ValueInformation, IntTrieSet)]
    ): Unit = {
        addFieldAccess(pc, field, FieldWriteAccessInformation.key,
            () => FieldWriteAccessInformation(
                Set.empty[(DefinedMethod, PCs)],
                IntMap((accessContext.id, IntMap((pc, receiver)))),
                IntMap((accessContext.id, IntMap((pc, param))))
            ))
        _writeReceivers = pcFieldMapNestedUpdate(_writeReceivers, pc, field.id, receiver)
        _parameters = pcFieldMapNestedUpdate(_parameters, pc, field.id, param)
    }
}

class DirectFieldAccesses extends DirectFieldAccessesBase with IncompleteFieldAccesses
class IndirectFieldAccesses extends IndirectFieldAccessesBase with IncompleteFieldAccesses
