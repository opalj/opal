/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package initialization;

abstract class Super{

    final int a = 5;

    final int i;

    Super(int i){
        doIt();
        this.i = i;
    }

    abstract int doIt();

}

class PrematureReads extends Super{

    final int b = 3;

    PrematureReads() {
        super(2);
    }

    int doIt() {
        System.out.println(a + " should be 5");
        System.out.println(b + " should be 3");
        System.out.println(i + " is unfortunately 0");
        return i;
    }


    public static void main(String []args) {
        PrematureReads pr = new PrematureReads();
        System.out.println("initalization finished");
        pr.doIt();
    }

}
