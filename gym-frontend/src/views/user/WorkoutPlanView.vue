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
              <el-tag v-if="plan.isAiGenerated" type="success">✨ Hệ Thống AI</el-tag>
              <el-tag v-else-if="!plan.isAiGenerated" type="warning" effect="plain">📋 Giáo án mẫu</el-tag>
            </div>
            <div v-if="plan.scheduleNote" class="backend-note">
              💡 <strong>Phân tích từ AI:</strong> {{ plan.scheduleNote }}
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

          <el-button
              v-if="weekProgress.canGoNextWeek"
              type="success"
              size="large"
              @click="openAdjustWeekDialog"
          >
            🚀 Sang Tuần Tiếp Theo (Căn Chỉnh AI)
          </el-button>
          <div v-else-if="weekProgress.isWeekDone" style="color:var(--c-warning);font-size:0.9rem;font-weight:600">
            ⚠ Bạn cần hoàn thành Checkout buổi cuối tuần để nộp số liệu trước khi chuyển tuần!
          </div>
        </div>
      </el-card>

      <div v-if="plan.suggestedDays && plan.suggestedDays.length" class="suggested-days-box">
        <div style="font-weight:700;font-size:0.9rem;margin-bottom:6px">📅 Gợi ý ngày tập tối ưu từ AI:</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-tag v-for="d in plan.suggestedDays" :key="d" effect="plain" type="success">
            {{ d }}
          </el-tag>
        </div>
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
            <div v-if="day.sessionStatus === 'NOT_SCHEDULED' || !day.scheduledDate" class="no-schedule">
              <el-button type="primary" plain size="small" @click="openScheduleDialog(day, index + 1)">
                📅 Lên lịch buổi tập
              </el-button>
            </div>

            <div v-else-if="day.sessionStatus === 'SCHEDULED'" class="scheduled">
              <div class="date-display">
                <span class="time-badge">W{{ plan.currentWeek }}</span>
                <strong>{{ fmtDate(day.scheduledDate) }}</strong>
                <span v-if="day.scheduledTime"> • {{ day.scheduledTime.substring(0, 5) }}</span>
              </div>
              <div style="display:flex;gap:4px">
                <el-button type="primary" link size="small" @click="openScheduleDialog(day, index + 1)">Sửa</el-button>
                <el-button type="success" size="small" @click="handleCheckIn(day.sessionId)">Check-in 🏃</el-button>
              </div>
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

        <el-tab-pane label="✨ AI tự tạo theo mục tiêu" name="ai">
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
              <el-form-item :label="'Số ngày/tuần (Min ' + minDaysRequired + ')'">
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

    <el-dialog v-model="scheduleDialog" title="CHỌN NGÀY & GIỜ TẬP THEO CHU KỲ" width="440px" align-center>
      <el-form :model="schedForm" label-position="top">
        <el-form-item label="Buổi tập cấu hình">
          <strong>Buổi {{ selectedDayNumber }} trên tổng số {{ plan?.sessionsPerWeek }} buổi tuần này</strong>
        </el-form-item>

        <div v-if="anchorDateDisplay" class="anchor-info-box">
          📌 Buổi tập mốc đầu tiên: <strong>{{ anchorDateDisplay }}</strong>.<br/>
          Ràng buộc chu kỳ 7 ngày: Chỉ được phép chọn từ ngày <strong>{{ anchorDateDisplay }}</strong> đến ngày <strong>{{ maxDateDisplay }}</strong>.
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px">
          <el-form-item label="Ngày tập">
            <el-date-picker
                v-model="schedForm.sessionDate"
                type="date"
                format="DD/MM/YYYY"
                value-format="YYYY-MM-DD"
                style="width:100%"
                :disabled-date="disableInvalidDates"
                placeholder="Chọn ngày tập"
            />
          </el-form-item>
          <el-form-item label="Giờ bắt đầu">
            <el-time-picker
                v-model="schedForm.scheduledTime"
                format="HH:mm"
                value-format="HH:mm:ss"
                placeholder="06:00"
                style="width:100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialog=false">Hủy</el-button>
        <el-button type="primary" @click="saveSchedule" :loading="saving">Lưu lịch tập</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkOutDialog" title="🏁 CHECK-OUT HOÀN THÀNH BUỔI TẬP" width="460px" align-center>
      <el-form :model="coForm" label-position="top">
        <div style="margin-bottom:14px; font-weight:600; color:var(--c-text)">
          Buổi {{ selectedDayNumber }} - Tuần {{ plan?.currentWeek }}
        </div>

        <el-form-item label="Tỉ lệ hoàn thành của buổi tập (%)" required>
          <div style="display:flex; align-items:center; gap:12px; width:100%">
            <el-slider v-model="coForm.completionRate" :min="0" :max="100" style="flex:1" />
            <span style="font-weight:700; width:45px; text-align:right">{{ coForm.completionRate }}%</span>
          </div>
        </el-form-item>

        <div v-if="coForm.isLastSessionOfWeek" class="body-progress-box">
          <h4 style="margin:0 0 10px 0; color:#b45309">⚖ CẬP NHẬT SỐ LIỆU CƠ THỂ (Buổi Cuối Tuần)</h4>
          <p style="font-size:0.8rem; margin:0 0 12px 0; color:#d97706">
            Đây là buổi tập cuối cùng trong tuần này. Vui lòng nhập cân nặng hiện tại để AI cấu trúc lại độ khó giáo án.
          </p>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px">
            <el-form-item label="Cân nặng hiện tại (kg) *" required>
              <el-input-number v-model="coForm.checkoutWeight" :min="30" :max="300" :precision="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="Tỉ lệ mỡ (%) - Body Fat">
              <el-input-number v-model="coForm.checkoutBodyFat" :min="2" :max="60" :precision="1" style="width:100%" />
            </el-form-item>
          </div>
        </div>

        <el-form-item label="Ghi chú buổi tập">
          <el-input type="textarea" v-model="coForm.notes" rows="2" placeholder="Cảm nhận hôm nay..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkOutDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submitCheckOut" :loading="checkingOut">Hoàn thành buổi tập</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustWeekDialog" title="🚀 CẬP NHẬT CHỈ SỐ & CHUYỂN TUẦN MỚI" width="460px" align-center>
      <div style="margin-bottom:14px;font-size:0.9rem;color:var(--c-text2)">
        Chúc mừng bạn đã hoàn thành tuần tập! Hãy nhập số liệu cơ thể hiện tại để AI tiến hành tính toán cấu trúc lại Reps/Sets/Độ khó tuần sau.
      </div>
      <el-form :model="adjustForm" label-position="top">
        <el-form-item label="Cân nặng hiện tại (kg) *" required>
          <el-input-number v-model="adjustForm.newWeight" :min="30" :max="300" :precision="1" style="width:100%"/>
        </el-form-item>
        <el-form-item label="Tỉ lệ mỡ cơ thể hiện tại (%) - Body Fat">
          <el-input-number v-model="adjustForm.newBodyFat" :min="2" :max="60" :precision="1" style="width:100%"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustWeekDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submitAdjustWeek" :loading="adjusting">
          🔄 CẬP NHẬT GIÁO ÁN TUẦN TIẾP THEO
        </el-button>
      </template>
    </el-dialog>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { planAPI, sessionAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const plan = ref(null)
const allPlans = ref([])
const weekProgress = ref(null)
const activeSessions = ref([])
const loading = ref(true)

const generating = ref(false)
const saving = ref(false)
const checkingOut = ref(false)
const adjusting = ref(false)

const goalDialog = ref(false)
const allPlansDialog = ref(false)
const exDetailDialog = ref(false)
const scheduleDialog = ref(false)
const checkOutDialog = ref(false)
const adjustWeekDialog = ref(false)

const selEx = ref(null)
const selectedDay = ref(null)
const selectedDayNumber = ref(null)
const checkoutSessionId = ref(null)

// === MỚI: state cho dialog tạo giáo án có 2 tab (AI / Giáo án mẫu) ===
const createTab = ref('ai')
const templates = ref([])
const loadingTemplates = ref(false)
const selectedTemplateId = ref(null)
const applyingTemplate = ref(false)

const genForm = reactive({
  goal: '',
  fitnessLevel: null,
  daysPerWeek: null
})

const adjustForm = reactive({
  newWeight: null,
  newBodyFat: null
})

const schedForm = reactive({
  planDayId: null,
  sessionDate: '',
  scheduledTime: '06:00:00',
  weekNumber: 1
})

const coForm = reactive({
  completionRate: 100,
  checkoutWeight: null,
  checkoutBodyFat: null,
  notes: '',
  isLastSessionOfWeek: false
})

const goals = [
  { value: 'MUSCLE_GAIN', icon: '💪', label: 'Tăng cơ / Sức mạnh', desc: 'Yêu cầu tối thiểu 4 buổi/tuần', aiNote: 'AI ưu tiên bài tập compound nặng, tăng Sets, hạ Reps. Phân bổ cách ngày để phục hồi cơ.' },
  { value: 'WEIGHT_LOSS', icon: '🔥', label: 'Giảm cân / Đốt mỡ', desc: 'Yêu cầu tối thiểu 4 buổi/tuần', aiNote: 'AI ưu tiên Cardio/HIIT, tăng lượng Reps, giảm thời gian nghỉ. Sắp xếp chu kỳ tập liên tục.' },
  { value: 'ENDURANCE', icon: '🏃', label: 'Tăng sức bền', desc: 'Yêu cầu tối thiểu 3 buổi/tuần', aiNote: 'AI chọn Cardio và Full Body thời gian dài, cường độ vừa, xen kẽ phục hồi tim mạch.' },
  { value: 'FLEXIBILITY', icon: '🤸', label: 'Tăng linh hoạt', desc: 'Yêu cầu tối thiểu 2 buổi/tuần', aiNote: 'AI ưu tiên các bài Yoga kéo giãn cơ, giữ thế lâu, giải tỏa căng cơ.' },
  { value: 'MAINTENANCE', icon: '⚖️', label: 'Duy trì thể hình', desc: 'Yêu cầu tối thiểu 2 buổi/tuần', aiNote: 'AI cân bằng đều giữa các nhóm cơ chính với cấu trúc Set/Rep tiêu chuẩn.' }
]

const minDaysRequired = computed(() => {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') return 4
  if (genForm.goal === 'ENDURANCE') return 3
  return 2
})

const validDaysOptions = computed(() => {
  const min = minDaysRequired.value
  return [2, 3, 4, 5, 6].filter(d => d >= min)
})

function handleGoalSelect(goalValue) {
  genForm.goal = goalValue
  if (genForm.daysPerWeek && genForm.daysPerWeek < minDaysRequired.value) {
    genForm.daysPerWeek = minDaysRequired.value
  }
}

const currentWeekAnchorDate = computed(() => {
  if (!activeSessions.value.length || !plan.value) return null
  const currentWeekSessions = activeSessions.value.filter(s =>
      s.weekNumber === plan.value.currentWeek &&
      s.planName === plan.value.planName &&
      s.sessionDate
  )
  if (!currentWeekSessions.length) return null
  const sorted = [...currentWeekSessions].sort((a, b) => dayjs(a.sessionDate).diff(dayjs(b.sessionDate)))
  return dayjs(sorted[0].sessionDate)
})

const anchorDateDisplay = computed(() => {
  return currentWeekAnchorDate.value ? currentWeekAnchorDate.value.format('DD/MM/YYYY') : null
})

const maxDateDisplay = computed(() => {
  return currentWeekAnchorDate.value ? currentWeekAnchorDate.value.add(6, 'day').format('DD/MM/YYYY') : null
})

function disableInvalidDates(date) {
  const today = dayjs().startOf('day')
  const checkDate = dayjs(date)
  if (checkDate.isBefore(today)) return true

  const formattedCheckDate = checkDate.format('YYYY-MM-DD')
  const isDateDuplicated = activeSessions.value.some(s =>
      s.sessionDate === formattedCheckDate &&
      s.planName === plan.value.planName &&
      !(selectedDay.value && s.dayName === selectedDay.value.dayName)
  )
  if (isDateDuplicated) return true

  if (currentWeekAnchorDate.value) {
    const anchor = currentWeekAnchorDate.value
    const maxAllowedDate = anchor.add(6, 'day')
    return checkDate.isBefore(anchor) || checkDate.isAfter(maxAllowedDate)
  }
  return false
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
            s.dayName === day.dayName &&
            s.planName === plan.value.planName
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

// ====================== SCHEDULE ======================
function openScheduleDialog(day, dayNumber) {
  selectedDay.value = day
  selectedDayNumber.value = dayNumber
  schedForm.planDayId = day.id
  schedForm.weekNumber = plan.value.currentWeek
  schedForm.sessionDate = day.scheduledDate || dayjs().format('YYYY-MM-DD')
  schedForm.scheduledTime = day.scheduledTime || '06:00:00'
  scheduleDialog.value = true
}

async function saveSchedule() {
  if (!schedForm.sessionDate) {
    ElMessage.warning('Vui lòng chọn ngày tập')
    return
  }
  saving.value = true
  try {
    await sessionAPI.enroll(schedForm)
    ElMessage.success('✅ Đã xếp lịch và đồng bộ chu kỳ thành công!')
    scheduleDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lên lịch thất bại')
  } finally {
    saving.value = false
  }
}

// ====================== CHECK IN / CHECK OUT ======================
async function handleCheckIn(sessionId) {
  try {
    await sessionAPI.checkIn(sessionId)
    ElMessage.success('Check-in thành công! Hãy tập luyện hết mình 💪')
    await load()
  } catch (err) {}
}

function openCheckOutDialog(day, dayNumber) {
  checkoutSessionId.value = day.sessionId
  selectedDayNumber.value = dayNumber
  coForm.completionRate = 100
  coForm.notes = ''
  coForm.checkoutWeight = null
  coForm.checkoutBodyFat = null
  coForm.isLastSessionOfWeek = (dayNumber === plan.value.sessionsPerWeek)
  checkOutDialog.value = true
}

async function submitCheckOut() {
  if (coForm.isLastSessionOfWeek && !coForm.checkoutWeight) {
    ElMessage.warning('Vui lòng nhập cân nặng cuối tuần để AI phân tích cấu trúc lại giáo án!')
    return
  }
  checkingOut.value = true
  try {
    await sessionAPI.complete(checkoutSessionId.value, {
      sessionId: checkoutSessionId.value,
      completionRate: coForm.completionRate,
      checkoutWeight: coForm.checkoutWeight,
      checkoutBodyFat: coForm.checkoutBodyFat,
      notes: coForm.notes,
      exerciseLogs: []
    })
    ElMessage.success('Hoàn thành buổi tập! 🎉')
    checkOutDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error('Hoàn thành buổi tập thất bại')
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

function openExDetail(ex) {
  selEx.value = ex
  exDetailDialog.value = true
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
.anchor-info-box {
  background: #fef0f0; color: #f56c6c; padding: 10px 12px; border-radius: 6px; font-size: 0.85rem; border: 1px solid #fde2e2;
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
</style>