import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Record {
    private Long[] parameters;
    private Long value;

    public Record(Long[] parameters) {
        this.parameters = parameters;
        value = calculateValue(
                parameters[0],
                parameters[1],
                parameters[2],
                parameters[3],
                parameters[4],
                parameters[5]
        );
    }

    public String formatParametersToFile() {
        StringBuilder sb = new StringBuilder();
        for (Long parameter : parameters) {
            sb.append(String.format("%0" + RecordsGenerator.FIXED_DIGITS_NUMBER + "d", parameter));
        }

        return sb.toString();
    }

    public static Long calculateValue(Long a0, Long a1, Long a2, Long a3, Long a4, Long x) {
        // g(x) = a0 + a1x + a2x^2 + a3x^3 + a4x^4
        return (long) (a0 + a1 * Math.pow(x, 1) + a2 * Math.pow(x, 2) + a3 * Math.pow(x, 3) + a4 * Math.pow(x, 4));
    }

}
