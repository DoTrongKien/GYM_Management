package com.example.gymmanagement.enums;

public enum PaymentStatus {
    PENDING,    // Hóa đơn vừa tạo, đang chờ user quét QR thanh toán
    PAID,       // Thanh toán thành công (MoMo IPN xác nhận)
    EXPIRED,    // Quá 5 phút không thanh toán, QR hết hạn
    CANCELLED,  // User chủ động hủy hóa đơn khi đang ở trạng thái chờ/hết hạn
    FAILED,     // MoMo trả về lỗi giao dịch
    REFUNDED    // Đã hoàn tiền sau khi hủy gói đã thanh toán
}