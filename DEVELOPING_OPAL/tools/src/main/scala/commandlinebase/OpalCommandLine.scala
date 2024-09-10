package commandlinebase

import org.rogach.scallop.ScallopOption

trait OpalCommandLine[A, T] {
    val commandLine: ScallopOption[A];
    def parse(): T;
}
