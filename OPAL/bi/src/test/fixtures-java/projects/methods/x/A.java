package x;

import y.B;

public class A {

  void  m() {
     System.out.println("A.m");
  }

  public static void main(String[] args) {
   A a = new B();
   a.m(); // calls <A>.m()
   ((B)a).m(); // calls <B>.m()
  }

}
