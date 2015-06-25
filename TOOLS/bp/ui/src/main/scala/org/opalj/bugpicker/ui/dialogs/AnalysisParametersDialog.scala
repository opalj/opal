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
package org.opalj
package bugpicker
package ui
package dialogs

import org.opalj.bugpicker.core.analysis.AnalysisParameters
import org.opalj.bugpicker.core.analysis.BugPickerAnalysis
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxNode2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.stage.Stage
import org.opalj.util.Milliseconds

/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author David Becker
 */
class AnalysisParametersDialog(owner: Stage) extends DialogStage(owner) {
    theStage ⇒

    title = "Set analysis parameters"

    var parameters: Option[AnalysisParameters] = None
    val buttonMinWidth = 80
    val buttonMargin = Insets(10)

    width = 640

    val maxEvalFactorField = new TextField {
        hgrow = Priority.Always
        alignment = Pos.BaselineRight
    }

    val maxEvalTimeField = new TextField {
        hgrow = Priority.Always
        alignment = Pos.BaselineRight
    }

    val maxCardinalityOfIntegerRangesField = new TextField {
        hgrow = Priority.Always
        alignment = Pos.BaselineRight
    }

    val maxCardinalityOfLongSetsField = new TextField {
        hgrow = Priority.Always
        alignment = Pos.BaselineRight
    }

    val maxCallChainLengthField = new TextField {
        hgrow = Priority.Always
        alignment = Pos.BaselineRight
    }

    import BugPickerAnalysis._

    scene = new Scene {
        root = new BorderPane {
            center = new GridPane {
                add(new Label("Maximum evaluation factor:"), 0, 0)
                add(maxEvalFactorField, 1, 0)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalFactorField.text = DefaultMaxEvalFactor.toString
                    }
                }, 2, 0)

                add(new Label("Maximum evaluation time (ms):"), 0, 1)
                add(maxEvalTimeField, 1, 1)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalTimeField.text = DefaultMaxEvalTime.timeSpan.toString
                    }
                }, 2, 1)

                add(new Label("Maximum cardinality of integer ranges:"), 0, 2)
                add(maxCardinalityOfIntegerRangesField, 1, 2)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxCardinalityOfIntegerRangesField.text = DefaultMaxCardinalityOfIntegerRanges.toString
                    }
                }, 2, 2)

                add(new Label("Maximum cardinality of long sets:"), 0, 3)
                add(maxCardinalityOfLongSetsField, 1, 3)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxCardinalityOfLongSetsField.text = DefaultMaxCardinalityOfLongSets.toString
                    }
                }, 2, 3)

                add(new Label("Maximum length of call chain:"), 0, 4)
                add(maxCallChainLengthField, 1, 4)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxCallChainLengthField.text = DefaultMaxCallChainLength.toString
                    }
                }, 2, 4)

                children foreach (c ⇒ GridPane.setMargin(c, Insets(10)))

                style = "-fx-border-width: 0 0 1 0; -fx-border-color: #ccc;"
            }

            bottom = new HBox {
                children = Seq(
                    new Button {
                        text = "_Defaults"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            maxEvalFactorField.text = DefaultMaxEvalFactor.toString
                            maxEvalTimeField.text = DefaultMaxEvalTime.toString
                            maxCardinalityOfIntegerRangesField.text = DefaultMaxCardinalityOfIntegerRanges.toString
                            maxCardinalityOfLongSetsField.text = DefaultMaxCardinalityOfLongSets.toString
                            maxCallChainLengthField.text = DefaultMaxCallChainLength.toString
                        }
                    },
                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒ close() }
                    },
                    new Button {
                        text = "_Ok"
                        mnemonicParsing = true
                        defaultButton = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            var interrupt = false
                            val maxEvalFactor = try {
                                maxEvalFactorField.text().toDouble
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for the maximum evaluation factor!",
                                        theStage)
                                    interrupt = true
                                    Double.NaN
                                }
                            }
                            val maxEvalTime = try {
                                new Milliseconds(maxEvalTimeField.text().toLong)
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for the maximum evaluation time!",
                                        theStage)
                                    interrupt = true
                                    Milliseconds.None
                                }
                            }
                            val maxCardinalityOfIntegerRanges = try {
                                maxCardinalityOfIntegerRangesField.text().toLong
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for the maximum cardinality of integer ranges!",
                                        theStage)
                                    interrupt = true
                                    Long.MinValue
                                }
                            }
                            val maxCardinalityOfLongSets = try {
                                maxCardinalityOfLongSetsField.text().toInt
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for the maximum cardinality of long sets!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }
                            val maxCallChainLength = try {
                                maxCallChainLengthField.text().toInt
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for the maximum call chain length!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }

                            if (!interrupt) {
                                parameters = Some(new AnalysisParameters(
                                    maxEvalTime = maxEvalTime,
                                    maxEvalFactor = maxEvalFactor,
                                    maxCardinalityOfIntegerRanges = maxCardinalityOfIntegerRanges,
                                    maxCardinalityOfLongSets = maxCardinalityOfLongSets,
                                    maxCallChainLength = maxCallChainLength))
                                close()
                            }
                        }
                    }
                )
                alignment = Pos.Center
            }
        }
    }

    def show(parameters: AnalysisParameters): Option[AnalysisParameters] = {
        maxEvalFactorField.text = parameters.maxEvalFactor.toString
        maxEvalTimeField.text = parameters.maxEvalTime.toString
        maxCardinalityOfIntegerRangesField.text = parameters.maxCardinalityOfIntegerRanges.toString
        maxCardinalityOfLongSetsField.text = parameters.maxCardinalityOfLongSets.toString
        maxCallChainLengthField.text = parameters.maxCallChainLength.toString
        showAndWait()
        this.parameters
    }
}
