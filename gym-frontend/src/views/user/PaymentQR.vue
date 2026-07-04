<template>
  <div class="payment-wrapper">
    <h2 class="page-title">CHI TIẾT THANH TOÁN ĐƠN HÀNG</h2>

    <div class="payment-container" v-if="invoiceData">
      <!-- KHU VỰC THÔNG TIN HÓA ĐƠN -->
      <div class="info-card">
        <h3 class="card-heading">Hóa đơn #{{ invoiceData.id }}</h3>
        <span :class="['status-badge', invoiceData.status.toLowerCase()]">
          {{ getStatusText(invoiceData.status) }}
        </span>
        <hr class="divider" />
        
        <div class="info-row">
          <span>Gói tập:</span>
          <span class="pkg-name">{{ invoiceData.membershipType }}</span>
        </div>
        <div class="info-row">
          <span>Số tiền:</span>
          <span class="price-text">{{ formatMoney(invoiceData.price) }}</span>
        </div>
        <div class="info-row">
          <span>Ngày tạo:</span>
          <span>{{ formatDate(invoiceData.createdAt) }}</span>
        </div>

        <!-- TH 1 & TH 2.1.1: Đã thanh toán thành công -->
        <div v-if="invoiceData.status === 'PAID'" class="invoice-success-box">
          <p class="success-alert">🎉 Thanh toán thành công! Gói tập đã được kích hoạt.</p>
          <button @click="goBack" class="btn-secondary">Quay lại trang Gói tập</button>
        </div>

        <!-- TH 2: Hết hạn 5 phút (Chưa thanh toán) -->
        <div v-else-if="invoiceData.status === 'EXPIRED'" class="invoice-expired-box">
          <p class="error-alert">⏰ Hóa đơn này đã quá thời gian thanh toán (5 phút).</p>
          <button @click="reCreateQR" class="btn-primary">Tạo lại mã QR mới để thanh toán</button>
        </div>

        <!-- TH 2.1.2: Đang chờ thanh toán bình thường -->
        <div v-else-if="invoiceData.status === 'PENDING'" class="invoice-pending-actions">
          <div class="countdown-box">
            <span>Thời gian còn lại để quét mã: <strong class="time-countdown">{{ countdownText }}</strong></span>
          </div>
          <button @click="cancelPayment" class="btn-cancel">Hủy thanh toán & Giữ nguyên gói cũ</button>
        </div>

        <!-- Không tạo được QR (lỗi hệ thống) -->
        <div v-else-if="invoiceData.status === 'FAILED'" class="invoice-expired-box">
          <p class="error-alert">❌ Không tạo được mã QR thanh toán.</p>
          <p v-if="invoiceData.resultMessage" style="color:#7f8c8d;font-size:13px;margin-bottom:12px">
            Chi tiết: {{ invoiceData.resultMessage }}
          </p>
          <button @click="reCreateQR" class="btn-primary">Thử tạo lại mã QR</button>
        </div>

        <!-- Hóa đơn đã bị hủy -->
        <div v-else-if="invoiceData.status === 'CANCELLED'" class="invoice-expired-box">
          <p class="error-alert">🚫 Hóa đơn này đã bị hủy.</p>
          <button @click="goBack" class="btn-secondary">Quay lại trang Gói tập</button>
        </div>
      </div>

      <!-- KHU VỰC QUÉT MÃ QR (Chỉ hiển thị khi đang PENDING) -->
      <div class="qr-card" v-if="invoiceData.status === 'PENDING'">
        <div class="momo-header">
          <span class="momo-emoji">🏦</span>
          <span class="momo-title">Quét mã VietQR bằng app Ngân hàng hoặc Ví điện tử</span>
        </div>

   <!-- Ảnh QR chuyển khoản VietQR, tự render trên trình duyệt từ chuỗi payload chuẩn EMVCo -->
<div class="qr-border" style="display: flex; justify-content: center; align-items: center; background: white; padding: 10px;">
  <img v-if="qrImageSrc" :src="qrImageSrc" alt="Mã quét QR" style="width: 240px; height: 240px; object-fit: contain;" />
  <div v-else class="qr-fallback">
    <p>Không thể tạo mã QR.</p>
    <button class="btn-primary" @click="reCreateQR">Thử tạo lại mã QR</button>
  </div>
</div>

        <div class="payment-instruction">
          <p>Mở app <strong>Ngân hàng</strong> bất kỳ (hoặc MoMo/ZaloPay) hỗ trợ quét VietQR để chuyển khoản.</p>
          <p class="warning-text">⚠️ Tuyệt đối không sửa đổi Số tiền và Nội dung chuyển khoản (mã hóa đơn) — hệ thống dựa vào đúng nội dung này để tự động xác nhận.</p>
        </div>
        
        <div class="checking-status">
          <div class="spinner"></div>
          <span>Hệ thống đang kiểm tra giao dịch tự động...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { invoiceAPI } from '@/api';
import { ElMessage, ElMessageBox } from 'element-plus';
import QRCode from 'qrcode';

const route = useRoute();
const router = useRouter();

const invoiceData = ref(null);
const countdownText = ref('05:00');
let timeLeft = 300; // 5 phút mặc định
let timer = null;
let statusInterval = null;

// Tạo ảnh QR NGAY TRÊN TRÌNH DUYỆT (không gọi ra server ngoài internet để lấy ảnh),
// nên không bao giờ bị vỡ ảnh do mạng/firewall chặn. Ưu tiên qrRawPayload (VietQR chuyển khoản
// ngân hàng), phòng khi không có thì fallback về payUrl (luồng MoMo cũ, nếu còn dùng).
const qrDataUrl = ref('');

watch(
  () => invoiceData.value?.qrRawPayload || invoiceData.value?.payUrl,
  async (qrSource) => {
    if (!qrSource) { qrDataUrl.value = ''; return; }
    try {
      qrDataUrl.value = await QRCode.toDataURL(qrSource, { width: 240, margin: 1 });
    } catch (e) {
      console.error('Không tạo được ảnh QR (local):', e);
      qrDataUrl.value = '';
    }
  },
  { immediate: true }
);

// Nếu vì lý do gì đó không tạo được QR local thì vẫn dùng ảnh QR từ backend (VietQR API) làm phương án cuối
const qrImageSrc = computed(() => qrDataUrl.value || invoiceData.value?.qrCodeUrl || '');

// Dùng đúng invoiceAPI (đã gắn JWT token qua interceptor trong @/api) — KHÔNG dùng axios gốc,
// vì /api/invoices/** yêu cầu đăng nhập, gọi axios gốc sẽ luôn bị 401.
const fetchInvoice = async () => {
  try {
    const response = await invoiceAPI.getOne(route.params.invoiceId);
    // response = ApiResponse { success, message, data: InvoiceResponse } -> lấy .data
    invoiceData.value = response.data;

    if (invoiceData.value.status === 'PENDING') {
      timeLeft = invoiceData.value.secondsRemaining || 300;
      startCountdown();
      startCheckingStatus();
    }
  } catch (error) {
    console.error('Không lấy được hóa đơn từ Backend:', error);
    ElMessage.error('Không tìm thấy hóa đơn này hoặc bạn không có quyền xem. Vui lòng quay lại trang Gói tập và thử lại.');
  }
};

// 2. Đếm ngược thời gian thanh toán
const startCountdown = () => {
  clearInterval(timer);
  timer = setInterval(() => {
    if (timeLeft > 0) {
      timeLeft--;
      const min = Math.floor(timeLeft / 60).toString().padStart(2, '0');
      const sec = (timeLeft % 60).toString().padStart(2, '0');
      countdownText.value = `${min}:${sec}`;
    } else {
      clearInterval(timer);
      clearInterval(statusInterval);
      invoiceData.value.status = 'EXPIRED';
    }
  }, 1000);
};

// 3. Liên tục gọi API kiểm tra trạng thái thanh toán từ Backend
const startCheckingStatus = () => {
  clearInterval(statusInterval);
  statusInterval = setInterval(async () => {
    try {
      const response = await invoiceAPI.getOne(invoiceData.value.id);
      const latest = response.data;
      if (latest.status === 'PAID') {
        clearInterval(statusInterval);
        clearInterval(timer);
        invoiceData.value = latest;
        ElMessage.success('Thanh toán thành công! Gói tập đã được kích hoạt.');
      } else if (latest.status === 'EXPIRED' || latest.status === 'FAILED') {
        clearInterval(statusInterval);
        clearInterval(timer);
        invoiceData.value = latest;
      }
    } catch (e) {
      console.log('Đang kiểm tra trạng thái thanh toán tự động...');
    }
  }, 3000); // 3 giây kiểm tra một lần
};

// Hủy thanh toán, giữ nguyên gói cũ
const cancelPayment = async () => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn hủy thanh toán hóa đơn này?', 'Xác nhận', { type: 'warning' });
    await invoiceAPI.cancel(invoiceData.value.id);
    ElMessage.success('Đã hủy thanh toán. Gói tập cũ của bạn được giữ nguyên.');
    router.push('/app/membership');
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Có lỗi xảy ra khi hủy hóa đơn.');
  }
};

// Trường hợp bấm tạo lại khi hóa đơn cũ hết hạn
const reCreateQR = async () => {
  try {
    const response = await invoiceAPI.regenerateQr(invoiceData.value.id);
    invoiceData.value = response.data;
    timeLeft = invoiceData.value.secondsRemaining || 300;
    startCountdown();
    startCheckingStatus();
  } catch (e) {
    ElMessage.error('Không thể tạo lại mã QR mới.');
  }
};

const goBack = () => router.push('/app/membership');
const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
const formatDate = (str) => new Date(str).toLocaleString('vi-VN');
const getStatusText = (status) => {
  if (status === 'PENDING') return 'Chờ thanh toán';
  if (status === 'PAID') return 'Đã thanh toán';
  if (status === 'EXPIRED') return 'Hết hạn (Chưa TT)';
  if (status === 'FAILED') return 'Thất bại';
  if (status === 'CANCELLED') return 'Đã hủy';
  return status;
};

onMounted(() => { fetchInvoice(); });
onUnmounted(() => { clearInterval(timer); clearInterval(statusInterval); });
</script>

<style scoped>
.payment-wrapper { padding: 10px; }
.page-title { font-size: 22px; font-weight: bold; color: #2b1b17; margin-bottom: 25px; }
.payment-container { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }
.info-card, .qr-card { background: white; border: 1px solid #ebdcd0; border-radius: 8px; padding: 25px; box-shadow: 0 2px 12px rgba(210,140,80,0.05); }
.card-heading { margin: 0 0 10px 0; color: #6d4c41; font-size: 20px; }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: bold; margin-bottom: 15px; text-transform: uppercase; }
.status-badge.pending { background: #fef9e7; color: #f39c12; }
.status-badge.paid { background: #e8f8f5; color: #2ecc71; }
.status-badge.expired { background: #fadbd8; color: #e74c3c; }
.status-badge.failed { background: #f2f3f4; color: #7f8c8d; }
.divider { border: 0; border-top: 1px solid #ebdcd0; margin: 15px 0; }
.info-row { display: flex; justify-content: space-between; margin-bottom: 15px; font-size: 15px; }
.pkg-name { font-weight: bold; color: #2c3e50; }
.price-text { color: #d98834; font-weight: bold; font-size: 18px; }
.countdown-box { background: #fffbf7; border: 1px dashed #d98834; padding: 12px; border-radius: 6px; text-align: center; margin-bottom: 15px; }
.time-countdown { color: #e74c3c; font-size: 16px; }

.btn-primary { background: #d98834; color: white; border: none; padding: 12px; border-radius: 6px; width: 100%; font-weight: bold; cursor: pointer; }
.btn-secondary { background: #7f8c8d; color: white; border: none; padding: 12px; border-radius: 6px; width: 100%; font-weight: bold; cursor: pointer; }
.btn-cancel { background: transparent; border: 1px solid #e74c3c; color: #e74c3c; padding: 12px; border-radius: 6px; width: 100%; font-weight: bold; cursor: pointer; }
.btn-cancel:hover { background: #fdf2f2; }

.invoice-success-box, .invoice-expired-box { text-align: center; margin-top: 20px; }
.success-alert { color: #27ae60; font-weight: bold; margin-bottom: 15px; }
.error-alert { color: #c0392b; font-weight: bold; margin-bottom: 15px; }

/* QR Code Section */
.qr-card { display: flex; flex-direction: column; align-items: center; justify-content: center; }
.momo-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.momo-emoji { font-size: 28px; }
.momo-title { font-weight: bold; color: #a50064; font-size: 16px; }
.qr-border { border: 1px solid #ebdcd0; padding: 15px; background: white; border-radius: 8px; }
.qr-fallback { text-align: center; padding: 30px; color: #7f8c8d; }
.qr-fallback .btn-primary { margin-top: 12px; }
.payment-instruction { text-align: center; margin-top: 15px; font-size: 14px; color: #555; }
.warning-text { color: #c0392b; font-weight: bold; margin-top: 5px; }
.checking-status { display: flex; align-items: center; gap: 10px; margin-top: 20px; color: #7f8c8d; font-size: 13px; }
.spinner { width: 16px; height: 16px; border: 2px solid #f3f3f3; border-top: 2px solid #d98834; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
</style>