package com.example.gymmanagement.enums;

public enum SupportStatus {
    PENDING,   // User đã gửi yêu cầu, chờ admin xác nhận
    ACTIVE,    // Admin đã chấp nhận, đang chat 1:1
    REJECTED,  // Admin từ chối yêu cầu
    CLOSED     // Phiên chat đã kết thúc
}
