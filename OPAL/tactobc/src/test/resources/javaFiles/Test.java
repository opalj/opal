public class Test {

    public static void main(String[] args){
        int x = 0;
        for (int i = 0; i < 3; i++){
            x += Integer.MAX_VALUE;
        }
        System.out.println(x);
    }
}