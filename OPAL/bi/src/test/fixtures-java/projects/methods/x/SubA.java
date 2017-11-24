package x;

import y.SubB;
import y.B;

public class SubA extends A {

  @Override
  public void m() {
     System.out.println("SubA.m");
  }

  public static void main(String[] args) {
   A a = new SubB();
   a.m(); // calls <SubB>.m()
   ((B)a).m(); // calls <SubB>.m()
  }

}
