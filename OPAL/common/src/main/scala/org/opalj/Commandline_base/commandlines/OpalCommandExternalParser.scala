package org.opalj.Commandline_base.commandlines

trait OpalCommandExternalParser {
    def parse[T](arg: T): Any
}
