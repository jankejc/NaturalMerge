import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FileManager {
    private static final int PARAMETER_SIZE_IN_BYTES = RecordsGenerator.FIXED_DIGITS_NUMBER;
    private static final int RECORD_SIZE_IN_BYTES = RecordsGenerator.PARAMETER_NUMBERS * PARAMETER_SIZE_IN_BYTES;
    private static final int RECORDS_IN_BLOCK = 4;
    private static final int BLOCK_SIZE_IN_BYTES = RECORDS_IN_BLOCK * RECORD_SIZE_IN_BYTES;

    public static int phase = 1;

    public static boolean naturalMergeSort(
            Tape mergeTape,
            String firstTapePath,
            String secondTapePath

    ) {
        CommunicationManager.say("SORTING - natural merge");

        SortStatus status = SortStatus.NOT_COMPLETED;
        while (status == SortStatus.NOT_COMPLETED) {
            CommunicationManager.say("---------------------------------");
            CommunicationManager.say("PHASE " + phase);
            phase++;

            Tape firstTape = new Tape(firstTapePath, TapeType.FIRST);
            Tape secondTape = new Tape(secondTapePath, TapeType.SECOND);
            Tape currentTape = firstTape;

            mergeTape.setLastValue(null);
            clearFile(firstTape.getFile());
            clearFile(secondTape.getFile());

            byte[] block = new byte[BLOCK_SIZE_IN_BYTES];
            List<Record> records = new ArrayList<>();
            int seriesNumber = 2;
            while (true) {
                int bytesRead = setNextBlock(mergeTape, block);
                if (bytesRead == 0) {
                    CommunicationManager.say("Exception in getting next block!");
                    break;
                } else if (bytesRead == -1) {
                    mergeTape.setPositionInFile(0L);
                    break;
                }

                processBlock(
                        block,
                        bytesRead,
                        parameters -> records.add(new Record(parameters))
                );

                currentTape = distributeOnTapes(records, currentTape, firstTape, secondTape);
                records.clear();
            }

            clearFile(mergeTape.getFile());

            status = mergeTapes(firstTape, secondTape, mergeTape);
            if (status == SortStatus.ERROR) {
                return false;
            }

            if (CommunicationManager.whetherPrintFile()) {
                try {
                    CommunicationManager.say("");
                    FileManager.printFile(mergeTape);
                } catch (IOException e) {
                    CommunicationManager.say("Error in reading from file.\n" + Arrays.toString(e.getStackTrace()));
                    return false;
                }
            }
        }

        return true;
    }

    private static Tape distributeOnTapes(List<Record> records, Tape currentTape, Tape firstTape, Tape secondTape) {
        for (Record record : records) {
            if (currentTape.getLastValue() != null && record.getValue() < currentTape.getLastValue()) {
                currentTape.setLastValue(null);

                if (currentTape.getType() == TapeType.FIRST) {
                    currentTape = secondTape;
                } else {
                    currentTape = firstTape;
                }
            }

            if (!writeToFile(currentTape.getFile(), record.formatParametersToFile())) {
                throw new RuntimeException("Didn't placed on tape!");
            }
            currentTape.setLastValue(record.getValue());
        }

        return currentTape;
    }

    private static SortStatus mergeTapes(Tape firstTape, Tape secondTape, Tape mergeTape) {

        byte[] firstTapeBlock = new byte[BLOCK_SIZE_IN_BYTES];
        List<Record> firstTapeBlockRecords = new ArrayList<>();
        boolean isFirstTapeEOF = false;
        firstTape.setLastValue(null);

        byte[] secondTapeBlock = new byte[BLOCK_SIZE_IN_BYTES];
        List<Record> secondTapeBlockRecords = new ArrayList<>();
        boolean isSecondTapeEOF = false;
        secondTape.setLastValue(null);

        // When every new value on mergeTape from both tapes will be not lower than the previous one.
        boolean isSortDone = true;

        // These vaars flags the last elements in the last block.
        // All of that is needed because there is possibility to read block to second tape
        // while the first is already at last run.
        boolean isFirstTapeLastBlock = false;
        boolean isSecondTapeLastBlock = false;
        int endFirstIndex = 0;
        int endSecondIndex = 0;

        int firstIndex = RECORDS_IN_BLOCK;
        int secondIndex = RECORDS_IN_BLOCK;
        while (true) {
            if (firstIndex == RECORDS_IN_BLOCK || (isFirstTapeLastBlock && firstIndex == endFirstIndex)) {
                firstTapeBlockRecords.clear();
                firstIndex = 0;
                int firstTapeBytesRead = setNextBlock(firstTape, firstTapeBlock);
                if (firstTapeBytesRead == 0) {
                    CommunicationManager.say("Exception in getting next block!");
                    return SortStatus.ERROR;
                } else if (firstTapeBytesRead == -1) {
                    // No more bytes to read.
                    firstTape.setPositionInFile(0L);
                    isFirstTapeEOF = true;
                } else if (firstTapeBytesRead < BLOCK_SIZE_IN_BYTES) {
                    isFirstTapeLastBlock = true;
                    endFirstIndex = firstTapeBytesRead / RECORD_SIZE_IN_BYTES;
                }

                if (!isFirstTapeEOF) {
                    processBlock(
                            firstTapeBlock,
                            firstTapeBytesRead,
                            parameters -> firstTapeBlockRecords.add(new Record(parameters))
                    );
                }
            }

            if (secondIndex == RECORDS_IN_BLOCK || (isSecondTapeLastBlock && secondIndex == endSecondIndex)) {
                secondTapeBlockRecords.clear();
                secondIndex = 0;
                int secondTapeBytesRead = setNextBlock(secondTape, secondTapeBlock);
                if (secondTapeBytesRead == 0) {
                    CommunicationManager.say("Exception in getting next block!");
                    break;
                } else if (secondTapeBytesRead == -1) {
                    // No more bytes to read.
                    secondTape.setPositionInFile(0L);
                    isSecondTapeEOF = true;
                    // todo index
                } else if (secondTapeBytesRead < BLOCK_SIZE_IN_BYTES) {
                    isSecondTapeLastBlock = true;
                    endSecondIndex = secondTapeBytesRead / RECORD_SIZE_IN_BYTES;
                }

                if (!isSecondTapeEOF) {
                    processBlock(
                            secondTapeBlock,
                            secondTapeBytesRead,
                            parameters -> secondTapeBlockRecords.add(new Record(parameters))
                    );
                }
            }

            if (isFirstTapeEOF && isSecondTapeEOF) {
                break;
            }

            while (firstIndex + secondIndex < firstTapeBlockRecords.size() + secondTapeBlockRecords.size()) {

                // Both block have records to compare.
                if (firstIndex < firstTapeBlockRecords.size() && secondIndex < secondTapeBlockRecords.size()) {

                    if (!endOfSequence(firstTape, firstTapeBlockRecords.get(firstIndex))
                            && !endOfSequence(secondTape, secondTapeBlockRecords.get(secondIndex))) {
                        // If seq. on first and second tape is NOT finished.

                        Record firstRecord = firstTapeBlockRecords.get(firstIndex);
                        Record secondRecord = secondTapeBlockRecords.get(secondIndex);
                        if (firstRecord.getValue() <= secondRecord.getValue()) {
                            // If record from first tape is not bigger.
                            if (!putOnMergeTape(mergeTape, firstTape, firstRecord) && isSortDone) {
                                isSortDone = false;
                            }
                            firstIndex++;
                        } else {
                            // If record from second tape is not bigger.
                            if (!putOnMergeTape(mergeTape, secondTape, secondRecord) && isSortDone) {
                                isSortDone = false;
                            }
                            secondIndex++;
                        }
                    } else if (endOfSequence(firstTape, firstTapeBlockRecords.get(firstIndex))
                            && endOfSequence(secondTape, secondTapeBlockRecords.get(secondIndex))) {
                        // If seq. on first and second tape is finished.
                        firstTape.setLastValue(null);
                        secondTape.setLastValue(null);

                    } else if (endOfSequence(firstTape, firstTapeBlockRecords.get(firstIndex))
                            && !endOfSequence(secondTape, secondTapeBlockRecords.get(secondIndex))) {
                        // If seq. on first tape is finished put records from the second tape until end of sequence.
                        if (!putOnMergeTape(mergeTape, secondTape, secondTapeBlockRecords.get(secondIndex)) && isSortDone) {
                            isSortDone = false;
                        }
                        secondIndex++;
                    } else if (endOfSequence(secondTape, secondTapeBlockRecords.get(secondIndex))
                            && !endOfSequence(firstTape, firstTapeBlockRecords.get(firstIndex))) {
                        // If seq. on second tape is finished put records from the first tape until end of sequence.
                        if (!putOnMergeTape(mergeTape, firstTape, firstTapeBlockRecords.get(firstIndex)) && isSortDone) {
                            isSortDone = false;
                        }
                        firstIndex++;
                    }
                } else if (firstIndex < firstTapeBlockRecords.size() && isSecondTapeEOF) {
                    // When records left in first block but the second tape is finished.
                    if (!putOnMergeTape(mergeTape, firstTape, firstTapeBlockRecords.get(firstIndex)) && isSortDone) {
                        isSortDone = false;
                    }
                    firstIndex++;

                } else if (secondIndex < secondTapeBlockRecords.size() && isFirstTapeEOF) {
                    // When records left in second block but the first tape is finished.
                    if (!putOnMergeTape(mergeTape, secondTape, secondTapeBlockRecords.get(secondIndex)) && isSortDone) {
                        isSortDone = false;
                    }
                    secondIndex++;

                } else {
                    // When records left on one tape but the second may have more records than in current finished block.
                    // We want to get more data.
                    break;
                }
            }
        }

        return isSortDone ? SortStatus.COMPLETED : SortStatus.NOT_COMPLETED;
    }

    /**
     * @return true if new value was bigger than the previous one
     */
    private static boolean putOnMergeTape(Tape mergeTape, Tape sourceTape, Record record) {
        if (!writeToFile(mergeTape.getFile(), record.formatParametersToFile())) {
            throw new RuntimeException("Didn't placed on tape!");
        }
        Long previousValue = mergeTape.getLastValue();
        mergeTape.setLastValue(record.getValue());
        sourceTape.setLastValue(record.getValue());

        if (previousValue != null) {
            return previousValue <= record.getValue();
        } else {
            return true;
        }
    }

    private static boolean endOfSequence(Tape tape, Record currentRecord) {
        return tape.getLastValue() != null && currentRecord.getValue() < tape.getLastValue();
    }

    public static void clearFile(File recordsFile) {
        try (FileOutputStream fos = new FileOutputStream(recordsFile, false)) {
            fos.getChannel().truncate(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean writeToFile(File recordsFile, String stringToFile) {
        try {
            FileWriter fileWriter = new FileWriter(recordsFile, true);
            fileWriter.write(stringToFile);
            fileWriter.close();

            return true;

        } catch (IOException e) {
            CommunicationManager.say("Writing to file unable.\n" + Arrays.toString(e.getStackTrace()));
            return false;
        }
    }

    public static void printFile(Tape tape) throws IOException {
        CommunicationManager.say("---------------------------------");
        CommunicationManager.say("RECORDS FILE");

        byte[] block = new byte[BLOCK_SIZE_IN_BYTES];
        while (true) {
            int bytesRead = setNextBlock(tape, block);
            if (bytesRead == 0) {
                CommunicationManager.say("Exception in getting next block!");
                break;
            } else if (bytesRead == -1) {
                tape.setPositionInFile(0L);
                break;
            }

            processBlock(
                    block,
                    bytesRead,
                    parameters -> {
                        CommunicationManager.say(new Record(parameters).getValue().toString());
                    }
            );
        }
    }

    private static int setNextBlock(
            Tape tape,
            byte[] block
    ) {
        try (RandomAccessFile raf = new RandomAccessFile(tape.getFile(), "r")) {
            raf.seek(tape.getPositionInFile());

            int bytesRead = raf.read(block);
            tape.setPositionInFile(tape.getPositionInFile() + bytesRead);

            return bytesRead;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static void processBlock(byte[] block, int bytesRead, Consumer<Long[]> actionOnRecordParameters) {
        for (int i = 0; i < bytesRead; i += RECORD_SIZE_IN_BYTES) {
            int endIndex = Math.min(i + RECORD_SIZE_IN_BYTES, bytesRead);
            byte[] record = new byte[endIndex - i];
            System.arraycopy(block, i, record, 0, endIndex - i);

            actionOnRecordParameters.accept(getRecordParameters(record));
        }
    }

    private static Long[] getRecordParameters(byte[] record) {
        Long[] recordParameters = new Long[RECORD_SIZE_IN_BYTES / PARAMETER_SIZE_IN_BYTES];
        int k = 0;
        for (int i = 0; i < RECORD_SIZE_IN_BYTES; i += PARAMETER_SIZE_IN_BYTES) {
            int endIndex = Math.min(i + PARAMETER_SIZE_IN_BYTES, RECORD_SIZE_IN_BYTES);
            byte[] number = new byte[endIndex - i];
            System.arraycopy(record, i, number, 0, endIndex - i);

            try {
                recordParameters[k++] = Long.parseLong(new String(number, 0, PARAMETER_SIZE_IN_BYTES, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return recordParameters;
    }
}
