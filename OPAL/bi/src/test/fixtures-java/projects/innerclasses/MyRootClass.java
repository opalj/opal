/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package innerclasses;

/**
 * Code that contains some (anonymous) inner classes; do NOT change the order of the defined
 * methods/inner classes!
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
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
