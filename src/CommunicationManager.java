import java.util.Scanner;

public class CommunicationManager {
    private static Scanner scanner;

    public static void setupScanner() {
        scanner = new Scanner(System.in);
    }

    public static void closeScanner() {
        scanner.close();
    }

    public static void welcome() {
        System.out.println("---------------------------------");
        System.out.println("Welcome in Natural Merge simulator!");
    }

    public static void somethingWentWrong(String cause) {
        System.out.println("---------------------------------");
        System.out.println("Something went wrong, program will be stopped. \nCAUSE:\n" + cause);
    }

    public static void say(String message) {
        System.out.println(message);
    }

    public static RecordsSource whatRecordsSource() {
        System.out.println("---------------------------------");
        System.out.println(
                """
                        What records source do you want? (type the number)
                        1. Generate records.
                        2. Put the records manually.
                        3. Records are in the file.
                        """
        );

        int choice = Integer.parseInt(scanner.nextLine());
        switch (choice) {
            case 1 -> {
                return RecordsSource.GENERATE;
            }
            case 2 -> {
                return RecordsSource.MANUALLY;
            }
            case 3 -> {
                return RecordsSource.TEST_FILE;
            }
            default -> {
                return RecordsSource.UNKNOWN;
            }
        }
    }

    public static Long recordsNumber() {
        System.out.println("---------------------------------");
        System.out.println("How many records?");

        return Long.parseLong(scanner.nextLine());
    }

    public static String testFilePath() {
        System.out.println("---------------------------------");
        System.out.println("What is the test file path?");
        return scanner.nextLine();
    }

    public static boolean whetherStartSort() {
        System.out.println("---------------------------------");
        System.out.println("Start sort? (type: 'yes' or 'no')");
        String input = scanner.nextLine();
        switch (input) {
            case "yes" -> { return true; }
            case "no" -> { return false; }
            default -> {
                say("'" + input + "' command is invalid.");
                return false;
            }
        }
    }

    public static boolean whetherPrintFile() {
        System.out.println("Show file? (type: 'yes' or 'no')");
        String input = scanner.nextLine();
        switch (input) {
            case "yes" -> { return true; }
            case "no" -> { return false; }
            default -> {
                say("'" + input + "' command is invalid.");
                return false;
            }
        }
    }

    public static String[] getRecord() {
        String[] numbers;
        String input;
        do {
            System.out.println("---------------------------------");
            System.out.println("Type in " + RecordsGenerator.PARAMETER_NUMBERS + " whole numbers delimited by spaces in-between (ex. '10 -11 ... 32 -3').");
            input = scanner.nextLine();
            numbers = input.split(" ");

        } while (!(numbers.length == RecordsGenerator.PARAMETER_NUMBERS));

        return numbers;
    }
}

