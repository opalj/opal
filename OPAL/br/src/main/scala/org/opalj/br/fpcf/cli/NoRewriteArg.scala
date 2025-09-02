/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.flagConverter

import org.opalj.br.reader.DynamicConstantRewriting
import org.opalj.br.reader.InvokedynamicRewriting

object NoRewriteArg extends PlainArg[Boolean] {
    override val name: String = "noRewrite"
    override val description: String = "Don't rewrite InvokeDynamic instructions and dynamic constants"
    override val defaultValue: Option[Boolean] = Some(false)

    override def apply(config: Config, value: Option[Boolean]): Config = {
        val newConfig = NoInvokeDynamicRewriteArg(config, value)
        NoDynamicConstantRewriteArg(newConfig, value)
    }
}

object NoInvokeDynamicRewriteArg extends PlainArg[Boolean] {
    override val name: String = "noInvokeDynamicRewrite"
    override val description: String = "Don't rewrite InvokeDynamic instructions"
    override val defaultValue: Option[Boolean] = Some(false)

    override def apply(config: Config, value: Option[Boolean]): Config = {
        if (value.get)
            config.withValue(
                InvokedynamicRewriting.InvokedynamicRewritingConfigKey,
                ConfigValueFactory.fromAnyRef(value.get)
            )
        else config
    }
}

object NoDynamicConstantRewriteArg extends PlainArg[Boolean] {
    override val name: String = "noDynamicConstantRewrite"
    override val description: String = "Don't rewrite dynamic constants"
    override val defaultValue: Option[Boolean] = Some(false)

    override def apply(config: Config, value: Option[Boolean]): Config = {
        if (value.get)
            config.withValue(
                DynamicConstantRewriting.RewritingConfigKey,
                ConfigValueFactory.fromAnyRef(value.get)
            )
        else config
    }
}
