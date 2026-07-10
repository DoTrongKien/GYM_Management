package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.ChatRequest;
import com.example.gymmanagement.dto.response.ChatMessageResponse;
import com.example.gymmanagement.dto.response.ChatResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.MembershipType;
import com.example.gymmanagement.enums.MuscleGroup;
import com.example.gymmanagement.enums.SessionStatus;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bot tư vấn nội bộ (rule-based): nhận diện ý định của người dùng qua từ khóa
 * và trả lời dựa trên dữ liệu thật của hệ thống (gói tập, bài tập, lịch tập,
 * hồ sơ, membership của user...). Không phụ thuộc dịch vụ LLM bên ngoài.
 */
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FileStorageService fileStorageService;

    // Giá & thời hạn gói tập (đồng bộ với MembershipService)
    private static final Map<MembershipType, Double> PRICES = Map.of(
            MembershipType.FREE, 0.0,
            MembershipType.VIP, 299000.0
    );
    private static final Map<MembershipType, Integer> DURATIONS_MONTHS = Map.of(
            MembershipType.FREE, 1200, // gói free không giới hạn thời gian (100 năm)
            MembershipType.VIP, 1
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Các tin nhắn gợi ý mặc định (hiển thị khi mở khung chat và khi bot không hiểu)
    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Các gói tập và bảng giá?",
            "Gói tập của tôi còn hạn không?",
            "Có những bài tập nào?",
            "Lịch tập sắp tới của tôi?",
            "Chỉ số BMI của tôi?",
            "Tư vấn dinh dưỡng"
    );

    public List<String> getSuggestions() {
        return DEFAULT_SUGGESTIONS;
    }

    /** Trả về lịch sử chat của user (dùng khi mở lại khung chat). */
    public List<ChatMessageResponse> getHistory(String email) {
        User user = getUser(email);
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public void clearHistory(String email) {
        User user = getUser(email);
        chatMessageRepository.deleteByUserId(user.getId());
    }

    @Transactional
    public ChatResponse chat(String email, ChatRequest request) {
        User user = getUser(email);

        String raw = request.getMessage() == null ? "" : request.getMessage().trim();
        if (raw.isEmpty()) {
            throw new RuntimeException("Tin nhắn không được để trống");
        }

        // Lưu tin nhắn của user
        save(user, "USER", raw);

        // Sinh câu trả lời theo ý định
        Answer answer = resolve(user, raw);

        // Lưu tin nhắn của bot
        save(user, "BOT", answer.reply);

        return ChatResponse.builder()
                .reply(answer.reply)
                .suggestions(answer.suggestions)
                .understood(answer.understood)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** User gửi file đính kèm cho bot (kèm chú thích tùy chọn). Bot xác nhận và trả lời theo chú thích nếu có. */
    @Transactional
    public ChatResponse chatAttachment(String email, MultipartFile file, String caption) {
        User user = getUser(email);
        FileStorageService.Stored stored = fileStorageService.store(file, "chatbot");

        String cap = caption != null ? caption.trim() : "";

        // Lưu tin nhắn đính kèm của user
        chatMessageRepository.save(ChatMessage.builder()
                .user(user).sender("USER")
                .content(cap.isEmpty() ? null : cap)
                .attachmentUrl(stored.getUrl())
                .attachmentName(stored.getName())
                .attachmentType(stored.getType())
                .attachmentSize(stored.getSize())
                .createdAt(LocalDateTime.now())
                .build());

        // Bot xác nhận đã nhận file
        String type = stored.getType() != null ? stored.getType() : "";
        String kind = type.startsWith("image/") ? "ảnh 📷"
                : type.startsWith("video/") ? "video 🎬"
                : "tệp “" + stored.getName() + "” 📎";
        StringBuilder reply = new StringBuilder(
                "Mình đã nhận được " + kind + " của bạn! "
                + "Hiện mình chưa thể tự phân tích nội dung file, "
                + "nhưng nếu cần tư vấn về gói tập, bài tập hay dinh dưỡng thì cứ mô tả bằng lời giúp mình nhé. "
                + "Bạn cũng có thể bấm “Nhắn với admin” để gửi file cho admin xem trực tiếp.");

        List<String> suggestions = DEFAULT_SUGGESTIONS;
        // Nếu có chú thích, trả lời luôn theo chú thích
        if (!cap.isEmpty()) {
            Answer a = resolve(user, cap);
            reply.append("\n\n").append(a.reply);
            suggestions = a.suggestions;
        }

        save(user, "BOT", reply.toString());

        return ChatResponse.builder()
                .reply(reply.toString())
                .suggestions(suggestions)
                .understood(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ────────────────────────────────────────────────
    // Nhận diện ý định
    // ────────────────────────────────────────────────
    private Answer resolve(User user, String raw) {
        String t = normalize(raw);

        // Chào hỏi
        if (containsAny(t, "xin chao", "hello", "helu", "chao ban", "chao bot") || t.equals("hi") || t.equals("chao")) {
            return new Answer(
                    "Xin chào " + safeName(user) + "! 👋 Mình là trợ lý của GymPro. "
                            + "Mình có thể giúp bạn về gói tập, bài tập, lịch tập, dinh dưỡng và hồ sơ của bạn. "
                            + "Bạn muốn hỏi gì nào?",
                    DEFAULT_SUGGESTIONS, true);
        }

        // Trợ giúp / bot làm được gì
        if (containsAny(t, "goi y", "ban lam duoc gi", "ban giup duoc gi", "huong dan", "menu", "tro giup", "help", "lam gi")) {
            return new Answer(
                    "Mình có thể trả lời các câu hỏi về:\n"
                            + "• 💳 Gói tập & bảng giá\n"
                            + "• 📌 Gói tập hiện tại của bạn (còn hạn / hết hạn)\n"
                            + "• 🏋️ Bài tập theo nhóm cơ\n"
                            + "• 📅 Lịch tập sắp tới của bạn\n"
                            + "• 📊 Chỉ số BMI & hồ sơ\n"
                            + "• 🥗 Tư vấn dinh dưỡng\n"
                            + "• 🕐 Giờ mở cửa\n"
                            + "Bạn cứ chọn một gợi ý bên dưới hoặc nhập câu hỏi nhé!",
                    DEFAULT_SUGGESTIONS, true);
        }

        // Gói tập của tôi (ưu tiên hơn bảng giá chung)
        if (containsAny(t, "goi cua toi", "goi cua minh", "goi hien tai", "goi cua tui",
                "con han", "het han", "goi dang", "membership cua toi", "toi dang tap goi",
                "goi tap cua toi", "goi tap cua minh")) {
            return myMembershipAnswer(user);
        }

        // Mua / đăng ký / hủy gói
        if (containsAny(t, "mua goi", "dang ky goi", "dang ki goi", "thanh toan", "huy goi",
                "cach mua", "lam sao de mua", "mua goi tap", "gia han")) {
            return new Answer(
                    "Để đăng ký gói tập, bạn vào mục \"Gói tập\" ở menu bên trái, chọn gói mong muốn "
                            + "và thanh toán qua Momo (quét mã QR). Sau khi thanh toán, gói tập sẽ được kích hoạt tự động.\n"
                            + "Nếu muốn hủy gói đang dùng, bạn gửi yêu cầu tại mục \"Gói tập\" — admin sẽ xét duyệt và hoàn tiền nếu hợp lệ. "
                            + "Điều kiện hủy: bạn chưa sử dụng hoặc chưa tạo giáo án với gói đó.",
                    List.of("Các gói tập và bảng giá?", "Gói tập của tôi còn hạn không?"), true);
        }

        // Gói tập & bảng giá
        if (containsAny(t, "goi tap", "bang gia", "gia goi", "cac goi", "gia bao nhieu", "gia tien",
                "bao nhieu tien", "gia ca", "gia la", "co nhung goi", "loai goi") || t.equals("gia") || t.equals("gia?")) {
            return packagesAnswer();
        }

        // Bài tập
        if (containsAny(t, "bai tap", "tap gi", "bai tap nao", "dong tac", "tap nhom co",
                "tap luyen", "tap the", "workout")) {
            return exercisesAnswer(t);
        }

        // Lịch tập / buổi tập
        if (containsAny(t, "lich tap", "buoi tap", "lich hom nay", "lich sap toi", "buoi sap toi",
                "hom nay tap", "khi nao tap", "lich cua toi", "session")) {
            return scheduleAnswer(user);
        }

        // BMI / hồ sơ / chỉ số
        if (containsAny(t, "bmi", "chi so", "ho so", "can nang", "chieu cao", "the trang",
                "muc tieu cua toi", "body", "trang thai co the")) {
            return profileAnswer(user);
        }

        // Dinh dưỡng
        if (containsAny(t, "dinh duong", "an uong", "che do an", "thuc don", "calo", "calories",
                "protein", "an gi", "giam can", "tang can", "an kieng")) {
            return new Answer(
                    "🥗 Về dinh dưỡng: hệ thống có thể tạo thực đơn gợi ý theo mục tiêu của bạn "
                            + "(giảm cân, tăng cơ, giữ dáng...). Bạn vào mục \"Dinh dưỡng\" và bấm tạo thực đơn — "
                            + "hệ thống sẽ tính lượng calo, protein, tinh bột, chất béo phù hợp.\n"
                            + "Gợi ý chung: uống đủ nước, ưu tiên đạm nạc, hạn chế đồ chiên rán & nước ngọt, và ăn đúng giờ nhé!",
                    List.of("Chỉ số BMI của tôi?", "Các gói tập và bảng giá?"), true);
        }

        // Giờ mở cửa / địa chỉ
        if (containsAny(t, "gio mo cua", "may gio", "mo cua", "dong cua", "gio giac", "dia chi",
                "o dau", "cho nao", "lien he", "hotline", "so dien thoai")) {
            return new Answer(
                    "🕐 GymPro mở cửa tất cả các ngày trong tuần:\n"
                            + "• Thứ 2 - Thứ 6: 6h00 - 22h00\n"
                            + "• Thứ 7 - Chủ nhật: 7h00 - 21h00\n"
                            + "Hotline hỗ trợ: 1900 xxxx. Bạn có thể tập bất kỳ khung giờ nào trong thời gian mở cửa nhé!",
                    DEFAULT_SUGGESTIONS, true);
        }

        // Cảm ơn
        if (containsAny(t, "cam on", "thanks", "thank you", "tks", "cam nghia")) {
            return new Answer("Rất vui được hỗ trợ bạn! 💪 Chúc bạn tập luyện hiệu quả nhé. "
                    + "Nếu cần gì thêm cứ hỏi mình!", DEFAULT_SUGGESTIONS, true);
        }

        // Không hiểu → gợi ý lại
        return new Answer(
                "Xin lỗi, mình chưa hiểu rõ câu hỏi của bạn 😅. Mình chỉ hỗ trợ các thông tin về "
                        + "gói tập, bài tập, lịch tập, dinh dưỡng và hồ sơ của bạn. "
                        + "Bạn thử chọn một trong các gợi ý bên dưới nhé:",
                DEFAULT_SUGGESTIONS, false);
    }

    // ────────────────────────────────────────────────
    // Các câu trả lời cụ thể
    // ────────────────────────────────────────────────
    private Answer packagesAnswer() {
        StringBuilder sb = new StringBuilder("🏋️ CÁC GÓI TẬP TẠI GYMPRO:\n");
        for (MembershipType type : MembershipType.values()) {
            sb.append("• ").append(type.name())
                    .append(" — ").append(vnd(PRICES.get(type)))
                    .append(" / ").append(durationText(type)).append("\n");
        }
        sb.append("Bạn muốn đăng ký gói nào? Vào mục \"Gói tập\" để thanh toán qua Momo nhé!");
        return new Answer(sb.toString(),
                List.of("Gói tập của tôi còn hạn không?", "Làm sao để mua gói tập?"), true);
    }

    private Answer myMembershipAnswer(User user) {
        Optional<Membership> opt = membershipRepository.findByUserIdAndIsActiveTrue(user.getId());
        if (opt.isEmpty()) {
            return new Answer(
                    "Hiện bạn chưa có gói tập nào đang hoạt động. Bạn đang dùng gói cơ bản miễn phí. "
                            + "Đăng ký gói tập để mở khóa thêm nhiều tính năng nhé!",
                    List.of("Các gói tập và bảng giá?", "Làm sao để mua gói tập?"), true);
        }
        Membership m = opt.get();
        int daysRemaining = 0;
        if (m.getEndDate() != null) {
            daysRemaining = (int) LocalDate.now().until(m.getEndDate(), java.time.temporal.ChronoUnit.DAYS);
        }
        String status = daysRemaining >= 0
                ? "còn " + daysRemaining + " ngày sử dụng"
                : "đã hết hạn " + Math.abs(daysRemaining) + " ngày trước";
        String reply = "📌 Gói tập hiện tại của bạn:\n"
                + "• Loại gói: " + m.getMembershipType().name() + "\n"
                + "• Ngày bắt đầu: " + fmt(m.getStartDate()) + "\n"
                + "• Ngày hết hạn: " + fmt(m.getEndDate()) + " (" + status + ")\n"
                + "• Thanh toán: " + (m.getPaymentStatus() != null ? m.getPaymentStatus().name() : "-") + "\n"
                + (daysRemaining >= 0 && daysRemaining <= 7
                    ? "⚠️ Gói của bạn sắp hết hạn, hãy gia hạn sớm nhé!"
                    : "Chúc bạn tập luyện hiệu quả! 💪");
        return new Answer(reply, List.of("Các gói tập và bảng giá?", "Lịch tập sắp tới của tôi?"), true);
    }

    private Answer exercisesAnswer(String t) {
        MuscleGroup group = detectMuscleGroup(t);
        if (group != null) {
            List<Exercise> list = exerciseRepository.findByMuscleGroupAndIsActiveTrue(group);
            if (list.isEmpty()) {
                return new Answer("Hiện chưa có bài tập nào cho nhóm cơ " + vnGroup(group)
                        + ". Bạn thử nhóm cơ khác nhé!",
                        List.of("Có những bài tập nào?"), true);
            }
            String names = list.stream().limit(6).map(Exercise::getName).collect(Collectors.joining(", "));
            return new Answer(
                    "🏋️ Một số bài tập cho nhóm cơ " + vnGroup(group) + " (" + list.size() + " bài): "
                            + names + ".\nVào mục \"Bài tập\" để xem hướng dẫn chi tiết và video nhé!",
                    List.of("Bài tập nhóm chân", "Bài tập nhóm lưng", "Lịch tập sắp tới của tôi?"), true);
        }
        long total = exerciseRepository.findByIsActiveTrue().size();
        return new Answer(
                "🏋️ Thư viện hiện có " + total + " bài tập, chia theo các nhóm cơ: "
                        + "ngực, lưng, vai, tay, chân, bụng (core), cardio và toàn thân.\n"
                        + "Bạn muốn xem bài tập cho nhóm cơ nào? Hoặc vào mục \"Bài tập\" để xem tất cả.",
                List.of("Bài tập nhóm ngực", "Bài tập nhóm chân", "Bài tập nhóm bụng"), true);
    }

    private Answer scheduleAnswer(User user) {
        List<WorkoutSession> scheduled = workoutSessionRepository
                .findByUserIdAndStatus(user.getId(), SessionStatus.SCHEDULED)
                .stream()
                .filter(s -> s.getSessionDate() != null)
                .sorted(Comparator.comparing(WorkoutSession::getSessionDate))
                .collect(Collectors.toList());

        if (scheduled.isEmpty()) {
            return new Answer(
                    "Bạn chưa có buổi tập nào được lên lịch. Hãy vào mục \"Giáo án\" để tạo giáo án "
                            + "và xếp lịch cho các buổi tập nhé!",
                    List.of("Có những bài tập nào?", "Chỉ số BMI của tôi?"), true);
        }
        StringBuilder sb = new StringBuilder("📅 Các buổi tập sắp tới của bạn:\n");
        scheduled.stream().limit(5).forEach(s -> {
            String name = s.getCustomSessionName() != null ? s.getCustomSessionName() : "Buổi tập";
            sb.append("• ").append(fmt(s.getSessionDate()))
                    .append(s.getScheduledTime() != null ? " lúc " + s.getScheduledTime() : "")
                    .append(" — ").append(name).append("\n");
        });
        sb.append("Tổng cộng ").append(scheduled.size()).append(" buổi đang chờ. Nhớ check-in đúng giờ nhé!");
        return new Answer(sb.toString(),
                List.of("Chỉ số BMI của tôi?", "Tư vấn dinh dưỡng"), true);
    }

    private Answer profileAnswer(User user) {
        Optional<UserProfile> opt = userProfileRepository.findByUserId(user.getId());
        if (opt.isEmpty() || opt.get().getBmi() == null) {
            return new Answer(
                    "Mình chưa có đủ thông tin hồ sơ của bạn. Hãy vào mục \"Hồ sơ\" và cập nhật "
                            + "chiều cao, cân nặng, mục tiêu... để mình tính chỉ số BMI và tư vấn chính xác hơn nhé!",
                    List.of("Các gói tập và bảng giá?", "Tư vấn dinh dưỡng"), true);
        }
        UserProfile p = opt.get();
        String reply = "📊 Chỉ số cơ thể của bạn:\n"
                + (p.getHeight() != null ? "• Chiều cao: " + p.getHeight() + " cm\n" : "")
                + (p.getWeight() != null ? "• Cân nặng: " + p.getWeight() + " kg\n" : "")
                + "• BMI: " + String.format(Locale.US, "%.1f", p.getBmi()) + " (" + bmiLabel(p.getBmi()) + ")\n"
                + (p.getGoal() != null ? "• Mục tiêu: " + vnGoal(p.getGoal().name()) + "\n" : "")
                + "Bạn có thể cập nhật hồ sơ bất cứ lúc nào ở mục \"Hồ sơ\".";
        return new Answer(reply, List.of("Tư vấn dinh dưỡng", "Lịch tập sắp tới của tôi?"), true);
    }

    // ────────────────────────────────────────────────
    // Tiện ích
    // ────────────────────────────────────────────────
    private MuscleGroup detectMuscleGroup(String t) {
        if (containsAny(t, "nguc", "chest")) return MuscleGroup.CHEST;
        if (containsAny(t, "lung", "back", "xo")) return MuscleGroup.BACK;
        if (containsAny(t, "vai", "shoulder")) return MuscleGroup.SHOULDERS;
        if (containsAny(t, "tay", "bap tay", "arm", "bicep", "tricep")) return MuscleGroup.ARMS;
        if (containsAny(t, "chan", "dui", "mong", "leg")) return MuscleGroup.LEGS;
        if (containsAny(t, "bung", "core", "eo")) return MuscleGroup.CORE;
        if (containsAny(t, "cardio", "tim mach", "chay bo", "dot mo")) return MuscleGroup.CARDIO;
        if (containsAny(t, "toan than", "full body", "toanthan")) return MuscleGroup.FULL_BODY;
        return null;
    }

    private String vnGroup(MuscleGroup g) {
        return switch (g) {
            case CHEST -> "ngực";
            case BACK -> "lưng";
            case SHOULDERS -> "vai";
            case ARMS -> "tay";
            case LEGS -> "chân";
            case CORE -> "bụng (core)";
            case CARDIO -> "cardio";
            case FULL_BODY -> "toàn thân";
        };
    }

    private String vnGoal(String goal) {
        return switch (goal) {
            case "WEIGHT_LOSS" -> "Giảm cân";
            case "MUSCLE_GAIN" -> "Tăng cơ";
            case "ENDURANCE" -> "Tăng sức bền";
            case "FLEXIBILITY" -> "Tăng độ linh hoạt";
            case "MAINTENANCE" -> "Giữ dáng";
            default -> goal;
        };
    }

    private String bmiLabel(Double bmi) {
        if (bmi == null) return "-";
        if (bmi < 18.5) return "thiếu cân";
        if (bmi < 23) return "bình thường";
        if (bmi < 25) return "thừa cân";
        return "béo phì";
    }

    /** Chuẩn hóa: bỏ dấu, chữ thường, đổi đ→d để so khớp từ khóa linh hoạt. */
    private String normalize(String s) {
        String lower = s.toLowerCase(Locale.forLanguageTag("vi"));
        lower = lower.replace('đ', 'd');
        String noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.trim();
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private String vnd(double amount) {
        return String.format(Locale.US, "%,d", (long) amount).replace(',', '.') + "đ";
    }

    // Gói FREE dùng thời hạn 1200 tháng làm mốc "không giới hạn", không hiển thị con số đó
    private String durationText(MembershipType type) {
        return type == MembershipType.FREE ? "không giới hạn"
                : DURATIONS_MONTHS.get(type) + " tháng";
    }

    private String fmt(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "-";
    }

    private String safeName(User user) {
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "bạn";
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void save(User user, String sender, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .user(user)
                .sender(sender)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .sender(m.getSender())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .attachmentName(m.getAttachmentName())
                .attachmentType(m.getAttachmentType())
                .attachmentSize(m.getAttachmentSize())
                .createdAt(m.getCreatedAt())
                .build();
    }

    /** Cấu trúc trung gian gói câu trả lời của bot. */
    private static class Answer {
        final String reply;
        final List<String> suggestions;
        final boolean understood;
        Answer(String reply, List<String> suggestions, boolean understood) {
            this.reply = reply;
            this.suggestions = suggestions;
            this.understood = understood;
        }
    }
}
