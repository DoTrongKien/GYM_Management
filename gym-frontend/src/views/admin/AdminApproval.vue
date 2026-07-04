<template>
  <div class="admin-dashboard-wrapper">
    <!-- Tiêu đề chuẩn font giống ảnh -->
    <h2 class="page-title">DUYỆT YÊU CẦU HỦY GÓI HỘI VIÊN</h2>

    <!-- Bảng danh sách được bọc trong card trắng giống phần Gửi thông báo -->
    <div class="content-card">
      <div class="table-container">
        <table class="gympro-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên Hội viên</th>
              <th>Gói hiện tại</th>
              <th>Ngày yêu cầu</th>
              <th>Lý do hủy</th>
              <th style="text-align: center;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="req in cancellationRequests" :key="req.id">
              <td>#{{ req.id }}</td>
              <td><strong>{{ req.memberName }}</strong></td>
              <td><span class="badge-pkg">{{ req.currentPackageName }}</span></td>
              <td>{{ formatDate(req.requestDate) }}</td>
              <td><span class="reason-text" :title="req.reason">{{ req.reason || 'Không có lý do' }}</span></td>
              <td>
                <div class="action-buttons">
                  <button @click="handleAction(req.id, 'APPROVE')" class="btn-gympro-approve">
                    Duyệt hủy
                  </button>
                  <button @click="handleAction(req.id, 'REJECT')" class="btn-gympro-reject">
                    Từ chối
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="cancellationRequests.length === 0">
              <td colspan="6" class="no-data">Hiện tại không có yêu cầu nào cần duyệt. 🎉</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const cancellationRequests = ref([]);

const fetchRequests = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/admin/membership-cancellations?status=PENDING');
    cancellationRequests.value = response.data;
  } catch (error) {
    console.error("Lỗi khi lấy danh sách yêu cầu:", error);
    // Data mẫu hiển thị test giao diện
    cancellationRequests.value = [
      { id: 1024, memberName: "Nguyễn Văn Anh", currentPackageName: "VIP 6 THÁNG", requestDate: "2026-07-03T10:00:00", reason: "Chuyển địa điểm làm việc sang quận khác" },
      { id: 1025, memberName: "Trần Thị Bình", currentPackageName: "PREMIUM 1 THÁNG", requestDate: "2026-07-03T15:30:00", reason: "Chấn thương cổ chân cần nghỉ ngơi dài hạn" }
    ];
  }
};

const handleAction = async (id, action) => {
  const confirmMsg = action === 'APPROVE' 
    ? "Bạn có chắc chắn muốn DUYỆT yêu cầu hủy này? Hệ thống sẽ tự động hạ cấp về gói cũ hoặc gói BASIC."
    : "Bạn muốn TỪ CHỐI yêu cầu hủy gói này?";

  if (!confirm(confirmMsg)) return;

  try {
    await axios.post(`http://localhost:8080/api/admin/membership-cancellations/${id}/process`, { action });
    alert(action === 'APPROVE' ? "Đã duyệt hủy gói thành công!" : "Đã từ chối yêu cầu.");
    await fetchRequests();
  } catch (error) {
    alert("Thao tác thất bại. Vui lòng kiểm tra kết nối với Backend.");
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

onMounted(() => {
  fetchRequests();
});
</script>

<style scoped>
/* Đồng bộ màu sắc theo phong cách GYMPRO trong ảnh của bạn */
.admin-dashboard-wrapper {
  padding: 5px 10px;
}
.page-title {
  font-size: 22px;
  font-weight: bold;
  color: #2b1b17; /* Màu chữ tiêu đề sẫm */
  margin-bottom: 25px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.content-card {
  background: #ffffff;
  border: 1px solid #ebdcd0; /* Viền cam nhạt đồng bộ */
  border-radius: 8px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(210, 140, 80, 0.05);
}
.table-container {
  overflow-x: auto;
}
.gympro-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.gympro-table th {
  background-color: #fbf6f0; /* Nền header nhẹ nhàng */
  color: #6d4c41; /* Màu nâu của Gympro */
  font-weight: bold;
  padding: 14px 16px;
  border-bottom: 2px solid #ebdcd0;
}
.gympro-table td {
  padding: 16px;
  border-bottom: 1px solid #f5ede6;
  color: #4a3b32;
  font-size: 15px;
}
.gympro-table tr:hover {
  background-color: #fffbf7;
}
.badge-pkg {
  background: #fdf5e6;
  color: #d35400; /* Màu cam thương hiệu */
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 13px;
  border: 1px solid #fadbd8;
}
.reason-text {
  display: block;
  max-width: 250px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #666;
}
.action-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}
/* Nút Duyệt màu Cam Đất giống nút bấm chính trong ảnh của bạn */
.btn-gympro-approve {
  background-color: #d98834; 
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}
.btn-gympro-approve:hover {
  background-color: #be7223;
}
/* Nút Từ chối dạng viền mảnh */
.btn-gympro-reject {
  background-color: transparent;
  color: #e74c3c;
  border: 1px solid #e74c3c;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.2s;
}
.btn-gympro-reject:hover {
  background-color: #fdf2f2;
}
.no-data {
  text-align: center;
  color: #9c8c82;
  padding: 40px 0 !important;
  font-style: italic;
}
</style>