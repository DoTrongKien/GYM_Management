package com.example.gymmanagement.service;

public class ManaMessageHelper {

    public static String buildMessage(int currentMana, int maxMana) {
        if (currentMana <= 50)
            return "😥 hơi thấp đó, cố gắng lên nhé!";
        if (currentMana <= 100)
            return "💪 thể lực của bạn cũng tốt đó!";
        if (currentMana <= 150)
            return "🔥 điểm thể lực đáng tự hào nha!";
        return "🌟 wow điểm thể lực này khiến tôi bất ngờ đấy!";
    }
}