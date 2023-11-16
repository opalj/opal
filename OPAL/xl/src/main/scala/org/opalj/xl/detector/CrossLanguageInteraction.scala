/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package detector

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.utility.JavaScriptFunctionCall
import org.opalj.xl.utility.NativeFunctionCall
import org.opalj.xl.utility.Language
import org.opalj.xl.utility.Language.Language
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.Coordinator.V

import org.opalj.fpcf.Property
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.TheTACAI

sealed trait CrossLanguageInteractionPropertyMetaInformation extends Property with PropertyMetaInformation {
    final type Self = CrossLanguageInteraction
}

sealed trait CrossLanguageInteraction extends CrossLanguageInteractionPropertyMetaInformation { // extends OrderedProperty
    def meet(other: CrossLanguageInteraction): CrossLanguageInteraction = {
        (this, other) match {
            case (_, _) => ScriptEngineInteraction()
        }
    }

    def checkIsEqualOrBetterThan(e: Entity, other: CrossLanguageInteraction): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[CrossLanguageInteraction] = CrossLanguageInteraction.key
}

object CrossLanguageInteraction extends CrossLanguageInteractionPropertyMetaInformation {
    final val key: PropertyKey[CrossLanguageInteraction] = PropertyKey.create(
        "CrossLanguageInteraction",
        (_: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case ScriptEngineInstance => ScriptEngineInteraction()
                case x                    => throw new IllegalArgumentException(s"$x is not a method")
            }
        }
    )
}

case class ScriptEngineInteraction[ContextType, PointsToSet](
        language:                Language                                                                   = Language.Unknown,
        code:                    List[String]                                                               = List.empty,
        javaScriptFunctionCalls: List[JavaScriptFunctionCall[ContextType, PointsToSet]]                     = List.empty[JavaScriptFunctionCall[ContextType, PointsToSet]],
        puts:                    Map[(String, ContextType, TheTACAI), (PointsToSet, V, Option[ObjectType])] = Map.empty[(String, ContextType, TheTACAI), (PointsToSet, V, Option[ObjectType])]
) extends CrossLanguageInteraction {
    def updated(scriptEngineInteraction: ScriptEngineInteraction[ContextType, PointsToSet]): ScriptEngineInteraction[ContextType, PointsToSet] = {
        ScriptEngineInteraction(
            if (this.language == Language.Unknown) scriptEngineInteraction.language else this.language,
            scriptEngineInteraction.code ::: this.code,
            this.javaScriptFunctionCalls ++ scriptEngineInteraction.javaScriptFunctionCalls,
            this.puts ++ scriptEngineInteraction.puts, //TODO join PointsToSet.included zum zusammenfassen
        )
    }
}

case class NativeInteraction(nativeFunctionCalls: List[NativeFunctionCall]) extends CrossLanguageInteraction
