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
package dialogs

import scalafx.Includes._
import scalafx.stage.Stage
import org.opalj.bugpicker.analysis.AnalysisParameters
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import scalafx.geometry.Insets
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos
import scalafx.scene.layout.GridPane
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.event.ActionEvent
import org.opalj.bugpicker.analysis.BugPickerAnalysis
import scalafx.scene.control.TitledPane

class AnalysisParametersDialog(owner: Stage) extends DialogStage(owner) {
    theStage ⇒
    title = "Set analysis parameters"

    var parameters: Option[AnalysisParameters] = None
    val buttonMinWidth = 80
    val buttonMargin = Insets(10)

    width = 640

    val maxEvalFactorField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }
    val maxEvalTimeField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }
    val maxCardinalityOfIntegerRangesField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }

    scene = new Scene {
        root = new BorderPane {
            center = new GridPane {
                add(new Label("Maximum evaluation factor:"), 0, 0)
                add(maxEvalFactorField, 1, 0)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalFactorField.text = BugPickerAnalysis.defaultMaxEvalFactor.toString
                    }
                }, 2, 0)

                add(new Label("Maximum evaluation time:"), 0, 1)
                add(maxEvalTimeField, 1, 1)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalTimeField.text = BugPickerAnalysis.defaultMaxEvalTime.toString
                    }
                }, 2, 1)

                add(new Label("Maximum cardinality of integer ranges:"), 0, 2)
                add(maxCardinalityOfIntegerRangesField, 1, 2)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxCardinalityOfIntegerRangesField.text = BugPickerAnalysis.defaultMaxCardinalityOfIntegerRanges.toString
                    }
                }, 2, 2)

                children foreach (c ⇒ GridPane.setMargin(c, Insets(10)))

                style = "-fx-border-width: 0 0 1 0; -fx-border-color: #ccc;"
            }
            bottom = new HBox {
                content = Seq(
                    new Button {
                        text = "_Defaults"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            maxEvalFactorField.text = BugPickerAnalysis.defaultMaxEvalFactor.toString
                            maxEvalTimeField.text = BugPickerAnalysis.defaultMaxEvalTime.toString
                            maxCardinalityOfIntegerRangesField.text = BugPickerAnalysis.defaultMaxCardinalityOfIntegerRanges.toString
                        }
                    },
                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒ close() }
                    },
                    new Button {
                        text = "_Ok"
                        mnemonicParsing = true
                        defaultButton = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            var interrupt = false
                            val maxEvalFactor = try {
                                maxEvalFactorField.text().toDouble
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum evaluation factor!",
                                        theStage)
                                    interrupt = true
                                    Double.NaN
                                }
                            }
                            val maxEvalTime = try {
                                maxEvalTimeField.text().toInt
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum evaluation time!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }
                            val maxCardinalityOfIntegerRanges = try {
                                maxCardinalityOfIntegerRangesField.text().toInt
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum cardinality of integer ranges!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }

                            if (!interrupt) {
                                parameters = Some(new AnalysisParameters(
                                    maxEvalTime = maxEvalTime,
                                    maxEvalFactor = maxEvalFactor,
                                    maxCardinalityOfIntegerRanges = maxCardinalityOfIntegerRanges))
                                close()
                            }
                        }
                    }
                )
                alignment = Pos.CENTER
            }
        }
    }

    def show(parameters: AnalysisParameters): Option[AnalysisParameters] = {
        maxEvalFactorField.text = parameters.maxEvalFactor.toString
        maxEvalTimeField.text = parameters.maxEvalTime.toString
        maxCardinalityOfIntegerRangesField.text = parameters.maxCardinalityOfIntegerRanges.toString
        showAndWait()
        this.parameters
    }
}
