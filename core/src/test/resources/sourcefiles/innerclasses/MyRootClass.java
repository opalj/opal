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
package innerclasses;

/**
 * Code that contains some inner classes.
 * 
 * Notice, we test on the anonymous inner classes, thus do NOT change the order of the defined
 * methods!
 * 
 * @author Ralf Mitschke
 */
@SuppressWarnings("all")
public class MyRootClass {

    public static void main(String[] args) {
        MyRootClass outer = new MyRootClass();

        MyRootClass.InnerPrinterOfX printer = outer.new InnerPrinterOfX();
        printer.displayInt();
        printer.displayIntReallyNice();
        outer.printXWithInnerClass();
        outer.anonymousClassField.displayInt();
        outer.anonymousClassField.displayIntReallyNice();
        outer.printXWithAnonymousInnerClass();

    }

    private int x = 3;

    private InnerPrinterOfX anonymousClassField = new InnerPrinterOfX() {
        class InnerPrinterOfAnonymousClass {
            public void displayInt() {
                System.out.println("anonymous inner declared" + x);
            }
        }

        public void displayInt() {
            new InnerPrinterOfAnonymousClass().displayInt();
        }

        public void displayIntReallyNice() {
            System.out.println(new Formatter() {
                public String format(final int theX) {

                    InnerPrinterOfX printer = new InnerPrinterOfX() {
                        public void displayInt() {
                            this.prettyPrinter.format(theX);
                        }
                    };
                    String prefix = "this is the x in a really nice anonymous presentation: ";

                    printer.prettyPrinter = this;
                    return prefix + theX;
                }
            }.format(x));
        }
    };

    interface Formatter {
        public String format(int theX);
    }

    class InnerPrinterOfX {
        public Formatter prettyPrinter;

        public InnerPrinterOfX() {
            this.prettyPrinter = new Formatter() {
                public String format(int theX) {
                    return "this is the x in a really nice presentation:" + theX;
                }
            };
        }

        class InnerPrettyPrinter implements Formatter {
            public String format(int theX) {
                return "this is the x:" + theX;
            }
        }

        public void displayInt() {
            InnerPrettyPrinter prettyPrinter = new InnerPrettyPrinter();
            System.out.println(prettyPrinter.format(x));
        }

        public void displayIntReallyNice() {
            System.out.println(prettyPrinter.format(x));
        }
    }

    private void printXWithInnerClass() {
        class MyInnerPrinter {
            public void displayInt() {
                System.out.println(x);
            }
        }
        // MyInnerPrinter can only be instantiated here
        MyInnerPrinter printer = new MyInnerPrinter();
        printer.displayInt();
    }

    private void printXWithAnonymousInnerClass() {
        InnerPrinterOfX printer = new InnerPrinterOfX() {
            public void displayInt() {
                System.out.println("anonymous " + x);
            }
        };
        printer.displayInt();
    }
}
