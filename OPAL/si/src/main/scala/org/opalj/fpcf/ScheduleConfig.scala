package org.opalj
package fpcf

class ScheduleConfig private (
    private var currentStrategy: ScheduleStrategy,
    private var lazyTransformerInBatches: Boolean
) {
    def setStrategy(strategy: ScheduleStrategy): Unit = {
        currentStrategy = strategy
    }

    def getStrategy: ScheduleStrategy = currentStrategy

    def setLazyTransformerInMultipleBatches(enable: Boolean): Unit = {
        lazyTransformerInBatches = enable
    }

    def isLazyTransformerInMultipleBatches: Boolean = lazyTransformerInBatches
}

object ScheduleConfig {
    private var config: Option[ScheduleConfig] = None

    def getConfig: ScheduleConfig = {
        config.getOrElse {
            config = Some(new ScheduleConfig(
                ScheduleStrategy.SPS, // Default strategy: SPS
                false                 // Default lazyTransformerInMultipleBatches: false
                ))
            config.get
        }
    }
}