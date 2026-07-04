package com.example.gymmanagement.enums;

public enum CancellationStatus {
    PENDING_REVIEW, // User đã gửi yêu cầu hủy, chờ admin duyệt
    APPROVED,       // Admin đồng ý hủy + hoàn tiền
    REJECTED        // Admin từ chối hủy
}