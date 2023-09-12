import lombok.Getter;
import lombok.Setter;

import java.io.File;

public class Tape {
    @Getter private final File file;
    @Getter
    @Setter
    private Long lastValue;
    @Getter
    @Setter
    private Long positionInFile = 0L;
    @Getter
    private final TapeType type;

    public Tape(String tapePath, TapeType type) {
        file = new File(tapePath);
        this.type = type;
    }
}
