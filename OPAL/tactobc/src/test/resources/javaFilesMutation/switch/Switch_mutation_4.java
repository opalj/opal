public class Switch_mutation_4 {

    public static void main(String[] args) {
        // Array of fruits to ensure all cases are covered
        String[] fruits = {"Banana", "Brocoli", "Wednesday"};

        // Loop through all possible fruits
        for (String fruit : fruits) {
            printFruit(fruit);
        }
    }

    public static void printFruit(String fruit) {
        switch (fruit) {
            case "Banana":
                System.out.println(fruit + " is a fruit");
                break;
            case "Brocoli":
                System.out.println("is not valid.");
                System.out.println(fruit + " is a vegetable.");
                break;
            default:
                System.out.println(fruit + " is not valid.");
                break;
        }
    }
}