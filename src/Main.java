import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    private static final String firstTape = "firstTape.txt";
    private static final String secondTape = "secondTape.txt";
    private static Tape mergeTape = new Tape("mergeTape.txt", TapeType.MERGE);

    public static void main(String[] args) {
        CommunicationManager.setupScanner();
        CommunicationManager.welcome();

        // Setting mergeTape.
        RecordsSource recordsSource = CommunicationManager.whatRecordsSource();
        if (recordsSource == RecordsSource.UNKNOWN) {
            CommunicationManager.somethingWentWrong("Records source is unknown.");
            return;
        }

        FileManager.clearFile(mergeTape.getFile());

        if (!setupRecordsFile(recordsSource)) {
            CommunicationManager.somethingWentWrong("Records file wasn't setup.");
            return;
        }

        // mergeTape print
        try {
            FileManager.printFile(mergeTape);
        } catch (IOException e) {
            CommunicationManager.somethingWentWrong(
                    "Error in reading from file.\n" + Arrays.toString(e.getStackTrace())
            );
            return;
        }

        // Whether start sorting.
        if (!CommunicationManager.whetherStartSort()) {
            CommunicationManager.somethingWentWrong("Will to quit or bad command.");
            return;
        }

        if (!FileManager.naturalMergeSort(mergeTape, firstTape, secondTape)) {
            CommunicationManager.somethingWentWrong("Natural merge has failed.");
            return;
        }

        CommunicationManager.closeScanner();
    }


    /**
     * @return True if records are in the mergeTape.
     */
    public static Boolean setupRecordsFile(RecordsSource recordsSource) {
        switch (recordsSource) {
            case GENERATE -> {
                Long recordsNumber = CommunicationManager.recordsNumber();
                if (!mergeTape.getFile().exists()) {
                    CommunicationManager.say("File doesn't exist.");
                    return false;
                }
                return RecordsGenerator.generate(mergeTape.getFile(), recordsNumber);
            }
            case MANUALLY -> {
                Long recordsNumber = CommunicationManager.recordsNumber();
                if (!mergeTape.getFile().exists()) {
                    CommunicationManager.say("File doesn't exist.");
                    return false;
                }
                return RecordsGenerator.generateManually(mergeTape.getFile(), recordsNumber);
            }
            case TEST_FILE -> {
                String testFilePath = CommunicationManager.testFilePath();

                mergeTape = new Tape(testFilePath, TapeType.MERGE);

                if (!mergeTape.getFile().exists()) {
                    CommunicationManager.say("File doesn't exist.");
                    return false;
                } else {
                    return true;
                }
            }

            default -> {
                return false;
            }
        }
    }
}