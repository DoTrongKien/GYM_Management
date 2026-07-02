package com.example.gymmanagement.service;

import com.example.gymmanagement.enums.Goal;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class Workoutplanhelper {
    public List<String> suggestDays(Goal goal, int sessions) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (sessions) {
                case 2 -> List.of("Monday", "Thursday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                case 4 -> List.of("Monday", "Tuesday", "Thursday", "Friday");
                case 5 -> List.of("Monday", "Tuesday", "Wednesday", "Friday", "Saturday");
                default -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
            };
            case WEIGHT_LOSS -> switch (sessions) {
                case 2 -> List.of("Monday", "Wednesday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                case 4 -> List.of("Monday", "Tuesday", "Thursday", "Friday");
                case 5 -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
                default -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
            };
            case ENDURANCE -> switch (sessions) {
                case 2 -> List.of("Tuesday", "Friday");
                case 3 -> List.of("Tuesday", "Thursday", "Saturday");
                case 4 -> List.of("Monday", "Wednesday", "Friday", "Sunday");
                default -> List.of("Monday", "Tuesday", "Thursday", "Friday", "Saturday");
            };
            default -> switch (sessions) {
                case 2 -> List.of("Monday", "Thursday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                default -> List.of("Monday", "Wednesday", "Friday", "Sunday");
            };
        };
    }

    /**
     * Map tên ngày tiếng Anh → ISO dow (1=Monday...7=Sunday).
     */
    public int dayNameToIsoDow(String dayName) {
        return switch (dayName) {
            case "Monday"    -> 1;
            case "Tuesday"   -> 2;
            case "Wednesday" -> 3;
            case "Thursday"  -> 4;
            case "Friday"    -> 5;
            case "Saturday"  -> 6;
            case "Sunday"    -> 7;
            default          -> -1;
        };
    }

}
