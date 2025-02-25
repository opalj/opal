import java.io.IOException;
import java.lang.reflect.Array;

public class CaughtException {

    public static void main(String[] args) {

        //TEST 1 - no exception
        try {
            System.out.println("test1 - no exception");
        } catch (Exception e) {
            System.out.println("caught test1 exception");
        }

        //TEST 2 - exception using throw
        try {
            System.out.println("test2 - throwing exception");
            throw new Exception();
        } catch (Exception e) {
            System.out.println("caught test2 exception");
        }

        //TEST 3 - exception from variable
        Exception exc = new Exception();
        try {
            System.out.println("test3 - throwing exception from var 1/2");
            throw exc;
        } catch (Exception e) {
            System.out.println("caught test3 exception from var 1/2");
        }
        //TEST 3.5 exception from variable + finally
        try {
            System.out.println("test3.5 - throwing exception local var 2/2");
            throw exc;
        } catch (Exception e) {
            System.out.println("caught test3.5 exception from var 2/2");
        } finally {
            System.out.println("test3.5 finally executed");
        }

        //TEST 4 - ArrayIndexOutOfBounds
        try {
            System.out.println("test4 - trying to access outside of array");
            int[] a = {0, 1, 2};
            int outOfBounds = a[3];
        } catch (Exception e) {
            System.out.println("caught test4 exception");
        }

        //TEST 5 - NullPointer + Catch Hierarchy + finally
        try {
            System.out.println("test5 - nullpointer");
            int[] a = null;
            int nullpointer = a.length;
        } catch (NullPointerException e) {
            System.out.println("caught test5 NullPointerException");
        } catch (Exception e) {
            System.out.println("caught test5 exception");
        } finally {
            System.out.println("test5 finally executed");
        }
    }
}