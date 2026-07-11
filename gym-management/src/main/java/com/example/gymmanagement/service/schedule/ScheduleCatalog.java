package com.example.gymmanagement.service.schedule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Danh mục các lịch tập khuyến nghị (candidate schedules) theo số buổi/tuần.
 * KHÔNG còn phụ thuộc Goal — theo I.docx mục 8.2, lịch tập chỉ phụ thuộc sessionsPerWeek.
 *
 * Dùng cho:
 *  - Gợi ý lịch khi tạo giáo án AI (WorkoutPlanService.buildPlanDaysNew - lấy candidate đầu tiên
 *    làm lịch mặc định hiển thị ban đầu).
 *  - Thuật toán loại trừ dần (Day Mismatch Detection) ở WorkoutSessionService.
 *  - API /confirm-schedule để validate lựa chọn của người dùng.
 */
public final class ScheduleCatalog {

    private ScheduleCatalog() {}

    // 1=Monday ... 7=Sunday (ISO dayOfWeek)
    private static final Map<Integer, List<List<Integer>>> CANDIDATES = new HashMap<>();
    static {
        CANDIDATES.put(2, List.of(
                List.of(1, 4),   // Monday - Thursday
                List.of(2, 5),   // Tuesday - Friday
                List.of(3, 6)    // Wednesday - Saturday
        ));
        CANDIDATES.put(3, List.of(
                List.of(1, 3, 5),   // Monday-Wednesday-Friday
                List.of(2, 4, 6)    // Tuesday-Thursday-Saturday
        ));
        CANDIDATES.put(4, List.of(
                List.of(1, 2, 4, 5),   // Monday-Tuesday-Thursday-Friday
                List.of(2, 3, 5, 6)    // Tuesday-Wednesday-Friday-Saturday
        ));
        CANDIDATES.put(5, List.of(
                List.of(1, 2, 3, 5, 6) // Monday-Tuesday-Wednesday-Friday-Saturday
        ));
        CANDIDATES.put(6, List.of(
                List.of(1, 2, 3, 4, 5, 6) // Monday-Tuesday-Wednesday-Thursday-Friday-Saturday
        ));
    }

    /** Trả về TẤT CẢ lịch khả dĩ cho 1 số buổi/tuần cho trước. */
    public static List<List<Integer>> candidatesFor(int sessions) {
        List<List<Integer>> result = CANDIDATES.get(sessions);
        if (result == null) {
            throw new IllegalStateException("Chưa khai báo lịch tập cho sessionsPerWeek=" + sessions
                    + " — thêm vào ScheduleCatalog.CANDIDATES trước khi dùng.");
        }
        return result;
    }

    /** Kiểm tra 1 danh sách dayOfWeek có khớp (không phân biệt thứ tự) với 1 candidate hợp lệ hay không. */
    public static Optional<List<Integer>> matchCandidate(int sessions, List<Integer> dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek.isEmpty()) return Optional.empty();
        Set<Integer> asSet = new HashSet<>(dayOfWeek);
        return candidatesFor(sessions).stream()
                .filter(c -> c.size() == dayOfWeek.size() && new HashSet<>(c).equals(asSet))
                .findFirst();
    }

    /** "1,3,5" -> [1,3,5] */
    public static List<Integer> parse(String s) {
        if (s == null || s.isBlank()) return null;
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /** [1,3,5] -> "1,3,5" */
    public static String format(List<Integer> dows) {
        return dows.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}