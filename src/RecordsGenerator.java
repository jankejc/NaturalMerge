import java.io.*;
import java.util.Random;

public class RecordsGenerator {

    public static final Long RANDOM_MIN = -5L;
    public static final Long RANDOM_MAX = 5L;
    public static final int FIXED_DIGITS_NUMBER = 5; // max digit number -> a4 * x^4 -> -5 * (-5^4) = -3125 => ~5 digits
    public static final int PARAMETER_NUMBERS = 6; // 44. Rekordy pliku: parametry a0,a1,a2,a3,a4,x.

    /**
     * @return True if generating was successful.
     */
    public static boolean generate(File recordsFile, Long recordsNumber) {

        Random random = new Random();
        for (long i = 0L; i < recordsNumber; i++) {
            StringBuilder recordToFile = new StringBuilder();

            for (int k = 0; k < PARAMETER_NUMBERS; k++) {
                Long randomLongInBound = random.nextLong(RANDOM_MAX - RANDOM_MIN + 1) + RANDOM_MIN;
                recordToFile.append(formatNumber(randomLongInBound.toString()));
            }

            // Write record by record.
            if (!FileManager.writeToFile(recordsFile, recordToFile.toString())) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return True if generating was successful.
     */
    public static boolean generateManually(File recordsFile, Long recordsNumber) {
        for (long i = 0L; i < recordsNumber; i++) {
            String[] numbersInRecord = CommunicationManager.getRecord();
            StringBuilder recordToFile = new StringBuilder();
            for (String number : numbersInRecord) {
                recordToFile.append(formatNumber(number));
            }

            // Write record by record.
            if (!FileManager.writeToFile(recordsFile, recordToFile.toString())) {
                return false;
            }
        }

        return true;
    }

    private static String formatNumber(String numberString) {
        return String.format("%0" + FIXED_DIGITS_NUMBER + "d", Long.parseLong(numberString));
    }
}
