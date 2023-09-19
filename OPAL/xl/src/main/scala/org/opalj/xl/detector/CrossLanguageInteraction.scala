/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package detector

import org.opalj.br.FieldType
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.utility.JavaScriptFunctionCall
import Coordinator.V
import org.opalj.xl.utility.NativeFunctionCall
import org.opalj.xl.utility.Language
import org.opalj.xl.utility.Language.Language
import org.opalj.xl.Coordinator.ScriptEngineInstance

import org.opalj.fpcf.Property

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

case class ScriptEngineInteraction(
        language:                Language                                              = Language.Unknown,
        code:                    List[String]                                          = List.empty,
        javaScriptFunctionCalls: List[JavaScriptFunctionCall]                          = List.empty,
        puts:                    Map[String, (FieldType, Set[AnyRef], Option[Double])] = Map.empty,
        gets:                    Map[V, String]                                        = Map.empty
) extends CrossLanguageInteraction {
    def updated(
        language:                Language                                              = Language.Unknown,
        code:                    List[String]                                          = List.empty,
        javaScriptFunctionCalls: List[JavaScriptFunctionCall]                          = List.empty,
        puts:                    Map[String, (FieldType, Set[AnyRef], Option[Double])] = Map.empty,
        gets:                    Map[V, String]                                        = Map.empty
    ): ScriptEngineInteraction = {
        ScriptEngineInteraction(
            {
                if (this.language == Language.Unknown)
                    language
                else
                    this.language
            },
            code ++ this.code,
            javaScriptFunctionCalls ++ this.javaScriptFunctionCalls,
            this.puts ++ puts,
            this.gets ++ gets
        )
    }
    def update(scriptEngineInteraction: ScriptEngineInteraction): ScriptEngineInteraction = {
        updated(
            code = scriptEngineInteraction.code,
            javaScriptFunctionCalls = scriptEngineInteraction.javaScriptFunctionCalls,
            puts = scriptEngineInteraction.puts,
            gets = scriptEngineInteraction.gets
        )
    }
}

case class NativeInteraction(nativeFunctionCalls: List[NativeFunctionCall]) extends CrossLanguageInteraction
