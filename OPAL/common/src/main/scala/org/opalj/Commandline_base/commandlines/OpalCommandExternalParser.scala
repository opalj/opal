package org.opalj.Commandline_base.commandlines

trait OpalCommandExternalParser[A] {
    def parse[T](arg: T): Any
}
