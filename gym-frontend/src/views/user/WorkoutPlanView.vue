<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN TẬP</h2>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <el-button v-if="plan" @click="allPlansDialog=true" plain>📋 Tất cả giáo án</el-button>
        <el-button @click="openGoalDialog" type="primary">✨ {{ plan ? 'Tạo lại' : 'Tạo giáo án' }}</el-button>
      </div>
    </div>

    <div v-if="!plan && !loading" class="empty-plan">
      <div style="font-size:4rem;margin-bottom:16px">🤖</div>
      <h3 class="display" style="font-size:1.8rem;color:var(--c-text);margin-bottom:8px">CHƯA CÓ GIÁO ÁN</h3>
      <p style="color:var(--c-text2);margin-bottom:20px;max-width:440px;margin-left:auto;margin-right:auto">
        Hệ thống sẽ tự động chọn bài tập phù hợp nhất theo mục tiêu của bạn, dựa trên chỉ số benefit và chỉ số hồ sơ cá nhân (BMI, cân nặng). Hoặc bạn có thể chọn một giáo án mẫu do phòng tập thiết kế sẵn.
      </p>
      <el-button type="primary" size="large" @click="openGoalDialog">✨ Chọn mục tiêu & Tạo giáo án</el-button>
    </div>

    <div v-if="loading" style="padding:40px 0">
      <el-skeleton :rows="8" animated style="background:var(--c-card);padding:24px;border-radius:12px"/>
    </div>

    <template v-if="plan && !loading">
      <el-card style="margin-bottom:24px;border-left:4px solid var(--c-accent)">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px">
          <div>
            <div class="display" style="font-size:1.6rem;color:var(--c-text)">{{ plan.planName }}</div>
            <div style="color:var(--c-text2);margin:4px 0 10px;font-size:0.875rem">{{ plan.description }}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px">
              <el-tag type="warning">{{ goalLabel(plan.goal) }}</el-tag>
              <el-tag type="info">{{ levelLabel(plan.targetLevel) }}</el-tag>
              <el-tag type="danger">Tuần {{ plan.currentWeek }} / {{ plan.durationWeeks }}</el-tag>
              <el-tag>{{ plan.sessionsPerWeek }} buổi/tuần</el-tag>
              <el-tag v-if="plan.isAiGenerated" type="success">✨ Giáo án cá nhân hóa </el-tag>
              <el-tag v-else-if="!plan.isAiGenerated" type="warning" effect="plain">📋 Giáo án mẫu</el-tag>
            </div>

          </div>
          <el-button type="primary" plain size="small" @click="openGoalDialog">
            🔄 Đổi mục tiêu
          </el-button>
        </div>
      </el-card>

      <el-card v-if="weekProgress" class="progress-panel" style="margin-bottom:24px;">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px">
          <div>
            <span style="font-weight:700;font-size:1.1rem;color:var(--c-text)">
              📊 Tiến độ Tuần {{ plan.currentWeek }}:
            </span>
            <span style="margin-left:8px;color:var(--c-accent);font-weight:700">
              {{ weekProgress.completed }} / {{ weekProgress.target }} Buổi hoàn thành
            </span>
            <div style="font-size:0.85rem;color:var(--c-text2);margin-top:4px" v-if="weekProgress.avgCompletionRate">
              Tỉ lệ hoàn thành trung bình bài tập: <strong>{{ Math.round(weekProgress.avgCompletionRate) }}%</strong>
            </div>
          </div>

          <div v-if="weekProgress.canGoNextWeek" style="color:#16a34a;font-size:0.9rem;font-weight:600">
            ✅ Đã hoàn thành tuần này! Giáo án đã tự động căn chỉnh và chuyển sang tuần tiếp theo.
          </div>
          <div v-else-if="weekProgress.isWeekDone" style="color:var(--c-warning);font-size:0.9rem;font-weight:600">
            ⚠ Bạn cần hoàn thành Checkout buổi cuối tuần để nộp số liệu trước khi chuyển tuần!
          </div>
        </div>
      </el-card>

      <!-- SỬA: suggestedDays giờ là mảng NHIỀU lịch khả dĩ (number[][], ISO dayOfWeek),
           không còn 1 lịch cố định theo tên ngày tiếng Anh như trước -->
      <div v-if="plan.suggestedDays && plan.suggestedDays.length" class="suggested-days-box">
        <div style="font-weight:700;font-size:0.9rem;margin-bottom:10px">📅 Các lịch tập khuyến nghị:</div>
        <div v-for="(option, idx) in plan.suggestedDays" :key="idx" style="margin-bottom:10px">
          <div style="font-size:0.78rem;color:var(--c-text3);margin-bottom:4px">Lịch {{ scheduleLabel(idx) }}</div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-tag v-for="d in option" :key="d" effect="plain" type="success">
              {{ dowVietName(d) }}
            </el-tag>
          </div>
        </div>
        <div style="font-size:0.78rem;color:var(--c-text3);margin-top:4px">
          Hệ thống sẽ tự nhận diện lịch bạn đang theo dựa trên các buổi check-in thực tế.
        </div>
      </div>

      <!-- Plan từ template: ngày tập cố định do admin set, map dayOfWeek → tên Việt -->
      <div v-else-if="!plan.isAiGenerated" class="suggested-days-box">
        <div style="font-weight:700;font-size:0.9rem;margin-bottom:6px">📅 Ngày tập theo lịch Admin:</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-tag v-for="day in plan.planDays" :key="day.id" effect="plain" type="warning">
            {{ dowVietName(day.dayOfWeek) }}
          </el-tag>
        </div>
      </div>

      <!-- Thanh Mana (thể lực) -->
      <div v-if="plan.maxMana" class="mana-box">
        <div style="display:flex;justify-content:space-between;margin-bottom:6px">
          <span style="font-weight:700">⚡ Thể lực</span>
          <span style="font-weight:700">{{ plan.currentMana }}/{{ plan.maxMana }}</span>
        </div>
        <div class="mana-bar-track">
          <div class="mana-bar-fill" :style="{ width: (plan.currentMana / plan.maxMana * 100) + '%' }"></div>
        </div>
        <div style="margin-top:6px;font-size:0.85rem">{{ plan.manaMessage }}</div>
      </div>

      <div class="days-grid">
        <el-card
            v-for="(day, index) in plan.planDays"
            :key="day.id"
            class="day-card"
            :class="{ disabled: weekProgress?.isWeekDone, 'session-completed': day.sessionStatus === 'COMPLETED' }"
        >
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span class="display accent" style="font-size:1.1rem">Buổi {{ index + 1 }}</span>
              <div>
                <el-tag v-if="day.sessionStatus === 'SCHEDULED'" type="info" size="small">⏳ Chờ tập</el-tag>
                <el-tag v-else-if="day.sessionStatus === 'CHECKED_IN'" type="danger" size="small">🏃 Đang tập</el-tag>
                <el-tag v-else-if="day.sessionStatus === 'COMPLETED'" type="success" size="small">✅ Hoàn thành</el-tag>
                <span v-else style="font-size:0.75rem;color:var(--c-text3)">{{ day.exercises?.length || 0 }} bài</span>
              </div>
            </div>
          </template>

          <div class="schedule-section">
            <!-- Check-in trực tiếp theo thời gian thực, dùng chung cho cả plan template và plan AI -->
            <div v-if="day.sessionStatus === 'NOT_SCHEDULED' || !day.sessionId" class="no-schedule">
              <el-button type="success" size="small" @click="handleDirectCheckIn(day, index + 1)">
                🏃 Check-in ngay
              </el-button>
            </div>
            <div v-else-if="day.sessionStatus === 'CHECKED_IN'" class="scheduled" style="background: #fef2f2; border: 1px dashed #fca5a5; padding: 6px; border-radius: 4px;">
              <div class="date-display" style="color: #dc2626; font-weight: 700; font-size: 0.85rem">
                🔥 Đang trong buổi tập...
              </div>
              <el-button type="danger" size="small" @click="openCheckOutDialog(day, index + 1)">Check-out 🏁</el-button>
            </div>
            <div v-else-if="day.sessionStatus === 'COMPLETED'" class="completed-zone">
              ✨ Hoàn thành vào {{ fmtDate(day.scheduledDate) }}
              <span v-if="day.completionRate !== null"> (Đạt {{ day.completionRate }}%)</span>
            </div>
          </div>

          <div class="exercise-list">
            <div v-for="ex in day.exercises" :key="ex.id" class="ex-row" @click="openExDetail(ex)">
              <div class="ex-info">
                <div class="ex-name">{{ ex.exerciseName }}</div>
                <div class="ex-sub">{{ muscleLabel(ex.muscleGroup) }} · {{ diffLabel(ex.difficulty) }}</div>
                <div v-if="ex.notes" class="ex-note">{{ ex.notes }}</div>
              </div>
              <div class="ex-meta">
                <div class="ex-sets">
                  <span v-if="ex.reps">{{ ex.sets }}×{{ ex.reps }}</span>
                  <span v-else-if="ex.durationSeconds">{{ ex.sets }}×{{ ex.durationSeconds }}s</span>
                </div>
                <div v-if="ex.restSeconds" style="font-size:0.7rem;color:var(--c-text3)">nghỉ {{ ex.restSeconds }}s</div>
              </div>
              <el-icon style="color:var(--c-text3);font-size:12px;flex-shrink:0"><ArrowRight/></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- ===================== DIALOG TẠO GIÁO ÁN (AI hoặc Giáo án mẫu) ===================== -->
    <el-dialog v-model="goalDialog" title="TẠO GIÁO ÁN" width="560px" align-center>
      <el-tabs v-model="createTab">

        <el-tab-pane label="✨ Tạo Giáo Án Cá Nhân Hóa" name="ai">
          <div style="margin-bottom:20px">
            <div style="font-weight:700;color:var(--c-text);margin-bottom:12px">🎯 Chọn mục tiêu chính</div>
            <div class="goal-grid">
              <div
                  v-for="g in goals" :key="g.value"
                  class="goal-card"
                  :class="{selected: genForm.goal === g.value}"
                  @click="handleGoalSelect(g.value)"
              >
                <div class="goal-icon">{{ g.icon }}</div>
                <div class="goal-label">{{ g.label }}</div>
                <div class="goal-desc">{{ g.desc }}</div>
              </div>
            </div>
          </div>

          <el-divider/>

          <div style="margin-bottom:16px">
            <div style="font-weight:700;color:var(--c-text);margin-bottom:10px">⚙️ Tuỳ chỉnh nâng cao</div>
            <div class="grid-2">
              <el-form-item label="Trình độ">
                <el-select v-model="genForm.fitnessLevel" placeholder="Tự lấy từ Hồ sơ (BMI)" clearable style="width:100%">
                  <el-option label="🌱 Mới bắt đầu" value="BEGINNER"/>
                  <el-option label="🔄 Trung bình" value="INTERMEDIATE"/>
                  <el-option label="⚡ Nâng cao" value="ADVANCED"/>
                </el-select>
              </el-form-item>
              <el-form-item :label="'Số ngày/tuần (' + minDaysRequired + '-' + maxDaysRequired + ')'">
                <el-select v-model="genForm.daysPerWeek" placeholder="Tự lấy từ Hồ sơ" clearable style="width:100%">
                  <el-option v-for="d in validDaysOptions" :key="d" :label="d + ' ngày'" :value="d"/>
                </el-select>
              </el-form-item>
            </div>
          </div>

          <div class="info-box" v-if="genForm.goal">
            <div style="font-weight:700;margin-bottom:6px;color:var(--c-accent)">
              {{ goals.find(g=>g.value===genForm.goal)?.icon }} {{ goals.find(g=>g.value===genForm.goal)?.label }}
            </div>
            <div style="font-size:0.82rem;color:var(--c-text2)">
              {{ goals.find(g=>g.value===genForm.goal)?.aiNote }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="📋 Chọn giáo án mẫu" name="template">
          <div v-if="loadingTemplates" style="padding:20px 0">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="!templates.length" style="text-align:center;padding:30px;color:var(--c-text3)">
            Hiện chưa có giáo án mẫu nào từ phòng tập.
          </div>
          <div v-else class="template-list">
            <div
              v-for="t in templates" :key="t.id"
              class="template-card"
              :class="{selected: selectedTemplateId === t.id}"
              @click="selectedTemplateId = t.id"
            >
              <div style="display:flex;justify-content:space-between;align-items:flex-start">
                <div>
                  <div style="font-weight:700;color:var(--c-text)">{{ t.planName }}</div>
                  <div style="font-size:0.8rem;color:var(--c-text2);margin-top:2px">{{ t.description }}</div>
                </div>
                <el-tag size="small">{{ t.sessionsPerWeek }} buổi/tuần</el-tag>
              </div>
              <div style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap">
                <el-tag type="warning" size="small">{{ goalLabel(t.goal) }}</el-tag>
                <el-tag type="info" size="small">{{ levelLabel(t.targetLevel) }}</el-tag>
                <el-tag size="small">{{ t.durationWeeks }} tuần</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>

      <template #footer>
        <el-button @click="goalDialog=false">Hủy</el-button>

        <el-button
          v-if="createTab === 'ai'"
          type="primary" @click="generateWithGoal"
          :loading="generating" :disabled="!genForm.goal"
        >
          ✨ KHỞI TẠO GIÁO ÁN
        </el-button>

        <el-button
          v-else
          type="primary" @click="applyTemplate"
          :loading="applyingTemplate" :disabled="!selectedTemplateId"
        >
          ✅ ÁP DỤNG GIÁO ÁN NÀY
        </el-button>
      </template>
    </el-dialog>

    <!-- ===================== DIALOG CHECK-OUT (THEO TỪNG BÀI TẬP) ===================== -->
    <el-dialog v-model="checkOutDialog" title="🏁 CHECK-OUT HOÀN THÀNH BUỔI TẬP" width="520px" align-center>
      <div style="margin-bottom:14px; font-weight:600; color:var(--c-text)">
        Buổi {{ selectedDayNumber }} - Tuần {{ plan?.currentWeek }}
      </div>

      <div v-for="ex in checkoutExercises" :key="ex.exerciseId" class="checkout-ex-row">
        <div style="font-weight:600;margin-bottom:8px">{{ ex.exerciseName }}</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-button
            v-for="pct in [0,25,50,75,100]" :key="pct"
            :type="coForm.logs[ex.exerciseId]?.completionPercent === pct ? 'primary' : ''"
            size="small"
            @click="setPercent(ex.exerciseId, pct)"
          >
            {{ pct }}%
          </el-button>
        </div>
      </div>

      <div v-if="coForm.isLastSessionOfWeek" class="body-progress-box">
        <h4 style="margin:0 0 10px 0; color:#b45309">⚖ CẬP NHẬT SỐ LIỆU CƠ THỂ (Buổi Cuối Tuần)</h4>
        <p style="font-size:0.8rem; margin:0 0 12px 0; color:#d97706">
          Đây là buổi tập cuối cùng trong tuần này. Vui lòng nhập cân nặng hiện tại để có thể cập nhật chỉ số cơ thể.
        </p>
        <div style="display:flex; flex-direction:column; gap:12px">
          <el-form-item label="Cân nặng hiện tại (kg) *" required style="margin-bottom:0">
            <el-input-number v-model="coForm.checkoutWeight" :min="30" :max="300" :precision="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="Tỉ lệ mỡ (%) - Body Fat" style="margin-bottom:0">
            <el-input-number v-model="coForm.checkoutBodyFat" :min="2" :max="60" :precision="1" style="width:100%" />
          </el-form-item>
        </div>
      </div>

      <template #footer>
        <el-button @click="checkOutDialog=false">Hủy</el-button>
        <el-button
          type="primary"
          @click="submitCheckOut"
          :loading="checkingOut"
        >
          {{ coForm.isLastSessionOfWeek && plan?.isAiGenerated ? '✅ Hoàn thành và căn chỉnh bài tập' : 'Hoàn thành buổi tập' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===================== DIALOG CHI TIẾT BÀI TẬP ===================== -->
    <el-dialog v-model="exDetailDialog" :title="selEx?.exerciseName" width="540px" align-center v-if="selEx">
      <div v-if="selEx.videoUrl" class="video-wrap">
        <iframe :src="ytEmbed(selEx.videoUrl)" frameborder="0" allowfullscreen
                style="width:100%;height:260px;border-radius:8px"/>
      </div>
      <div v-else class="no-video">📹 Chưa có video hướng dẫn</div>
      <el-descriptions :column="2" border size="small" style="margin-top:14px">
        <el-descriptions-item label="Nhóm cơ">{{ muscleLabel(selEx.muscleGroup) }}</el-descriptions-item>
        <el-descriptions-item label="Độ khó">
          <span class="badge" :class="diffBadge(selEx.difficulty)">{{ diffLabel(selEx.difficulty) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Sets">{{ selEx.sets }}</el-descriptions-item>
        <el-descriptions-item label="Reps / Thời gian">
          <span v-if="selEx.reps">{{ selEx.reps }} reps</span>
          <span v-else>{{ selEx.durationSeconds }}s</span>
        </el-descriptions-item>
        <el-descriptions-item label="Nghỉ giữa set">{{ selEx.restSeconds || '--' }}s</el-descriptions-item>
        <el-descriptions-item label="Calories/set">{{ selEx.caloriesBurned || '--' }} kcal</el-descriptions-item>
        <el-descriptions-item label="Ghi chú" :span="2" v-if="selEx.notes">
          {{ selEx.notes }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- Hướng dẫn xác định tạ -->
      <div class="weight-guide-box">
        📏 <strong>Cách xác định tạ:</strong> chọn 1 mức tạ mà bạn tập đến set thứ 12 thì không thể tập tiếp được nữa.
        Với bài không dùng tạ, lấy cân nặng cơ thể làm chuẩn (có thể thêm dây kháng lực để tăng/giảm khối lượng).
      </div>

      <!-- Chưa có tạ khởi điểm -> cho nhập (chỉ 1 lần) -->
      <div v-if="!selEx.baseWeightKg" style="margin-top:14px">
        <el-form-item label="Nhập mức tạ khởi điểm (kg)">
          <el-input-number v-model="baseWeightInput" :min="0" :max="500" :precision="1" style="width:100%"/>
        </el-form-item>
        <el-button type="primary" @click="saveBaseWeight" :loading="savingWeight">Lưu tạ khởi điểm</el-button>
      </div>

      <!-- Hiển thị tạ hiện tại trực tiếp, không cần bấm mở -->
      <div class="weight-reveal">
        ⚖️ Tạ áp dụng tuần này: <strong>{{ selEx.currentWeightKg }} kg</strong>
        <span v-if="selEx.weightJustRevealed && selEx.currentWeightKg > selEx.baseWeightKg" style="color:#16a34a"> (tăng so với tuần trước 📈)</span>
        <span v-else-if="selEx.weightJustRevealed && selEx.currentWeightKg < selEx.baseWeightKg" style="color:#dc2626"> (giảm so với tuần trước 📉)</span>
      </div>

      <template #footer>
        <el-button @click="exDetailDialog=false">Đóng</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="allPlansDialog" title="TẤT CẢ GIÁO ÁN" width="600px" align-center>
      <div v-if="!allPlans.length" class="empty-state">Chưa có giáo án nào</div>
      <div v-else class="plans-list">
        <div v-for="p in allPlans" :key="p.id" class="plan-item" :class="{active:p.isActive}">
          <div style="flex:1">
            <div style="font-weight:700;color:var(--c-text)">{{ p.planName }}</div>
            <div style="font-size:0.8rem;color:var(--c-text3);margin-top:2px">
              {{ goalLabel(p.goal) }} · {{ levelLabel(p.targetLevel) }} · {{ p.durationWeeks }} tuần
            </div>
          </div>
          <div style="display:flex;gap:6px;align-items:center">
            <el-tag v-if="p.isActive" type="success" size="small">Đang chạy</el-tag>
            <el-tag v-if="p.isAiGenerated" size="small">✨ AI</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="allPlansDialog=false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- ===================== MỚI: DIALOG BẮT BUỘC CHỌN LỊCH TẬP (mục 8.3) =====================
         Hiện khi backend không còn xác định được lịch tập chuẩn nào phù hợp với lịch sử
         check-in (scheduleSelectionRequired=true). Không có nút Hủy/đóng — người dùng
         BẮT BUỘC chọn 1 trong các lịch khuyến nghị rồi xác nhận mới tiếp tục được. -->
    <el-dialog
        v-model="scheduleSelectionDialog"
        title="⚠️ CHỌN LẠI LỊCH TẬP CHUẨN"
        width="480px"
        align-center
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
    >
      <p style="color:var(--c-text2);font-size:0.9rem;margin-bottom:16px">
        Hệ thống không thể xác định lịch tập chuẩn của bạn từ các buổi tập gần đây (có thể do bạn tập lệch ngày nhiều lần).
        Vui lòng chọn lại 1 trong các lịch khuyến nghị dưới đây để hệ thống tiếp tục theo dõi đúng chu kỳ.
      </p>
      <div style="display:flex;flex-direction:column;gap:10px">
        <div
            v-for="(option, idx) in scheduleOptionsList" :key="idx"
            class="schedule-option-card"
            :class="{selected: selectedScheduleIndex === idx}"
            @click="selectedScheduleIndex = idx"
        >
          <div style="font-size:0.78rem;color:var(--c-text3);margin-bottom:6px">Lịch {{ scheduleLabel(idx) }}</div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-tag v-for="d in option" :key="d" effect="plain" type="success">
              {{ dowVietName(d) }}
            </el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button
            type="primary"
            @click="confirmScheduleSelection"
            :loading="confirmingSchedule"
            :disabled="selectedScheduleIndex === null"
        >
          Xác nhận lịch tập này
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { planAPI, sessionAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const plan = ref(null)
const allPlans = ref([])
const weekProgress = ref(null)
const activeSessions = ref([])
const loading = ref(true)

const generating = ref(false)
const checkingOut = ref(false)
const adjusting = ref(false)

const goalDialog = ref(false)
const allPlansDialog = ref(false)
const exDetailDialog = ref(false)
const checkOutDialog = ref(false)
const adjustWeekDialog = ref(false)

const selEx = ref(null)
const selectedDayNumber = ref(null)
const checkoutSessionId = ref(null)

// === state cho dialog tạo giáo án có 2 tab (AI / Giáo án mẫu) ===
const createTab = ref('ai')
const templates = ref([])
const loadingTemplates = ref(false)
const selectedTemplateId = ref(null)
const applyingTemplate = ref(false)

// === state cho checkout theo từng bài tập ===
const checkoutExercises = ref([])

// === state cho nhập tạ khởi điểm ===
const baseWeightInput = ref(null)
const savingWeight = ref(false)

// === MỚI: state cho dialog bắt buộc chọn lại lịch tập (mục 8.3) ===
const scheduleSelectionDialog = ref(false)
const scheduleOptionsList = ref([])
const selectedScheduleIndex = ref(null)
const confirmingSchedule = ref(false)

const genForm = reactive({
  goal: '',
  fitnessLevel: null,
  daysPerWeek: null
})

const adjustForm = reactive({
  newWeight: null,
  newBodyFat: null
})

// === coForm.logs thay cho completionRate tổng ===
const coForm = reactive({
  logs: {}, // { [exerciseId]: { completionPercent, weightUsedKg } }
  checkoutWeight: null,
  checkoutBodyFat: null,
  notes: '',
  isLastSessionOfWeek: false
})

// SỬA: desc cập nhật đúng khoảng min-max theo mục 4 I.docx (không còn chỉ nói "tối thiểu")
const goals = [
  { value: 'MUSCLE_GAIN', icon: '💪', label: 'Tăng cơ / Sức mạnh', desc: 'Yêu cầu 4-6 buổi/tuần', aiNote: 'ưu tiên bài tập compound nặng, tăng Sets, hạ Reps. Phân bổ cách ngày để phục hồi cơ.' },
  { value: 'WEIGHT_LOSS', icon: '🔥', label: 'Giảm cân / Đốt mỡ', desc: 'Yêu cầu 4-6 buổi/tuần', aiNote: 'ưu tiên Cardio/HIIT, tăng lượng Reps, giảm thời gian nghỉ. Sắp xếp chu kỳ tập liên tục.' },
  { value: 'ENDURANCE', icon: '🏃', label: 'Tăng sức bền', desc: 'Yêu cầu 3-5 buổi/tuần', aiNote: 'chọn Cardio và Full Body thời gian dài, cường độ vừa, xen kẽ phục hồi tim mạch.' },
  { value: 'FLEXIBILITY', icon: '🤸', label: 'Tăng linh hoạt', desc: 'Yêu cầu 2-4 buổi/tuần', aiNote: 'ưu tiên các bài Yoga kéo giãn cơ, giữ thế lâu, giải tỏa căng cơ.' },
  { value: 'MAINTENANCE', icon: '⚖️', label: 'Duy trì thể hình', desc: 'Yêu cầu 3-5 buổi/tuần', aiNote: 'cân bằng đều giữa các nhóm cơ chính với cấu trúc Set/Rep tiêu chuẩn.' }
]

// SỬA: thêm maxDaysRequired, khớp calcSessionsPerWeek mới ở backend (mục 4 I.docx)
const minDaysRequired = computed(() => {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') return 4
  if (genForm.goal === 'ENDURANCE' || genForm.goal === 'MAINTENANCE') return 3
  if (genForm.goal === 'FLEXIBILITY') return 2
  return 2
})

const maxDaysRequired = computed(() => {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') return 6
  if (genForm.goal === 'ENDURANCE' || genForm.goal === 'MAINTENANCE') return 5
  if (genForm.goal === 'FLEXIBILITY') return 4
  return 6
})

const validDaysOptions = computed(() => {
  const opts = []
  for (let d = minDaysRequired.value; d <= maxDaysRequired.value; d++) opts.push(d)
  return opts
})

function handleGoalSelect(goalValue) {
  genForm.goal = goalValue
  if (genForm.daysPerWeek && genForm.daysPerWeek < minDaysRequired.value) {
    genForm.daysPerWeek = minDaysRequired.value
  }
  if (genForm.daysPerWeek && genForm.daysPerWeek > maxDaysRequired.value) {
    genForm.daysPerWeek = maxDaysRequired.value
  }
}

// ====================== LOAD DATA ======================
async function load() {
  loading.value = true
  try {
    const [act, all, sess] = await Promise.all([
      planAPI.getActive().catch(() => ({ data: null })),
      planAPI.getAll(),
      sessionAPI.getAll()
    ])

    plan.value = act.data
    allPlans.value = all.data || []
    activeSessions.value = sess.data || []

    if (plan.value) {
      const progressRes = await sessionAPI.getWeekProgress(plan.value.id, plan.value.currentWeek)
      weekProgress.value = progressRes.data

      plan.value.planDays.forEach(day => {
        const standardSession = activeSessions.value.find(s =>
            s.weekNumber === plan.value.currentWeek &&
            s.planId === plan.value.id &&
            s.dayName === day.dayName
        )
        if (standardSession) {
          day.sessionId = standardSession.id
          day.sessionStatus = standardSession.status
          day.scheduledDate = standardSession.sessionDate
          day.scheduledTime = standardSession.scheduledTime
          day.completionRate = standardSession.completionRate
        } else {
          day.sessionId = null
          day.sessionStatus = 'NOT_SCHEDULED'
          day.scheduledDate = ''
          day.scheduledTime = ''
          day.completionRate = null
        }
      })

      // MỚI: nếu có buổi tập nào của giáo án hiện tại đang yêu cầu chọn lại lịch
      // (mục 8.3) — hiện dialog bắt buộc chọn ngay khi vào trang.
      const planSessions = activeSessions.value.filter(s => s.planId === plan.value.id)
      const needsSelection = planSessions.find(s => s.scheduleSelectionRequired)
      checkScheduleSelection(needsSelection)
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

// ====================== MỞ DIALOG TẠO GIÁO ÁN ======================
function openGoalDialog() {
  goalDialog.value = true
  createTab.value = 'ai'
  selectedTemplateId.value = null
  loadTemplates()
}

async function loadTemplates() {
  loadingTemplates.value = true
  try {
    const res = await planAPI.getTemplates()
    templates.value = res.data || []
  } catch (e) {
    // im lặng, không chặn tab AI nếu API template lỗi
  } finally {
    loadingTemplates.value = false
  }
}

// ====================== GENERATE PLAN (AI) ======================
async function generateWithGoal() {
  if (!genForm.goal) {
    ElMessage.warning('Hãy chọn mục tiêu')
    return
  }
  generating.value = true
  try {
    const payload = {
      goal: genForm.goal,
      fitnessLevel: genForm.fitnessLevel || null,
      daysPerWeek: genForm.daysPerWeek || null
    }
    const r = await planAPI.generateWithGoal(payload)
    plan.value = r.data
    goalDialog.value = false
    ElMessage.success('Giáo án thích ứng đã khởi tạo thành công! 🎉')
    genForm.goal = ''
    genForm.fitnessLevel = null
    genForm.daysPerWeek = null
    await load()
  } catch (err) {
    ElMessage.error('Tạo giáo án thất bại')
  } finally {
    generating.value = false
  }
}

// ====================== CHỌN GIÁO ÁN MẪU ======================
async function applyTemplate() {
  if (!selectedTemplateId.value) return
  applyingTemplate.value = true
  try {
    const r = await planAPI.selectTemplate(selectedTemplateId.value)
    plan.value = r.data
    goalDialog.value = false
    selectedTemplateId.value = null
    ElMessage.success('Đã áp dụng giáo án mẫu thành công! 🎉')
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Áp dụng giáo án mẫu thất bại')
  } finally {
    applyingTemplate.value = false
  }
}

// === mở checkout dialog, load danh sách bài tập của buổi đó ===
function openCheckOutDialog(day, dayNumber) {
  checkoutSessionId.value = day.sessionId
  selectedDayNumber.value = dayNumber
  checkoutExercises.value = day.exercises || []

  coForm.logs = {}
  checkoutExercises.value.forEach(ex => {
    coForm.logs[ex.exerciseId] = { completionPercent: null, weightUsedKg: null }
  })
  coForm.notes = ''
  coForm.checkoutWeight = null
  coForm.checkoutBodyFat = null
  coForm.isLastSessionOfWeek = (dayNumber === plan.value.sessionsPerWeek)
  checkOutDialog.value = true
}

// === chọn tỉ lệ hoàn thành cho 1 bài tập ===
function setPercent(exerciseId, pct) {
  coForm.logs[exerciseId].completionPercent = pct
}

async function submitCheckOut() {
  const missing = Object.values(coForm.logs).some(l => l.completionPercent === null)
  if (missing) {
    ElMessage.warning('Vui lòng chọn tỉ lệ hoàn thành cho tất cả bài tập!')
    return
  }
  if (coForm.isLastSessionOfWeek && !coForm.checkoutWeight) {
    ElMessage.warning('Vui lòng nhập cân nặng để cập nhật chỉ số cơ thể!')
    return
  }

  const exerciseLogs = Object.entries(coForm.logs).map(([exerciseId, v]) => ({
    exerciseId: Number(exerciseId),
    completionPercent: v.completionPercent,
    weightUsedKg: v.weightUsedKg
  }))

  checkingOut.value = true
  try {
    const r = await sessionAPI.complete(checkoutSessionId.value, {
      checkoutWeight: coForm.checkoutWeight,
      checkoutBodyFat: coForm.checkoutBodyFat,
      notes: coForm.notes,
      exerciseLogs
    })

    // Cảnh báo chấn thương nếu vượt mana
    if (r.data?.injuryRisk) {
      ElMessageBox.alert(
        'Bạn đã tập vượt quá thể lực hiện có. Nguy cơ chấn thương — hãy nghỉ ngơi trước khi tập tiếp!',
        '⚠️ Cảnh báo chấn thương',
        { type: 'warning' }
      )
    }

    const isLastAi = coForm.isLastSessionOfWeek && plan.value?.isAiGenerated
    ElMessage.success(isLastAi ? 'Hoàn thành tuần tập! Giáo án đã được căn chỉnh 🎉' : 'Hoàn thành buổi tập! 🎉')
    if (r.data?.dayMismatchWarning) {
      ElMessage({ message: r.data.dayMismatchWarning, type: 'warning', duration: 8000 })
    }
    checkOutDialog.value = false

    // MỚI: nếu buổi vừa checkout yêu cầu chọn lại lịch tập, mở dialog bắt buộc
    checkScheduleSelection(r.data)
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Hoàn thành buổi tập thất bại')
  } finally {
    checkingOut.value = false
  }
}

// ====================== ADJUST WEEK ======================
function openAdjustWeekDialog() {
  adjustForm.newWeight = plan.value?.startingWeight || null
  adjustForm.newBodyFat = null
  adjustWeekDialog.value = true
}

async function submitAdjustWeek() {
  if (!adjustForm.newWeight) {
    ElMessage.warning('Vui lòng nhập cân nặng hiện tại')
    return
  }

  adjusting.value = true
  try {
    const apiResponse = await planAPI.adjustWeek(plan.value.id, {
      newWeight: adjustForm.newWeight,
      newBodyFat: adjustForm.newBodyFat
    })

    const planData = apiResponse.data
    const adjustNote = planData?.scheduleNote || 'Giáo án đã được cập nhật cho tuần mới.'

    if (adjustNote.includes('📉') || adjustNote.includes('quá sức')) {
      ElMessage({
        message: adjustNote,
        type: 'warning',
        duration: 9000,
        dangerouslyUseHTMLString: true
      })
    } else if (adjustNote.includes('🚀') || adjustNote.includes('🔥')) {
      ElMessage({
        message: adjustNote,
        type: 'success',
        duration: 9000,
        dangerouslyUseHTMLString: true
      })
    } else {
      ElMessage.success(adjustNote)
    }

    adjustWeekDialog.value = false
    await load()

  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Có lỗi xảy ra khi điều chỉnh giáo án')
  } finally {
    adjusting.value = false
  }
}

// ====================== UTILITY FUNCTIONS ======================
function fmtDate(d) {
  return d ? dayjs(d).format('DD/MM/YYYY') : ''
}

// === mở dialog chi tiết bài tập, reset state tạ ===
function openExDetail(ex) {
  selEx.value = ex
  baseWeightInput.value = null
  exDetailDialog.value = true
}

// === lưu tạ khởi điểm (chỉ 1 lần) ===
async function saveBaseWeight() {
  if (!baseWeightInput.value) {
    ElMessage.warning('Nhập mức tạ khởi điểm')
    return
  }
  savingWeight.value = true
  try {
    await planAPI.setBaseWeight(selEx.value.id, { weight: baseWeightInput.value })
    ElMessage.success('Đã lưu tạ khởi điểm!')
    exDetailDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu tạ thất bại')
  } finally {
    savingWeight.value = false
  }
}

function ytEmbed(url) {
  const m = (url || '').match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return m ? `https://www.youtube.com/embed/${m[1]}` : url
}

function goalLabel(g) {
  return {
    WEIGHT_LOSS: '🔥 Giảm cân',
    MUSCLE_GAIN: '💪 Tăng cơ',
    ENDURANCE: '🏃 Sức bền',
    FLEXIBILITY: '🤸 Linh hoạt',
    MAINTENANCE: '⚖️ Duy trì'
  }[g] || g
}

function levelLabel(l) {
  return {
    BEGINNER: 'Starter (Mới bắt đầu)',
    INTERMEDIATE: 'Progress (Trung bình)',
    ADVANCED: 'Elite (Nâng cao)'
  }[l] || l
}

function muscleLabel(m) {
  return {
    CHEST: 'Ngực', BACK: 'Lưng', SHOULDERS: 'Vai', ARMS: 'Tay',
    LEGS: 'Chân', CORE: 'Cơ lõi', CARDIO: 'Cardio', FULL_BODY: 'Toàn thân'
  }[m] || m
}

function diffLabel(d) {
  return { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' }[d] || d
}

function diffBadge(d) {
  return { EASY: 'badge-success', MEDIUM: 'badge-warning', HARD: 'badge-danger' }[d] || ''
}

// SỬA: suggestedDays và scheduleOptions giờ LUÔN là số ISO dayOfWeek (1=Thứ Hai...7=Chủ Nhật),
// bỏ nhánh map theo tên tiếng Anh (không còn dùng tới).
function dowVietName(day) {
  const map = {
    1: 'Thứ Hai',
    2: 'Thứ Ba',
    3: 'Thứ Tư',
    4: 'Thứ Năm',
    5: 'Thứ Sáu',
    6: 'Thứ Bảy',
    7: 'Chủ Nhật'
  }
  return map[day] || day
}

// MỚI: label A, B, C... cho từng lịch khuyến nghị
function scheduleLabel(idx) {
  return String.fromCharCode(65 + idx)
}

// Check-in trực tiếp cho cả plan template lẫn plan AI (không cần lên lịch trước)
async function handleDirectCheckIn(day, dayNumber) {
  const today = dayjs().format('YYYY-MM-DD')
  const nowTime = dayjs().format('HH:mm:ss')
  const isLast = (dayNumber === plan.value.sessionsPerWeek)

  try {
    const enrollRes = await sessionAPI.enroll({
      planDayId: day.id,
      sessionDate: today,
      scheduledTime: nowTime,
      weekNumber: plan.value.currentWeek,
      isLastSessionOfWeek: isLast
    })
    const sessionId = enrollRes.data?.id
    if (!sessionId) throw new Error('Không lấy được session id')

    await performCheckIn(sessionId)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Check-in thất bại')
  }
}

// Check-in qua đúng luồng 2 bước của backend.
// Lần gọi đầu confirmReducedIntensity=false — nếu mana không đủ, backend trả
// requiresConfirmation=true (CHƯA check-in thật) kèm warningMessage.
// Nếu người dùng chọn "vẫn muốn tập", gọi lại chính hàm này với true.
// LƯU Ý: theo thiết kế mana mới, xác nhận này KHÔNG còn làm giảm Set/Rep — mana chỉ
// còn đại diện cho khả năng hồi phục.
async function performCheckIn(sessionId, confirmReducedIntensity = false) {
  try {
    const r = await sessionAPI.checkIn(sessionId, confirmReducedIntensity)
    const result = r.data

    if (result?.requiresConfirmation) {
      try {
        await ElMessageBox.confirm(
          result.warningMessage || 'Thể lực hiện tại không đủ để hồi phục tối ưu sau buổi tập.',
          '⚠️ Cảnh báo thể lực',
          {
            confirmButtonText: 'Vẫn tiếp tục tập',
            cancelButtonText: 'Nghỉ ngơi',
            type: 'warning'
          }
        )
        // Người dùng chọn "vẫn muốn tập" -> check-in thật, Set/Rep giữ nguyên
        await performCheckIn(sessionId, true)
      } catch {
        // Người dùng chọn "nghỉ ngơi" -> không làm gì thêm
        ElMessage.info('Đã hủy check-in. Hãy nghỉ ngơi để hồi phục thể lực nhé 💤')
      }
      return
    }

    // Check-in thành công thật sự
    ElMessage.success('Check-in thành công! Hãy tập luyện hết mình 💪')
    // dayMismatchWarning nằm trong result.session
    if (result?.session?.dayMismatchWarning) {
      ElMessage({ message: result.session.dayMismatchWarning, type: 'warning', duration: 8000 })
    }
    // MỚI: kiểm tra xem buổi vừa check-in có yêu cầu chọn lại lịch tập không (mục 8.3)
    checkScheduleSelection(result?.session)
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Check-in thất bại')
  }
}

// ====================== MỚI: Chọn lại lịch tập chuẩn (mục 8.3) ======================
// Kiểm tra 1 WorkoutSessionResponse bất kỳ (từ check-in, check-out, hoặc danh sách sessions)
// có đang yêu cầu chọn lại lịch không; nếu có thì mở dialog bắt buộc chọn.
function checkScheduleSelection(sessionObj) {
  if (sessionObj?.scheduleSelectionRequired && sessionObj?.scheduleOptions?.length) {
    scheduleOptionsList.value = sessionObj.scheduleOptions
    selectedScheduleIndex.value = null
    scheduleSelectionDialog.value = true
    return true
  }
  return false
}

async function confirmScheduleSelection() {
  if (selectedScheduleIndex.value === null) {
    ElMessage.warning('Vui lòng chọn 1 lịch tập')
    return
  }
  if (!plan.value?.id) return

  confirmingSchedule.value = true
  try {
    const dayOfWeek = scheduleOptionsList.value[selectedScheduleIndex.value]
    const r = await planAPI.confirmSchedule(plan.value.id, dayOfWeek)
    plan.value = r.data
    ElMessage.success('Đã lưu lịch tập chuẩn!')
    scheduleSelectionDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu lịch tập thất bại')
  } finally {
    confirmingSchedule.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.empty-plan {
  text-align:center; padding:80px 40px;
  background:var(--c-card); border-radius:var(--radius-lg); box-shadow:var(--shadow);
}
.backend-note {
  margin-top: 10px; padding: 10px 14px; background: #eef5fe;
  border-left: 4px solid #409eff; color: #409eff; font-size: 0.85rem; border-radius: 4px;
}
.progress-panel {
  background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px;
}
.suggested-days-box {
  background: var(--c-card); padding: 14px; border-radius: 8px; margin-bottom: 20px; border: 1px dashed var(--c-border);
}
.weight-adjustment-box {
  background: #fff7ed; color: #c2410c; padding: 12px 14px; border-radius: 8px;
  margin-bottom: 20px; border: 1px solid #fed7aa; font-size: 0.875rem;
}
.body-progress-box {
  background: #fffbeb; border: 1px dashed #fef3c7; padding: 14px; border-radius: 8px; margin: 16px 0;
}
.goal-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(140px,1fr)); gap:10px; }
.goal-card {
  border:2px solid var(--c-border2); border-radius:var(--radius-lg); padding:14px 10px;
  text-align:center; cursor:pointer; transition:all var(--transition); background:var(--c-card2);
}
.goal-card:hover  { border-color:var(--c-accent); }
.goal-card.selected { border-color:var(--c-accent); background:#FFF8F0; }
.goal-icon  { font-size:1.8rem; margin-bottom:6px; }
.goal-label { font-weight:700; font-size:0.82rem; color:var(--c-text); margin-bottom:3px; }
.goal-desc  { font-size:0.72rem; color:var(--c-text3); }

.info-box {
  padding:12px 14px; background:#FFF8F0; border:1px solid var(--c-border); border-radius:var(--radius-lg); margin-top:12px;
}

.template-list { display:flex; flex-direction:column; gap:10px; max-height:360px; overflow-y:auto; }
.template-card {
  border:2px solid var(--c-border2); border-radius:var(--radius-lg);
  padding:12px 14px; cursor:pointer; transition:all var(--transition); background:var(--c-card2);
}
.template-card:hover { border-color:var(--c-accent); }
.template-card.selected { border-color:var(--c-accent); background:#FFF8F0; }

.grid-2 { display:grid; grid-template-columns:1fr 1fr; gap:16px; }

.schedule-section { margin: 12px 0; padding: 10px 12px; background: var(--c-card2); border-radius: var(--radius); }
.no-schedule { display: flex; justify-content: center; padding: 2px 0; }
.scheduled { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.completed-zone { background: #f0fdf4; border: 1px solid #bbf7d0; color: #16a34a; font-weight: 500; text-align: center; padding: 6px; border-radius: 4px; font-size: 0.85rem; }
.time-badge { background: var(--c-accent); color: white; padding: 2px 6px; font-size: 0.75rem; border-radius: 4px; margin-right: 6px; }
.date-display { font-size: 0.9rem; color: var(--c-text); }

.days-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(300px,1fr)); gap:16px; margin-top:16px; }
.day-card.disabled { opacity: 0.85; }
.session-completed { border-top: 3px solid #16a34a; }
.exercise-list { display:flex; flex-direction:column; gap:6px; }
.ex-row {
  display:flex; align-items:center; gap:10px; padding:8px 10px; background:var(--c-card2); border-radius:var(--radius);
  cursor:pointer; transition:background var(--transition);
}
.ex-row:hover { background:#EDE0D0; }
.ex-info { flex:1; min-width:0; }
.ex-name { font-size:0.875rem; font-weight:600; color:var(--c-text); }
.ex-sub  { font-size:0.72rem; color:var(--c-text3); margin-top:1px; }
.ex-note { font-size:0.7rem; color:var(--c-accent); margin-top:2px; }
.ex-meta { text-align:right; flex-shrink:0; }
.ex-sets { font-size:0.82rem; color:var(--c-accent); font-family:var(--font-mono); font-weight:700; }

.plans-list { display:flex; flex-direction:column; gap:10px; max-height:400px; overflow-y:auto; }
.plan-item {
  display:flex; align-items:center; gap:12px; padding:12px 14px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition:border-color var(--transition);
}
.plan-item.active { border-color:var(--c-accent); }
.video-wrap { border-radius:8px; overflow:hidden; }
.no-video { text-align:center; padding:20px; color:var(--c-text3); background:var(--c-card2); border-radius:8px; }

.mana-box {
  background:var(--c-card); padding:14px; border-radius:8px; margin-bottom:20px;
}
.mana-bar-track {
  height:14px; background:#e2e8f0; border-radius:7px; overflow:hidden;
}
.mana-bar-fill {
  height:100%; background:linear-gradient(90deg,#22c55e,#4ade80); transition:width .4s;
}
.checkout-ex-row {
  padding:12px 0; border-bottom:1px solid var(--c-border2);
}
.weight-guide-box {
  margin-top:14px; padding:10px 12px; background:#eef5fe; border-left:4px solid #409eff;
  font-size:0.82rem; border-radius:4px;
}
.weight-reveal {
  margin-top:16px; padding:14px; background:#f0fdf4; border-radius:8px; font-size:0.95rem;
}

/* MỚI: dialog chọn lại lịch tập */
.schedule-option-card {
  border:2px solid var(--c-border2); border-radius:var(--radius-lg);
  padding:12px 14px; cursor:pointer; transition:all var(--transition); background:var(--c-card2);
}
.schedule-option-card:hover { border-color:var(--c-accent); }
.schedule-option-card.selected { border-color:var(--c-accent); background:#FFF8F0; }
</style>