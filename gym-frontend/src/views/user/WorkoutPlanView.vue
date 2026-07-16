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
              <el-tag v-if="plan.fitnessScore != null" type="success" effect="plain">
                💪 Thể lực: {{ fitnessScoreText(plan) }}
              </el-tag>
              <el-tag v-if="plan.bodyType" effect="plain">
                🧍 Thể trạng: {{ bodyTypeLabel(plan.bodyType) }}
              </el-tag>
            </div>

            <!-- MỚI (Patch 7): estimatedWeeks — chỉ hiện với giáo án mới (có estimatedWeeks) -->
            <div v-if="plan.estimatedWeeks != null" style="font-size:0.82rem;color:var(--c-text2);margin-bottom:6px">
              ⏱ Dự kiến ban đầu: <strong>{{ plan.estimatedWeeks }} tuần</strong>
              <span v-if="plan.durationWeeks !== plan.estimatedWeeks">
                (hiện đã điều chỉnh còn <strong>{{ plan.durationWeeks }} tuần</strong>)
              </span>
            </div>

            <!-- MỚI (Patch 7-8): Tiến độ mục tiêu — dữ liệu LUÔN lấy trực tiếp từ Backend,
                 không tự tính lại (kể cả ENDURANCE — targetCurrentValue đã là giá trị live). -->
            <div v-if="hasTarget(plan)" class="target-progress-box">
              <div style="font-weight:700;margin-bottom:6px">🎯 Tiến độ mục tiêu</div>
              <div style="font-size:0.85rem">
                {{ targetBaselineText(plan) }} → {{ targetGoalText(plan) }}
              </div>
              <div style="font-size:0.85rem;margin-top:2px">
                Hiện tại: <strong>{{ targetCurrentText(plan) }}</strong>
              </div>
              <el-tag v-if="plan.targetAchieved" type="success" size="small" style="margin-top:8px">
                ✅ Đã đạt mục tiêu
              </el-tag>
            </div>
          </div>
          <el-button type="primary" plain size="small" @click="openGoalDialog">
            🔄 Đổi mục tiêu
          </el-button>
        </div>
      </el-card>

      <!-- MỚI: weightAdjustmentNote — hiển thị NGUYÊN VĂN nội dung Backend trả về,
           áp dụng cho mọi Goal (Patch 5 dùng cho MUSCLE_GAIN/WEIGHT_LOSS/ENDURANCE,
           Patch 9 dùng cho MAINTENANCE). Frontend không tự sinh message. -->
      <div v-if="plan.weightAdjustmentNote" class="weight-adjustment-box">
        ⚖️ {{ plan.weightAdjustmentNote }}
      </div>

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

      <div v-else-if="!plan.isAiGenerated" class="suggested-days-box">
        <div style="font-weight:700;font-size:0.9rem;margin-bottom:6px">📅 Ngày tập theo lịch Admin:</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-tag v-for="day in plan.planDays" :key="day.id" effect="plain" type="warning">
            {{ dowVietName(day.dayOfWeek) }}
          </el-tag>
        </div>
      </div>

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

          <!-- MỚI (Patch 7): Nhập mục tiêu cụ thể theo Goal -->
          <template v-if="genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS'">
            <el-divider/>
            <div style="margin-bottom:16px">
              <div style="font-weight:700;color:var(--c-text);margin-bottom:10px">
                🎯 Mục tiêu {{ genForm.goal === 'MUSCLE_GAIN' ? 'tăng' : 'giảm' }} cân
              </div>
              <el-form-item :label="'Số kg muốn ' + (genForm.goal === 'MUSCLE_GAIN' ? 'tăng' : 'giảm')">
                <el-input-number v-model="genForm.targetDeltaKgInput" :min="0.5" :max="100" :precision="1" style="width:100%"/>
              </el-form-item>
            </div>
          </template>

          <!-- MỚI (Patch 6+7): Bài test sức bền + chọn mục tiêu ENDURANCE -->
          <template v-else-if="genForm.goal === 'ENDURANCE'">
            <el-divider/>
            <div style="margin-bottom:16px">
              <div style="font-weight:700;color:var(--c-text);margin-bottom:10px">🏃 Bài test sức bền</div>

              <div v-if="loadingEnduranceTest">
                <el-skeleton :rows="2" animated/>
              </div>

              <template v-else>
                <div v-if="enduranceTest && !showEnduranceTestForm" class="endurance-test-result">
                  <div>💪 Chống đẩy: <strong>{{ enduranceTest.pushupReps }}</strong> reps</div>
                  <div>🧘 Plank: <strong>{{ enduranceTest.plankSeconds }}</strong> giây</div>
                  <div>🦵 Squat: <strong>{{ enduranceTest.squatReps }}</strong> reps</div>
                  <el-button size="small" text @click="showEnduranceTestForm = true">🔄 Làm lại bài test</el-button>
                </div>

                <div v-else class="endurance-test-form">
                  <p style="font-size:0.82rem;color:var(--c-text2);margin-bottom:10px">
                    Vui lòng thực hiện đủ 3 bài test dưới đây trước khi đặt mục tiêu.
                  </p>
                  <div class="grid-2">
                    <el-form-item label="Chống đẩy tối đa (reps)">
                      <el-input-number v-model="enduranceTestForm.pushupReps" :min="0" :max="500" style="width:100%"/>
                    </el-form-item>
                    <el-form-item label="Plank tối đa (giây)">
                      <el-input-number v-model="enduranceTestForm.plankSeconds" :min="0" :max="3600" style="width:100%"/>
                    </el-form-item>
                  </div>
                  <el-form-item label="Squat tối đa (reps)">
                    <el-input-number v-model="enduranceTestForm.squatReps" :min="0" :max="500" style="width:100%"/>
                  </el-form-item>
                  <el-button type="primary" size="small" @click="submitEnduranceTest" :loading="submittingEnduranceTest">
                    Lưu kết quả test
                  </el-button>
                </div>

                <div v-if="enduranceTest && !showEnduranceTestForm" style="margin-top:16px">
                  <div style="font-weight:700;color:var(--c-text);margin-bottom:10px">🎯 Chọn chỉ số mục tiêu</div>
                  <el-radio-group v-model="genForm.enduranceMetric" style="display:flex;flex-direction:column;gap:8px">
                    <el-radio v-for="m in enduranceMetricOptions" :key="m.value" :label="m.value">
                      {{ m.label }} — hiện tại: {{ enduranceBaselineFor(m.value) ?? '--' }} {{ m.unit }}
                    </el-radio>
                  </el-radio-group>

                  <el-form-item v-if="genForm.enduranceMetric" label="Mục tiêu muốn đạt" style="margin-top:10px">
                    <el-input-number v-model="genForm.enduranceTargetValue" :min="0" :max="9999" style="width:100%"/>
                  </el-form-item>
                </div>
              </template>
            </div>
          </template>

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
          :loading="generating" :disabled="!canGenerate"
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

      <!-- SỬA (Patch 4): nhập số Rep/Giây thực hiện thay vì chọn % — Backend tự tính
           completionPercent từ dữ liệu này đối chiếu với kế hoạch (sets×reps hoặc sets×duration) -->
      <div v-for="ex in checkoutExercises" :key="ex.exerciseId" class="checkout-ex-row">
        <div style="font-weight:600;margin-bottom:4px">{{ ex.exerciseName }}</div>
        <div style="font-size:0.78rem;color:var(--c-text3);margin-bottom:8px">
          Kế hoạch: {{ plannedText(ex) }}
        </div>
        <el-input-number
          v-if="ex.reps"
          v-model="coForm.logs[ex.exerciseId].repsCompleted"
          :min="0" :max="9999" style="width:100%"
          placeholder="Số rep thực hiện"
        />
        <el-input-number
          v-else-if="ex.durationSeconds"
          v-model="coForm.logs[ex.exerciseId].durationCompleted"
          :min="0" :max="99999" style="width:100%"
          placeholder="Số giây thực hiện"
        />
        <div v-else style="font-size:0.78rem;color:var(--c-text3)">Không có dữ liệu kế hoạch cho bài này</div>
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

      <div class="weight-guide-box">
        📏 <strong>Cách xác định tạ:</strong> chọn 1 mức tạ mà bạn tập đến set thứ 12 thì không thể tập tiếp được nữa.
        Với bài không dùng tạ, lấy cân nặng cơ thể làm chuẩn (có thể thêm dây kháng lực để tăng/giảm khối lượng).
      </div>

      <!-- MỚI (Patch 3): hiển thị mức tạ khuyến nghị (snapshot AI, không đổi) nếu có -->
      <div v-if="selEx.recommendedWeightKg != null" class="recommended-weight-box">
        🎯 Mức tạ khuyến nghị (AI đề xuất lúc tạo giáo án): <strong>{{ selEx.recommendedWeightKg }} kg</strong>
      </div>

      <div v-if="!selEx.baseWeightKg" style="margin-top:14px">
        <el-form-item label="Nhập mức tạ khởi điểm (kg)">
          <el-input-number v-model="baseWeightInput" :min="0" :max="500" :precision="1" style="width:100%"/>
        </el-form-item>
        <el-button type="primary" @click="saveBaseWeight" :loading="savingWeight">Lưu tạ khởi điểm</el-button>
      </div>

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
import { planAPI, sessionAPI, enduranceTestAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const plan = ref(null)
const allPlans = ref([])
const weekProgress = ref(null)
const activeSessions = ref([])
const loading = ref(true)

const generating = ref(false)
const checkingOut = ref(false)

const goalDialog = ref(false)
const allPlansDialog = ref(false)
const exDetailDialog = ref(false)
const checkOutDialog = ref(false)

const selEx = ref(null)
const selectedDayNumber = ref(null)
const checkoutSessionId = ref(null)

const createTab = ref('ai')
const templates = ref([])
const loadingTemplates = ref(false)
const selectedTemplateId = ref(null)
const applyingTemplate = ref(false)

const checkoutExercises = ref([])

const baseWeightInput = ref(null)
const savingWeight = ref(false)

const scheduleSelectionDialog = ref(false)
const scheduleOptionsList = ref([])
const selectedScheduleIndex = ref(null)
const confirmingSchedule = ref(false)

// ── MỚI (Patch 6): Endurance Test state ──
const enduranceTest = ref(null)
const loadingEnduranceTest = ref(false)
const enduranceTestForm = reactive({ pushupReps: null, plankSeconds: null, squatReps: null })
const submittingEnduranceTest = ref(false)
const showEnduranceTestForm = ref(false)

const enduranceMetricOptions = [
  { value: 'PUSHUP_REPS', label: 'Chống đẩy', unit: 'reps' },
  { value: 'PLANK_SECONDS', label: 'Plank', unit: 'giây' },
  { value: 'SQUAT_REPS', label: 'Squat', unit: 'reps' }
]

const genForm = reactive({
  goal: '',
  fitnessLevel: null,
  daysPerWeek: null,
  // MỚI (Patch 7): target theo Goal
  targetDeltaKgInput: null,
  enduranceMetric: null,
  enduranceTargetValue: null
})

// === coForm.logs: repsCompleted/durationCompleted thay cho completionPercent (Patch 4) ===
const coForm = reactive({
  logs: {}, // { [exerciseId]: { repsCompleted, durationCompleted, weightUsedKg } }
  checkoutWeight: null,
  checkoutBodyFat: null,
  notes: '',
  isLastSessionOfWeek: false
})

// SỬA (Patch 1): đã bỏ FLEXIBILITY khỏi hệ thống
const goals = [
  { value: 'MUSCLE_GAIN', icon: '💪', label: 'Tăng cơ / Sức mạnh', desc: 'Yêu cầu 4-6 buổi/tuần', aiNote: 'ưu tiên bài tập compound nặng, tăng Sets, hạ Reps. Phân bổ cách ngày để phục hồi cơ.' },
  { value: 'WEIGHT_LOSS', icon: '🔥', label: 'Giảm cân / Đốt mỡ', desc: 'Yêu cầu 4-6 buổi/tuần', aiNote: 'ưu tiên Cardio/HIIT, tăng lượng Reps, giảm thời gian nghỉ. Sắp xếp chu kỳ tập liên tục.' },
  { value: 'ENDURANCE', icon: '🏃', label: 'Tăng sức bền', desc: 'Yêu cầu 3-5 buổi/tuần', aiNote: 'chọn Cardio và Full Body thời gian dài, cường độ vừa, xen kẽ phục hồi tim mạch.' },
  { value: 'MAINTENANCE', icon: '⚖️', label: 'Duy trì thể hình', desc: 'Yêu cầu 3-5 buổi/tuần', aiNote: 'cân bằng đều giữa các nhóm cơ chính với cấu trúc Set/Rep tiêu chuẩn.' }
]

const minDaysRequired = computed(() => {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') return 4
  if (genForm.goal === 'ENDURANCE' || genForm.goal === 'MAINTENANCE') return 3
  return 3
})

const maxDaysRequired = computed(() => {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') return 6
  if (genForm.goal === 'ENDURANCE' || genForm.goal === 'MAINTENANCE') return 5
  return 5
})

const validDaysOptions = computed(() => {
  const opts = []
  for (let d = minDaysRequired.value; d <= maxDaysRequired.value; d++) opts.push(d)
  return opts
})

// MỚI: chặn khởi tạo nếu ENDURANCE chưa có kết quả test
const canGenerate = computed(() => {
  if (!genForm.goal) return false
  if (genForm.goal === 'ENDURANCE' && !enduranceTest.value) return false
  return true
})

function handleGoalSelect(goalValue) {
  genForm.goal = goalValue
  genForm.targetDeltaKgInput = null
  genForm.enduranceMetric = null
  genForm.enduranceTargetValue = null
  if (genForm.daysPerWeek && genForm.daysPerWeek < minDaysRequired.value) {
    genForm.daysPerWeek = minDaysRequired.value
  }
  if (genForm.daysPerWeek && genForm.daysPerWeek > maxDaysRequired.value) {
    genForm.daysPerWeek = maxDaysRequired.value
  }
  if (goalValue === 'ENDURANCE') {
    loadEnduranceTest().then(() => {
      showEnduranceTestForm.value = !enduranceTest.value
    })
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

// ====================== ENDURANCE TEST ======================
async function loadEnduranceTest() {
  loadingEnduranceTest.value = true
  try {
    const res = await enduranceTestAPI.getMine()
    enduranceTest.value = res.data || null
  } catch (e) {
    enduranceTest.value = null
  } finally {
    loadingEnduranceTest.value = false
  }
}

async function submitEnduranceTest() {
  if (enduranceTestForm.pushupReps == null || enduranceTestForm.plankSeconds == null || enduranceTestForm.squatReps == null) {
    ElMessage.warning('Vui lòng nhập đủ cả 3 bài test')
    return
  }
  submittingEnduranceTest.value = true
  try {
    const res = await enduranceTestAPI.submit({ ...enduranceTestForm })
    enduranceTest.value = res.data
    showEnduranceTestForm.value = false
    ElMessage.success('Đã lưu kết quả bài test!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu kết quả test thất bại')
  } finally {
    submittingEnduranceTest.value = false
  }
}

function enduranceBaselineFor(metric) {
  if (!enduranceTest.value) return null
  return {
    PUSHUP_REPS: enduranceTest.value.pushupReps,
    PLANK_SECONDS: enduranceTest.value.plankSeconds,
    SQUAT_REPS: enduranceTest.value.squatReps
  }[metric]
}

// ====================== MỞ DIALOG TẠO GIÁO ÁN ======================
function resetGenForm() {
  genForm.goal = ''
  genForm.fitnessLevel = null
  genForm.daysPerWeek = null
  genForm.targetDeltaKgInput = null
  genForm.enduranceMetric = null
  genForm.enduranceTargetValue = null
  showEnduranceTestForm.value = false
}

function openGoalDialog() {
  goalDialog.value = true
  createTab.value = 'ai'
  selectedTemplateId.value = null
  resetGenForm()
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
function validateTargetInput() {
  if (genForm.goal === 'MUSCLE_GAIN' || genForm.goal === 'WEIGHT_LOSS') {
    if (!genForm.targetDeltaKgInput || genForm.targetDeltaKgInput <= 0) {
      ElMessage.warning('Vui lòng nhập số kg mục tiêu (số dương)')
      return false
    }
  }
  if (genForm.goal === 'ENDURANCE') {
    if (!enduranceTest.value) {
      ElMessage.warning('Vui lòng hoàn thành bài test sức bền trước')
      return false
    }
    if (!genForm.enduranceMetric) {
      ElMessage.warning('Vui lòng chọn chỉ số mục tiêu (Chống đẩy / Plank / Squat)')
      return false
    }
    if (genForm.enduranceTargetValue == null) {
      ElMessage.warning('Vui lòng nhập giá trị mục tiêu')
      return false
    }
  }
  return true
}

async function generateWithGoal() {
  if (!genForm.goal) {
    ElMessage.warning('Hãy chọn mục tiêu')
    return
  }
  if (!validateTargetInput()) return

  generating.value = true
  try {
    const payload = {
      goal: genForm.goal,
      fitnessLevel: genForm.fitnessLevel || null,
      daysPerWeek: genForm.daysPerWeek || null,
      targetDeltaKg: null,
      enduranceMetric: null,
      enduranceTargetValue: null
    }
    if (genForm.goal === 'MUSCLE_GAIN') {
      payload.targetDeltaKg = Math.abs(genForm.targetDeltaKgInput)
    } else if (genForm.goal === 'WEIGHT_LOSS') {
      payload.targetDeltaKg = -Math.abs(genForm.targetDeltaKgInput)
    } else if (genForm.goal === 'ENDURANCE') {
      payload.enduranceMetric = genForm.enduranceMetric
      payload.enduranceTargetValue = genForm.enduranceTargetValue
    }
    const r = await planAPI.generateWithGoal(payload)
    plan.value = r.data
    goalDialog.value = false
    ElMessage.success('Giáo án thích ứng đã khởi tạo thành công! 🎉')
    resetGenForm()
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Tạo giáo án thất bại')
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
    coForm.logs[ex.exerciseId] = { repsCompleted: null, durationCompleted: null, weightUsedKg: null }
  })
  coForm.notes = ''
  coForm.checkoutWeight = null
  coForm.checkoutBodyFat = null
  coForm.isLastSessionOfWeek = (dayNumber === plan.value.sessionsPerWeek)
  checkOutDialog.value = true
}

// MỚI (Patch 4): hiển thị kế hoạch reps/duration để người dùng đối chiếu
function plannedText(ex) {
  if (ex.reps) return `${ex.sets} set × ${ex.reps} rep = ${ex.sets * ex.reps} rep`
  if (ex.durationSeconds) return `${ex.sets} set × ${ex.durationSeconds}s = ${ex.sets * ex.durationSeconds}s`
  return '--'
}

async function submitCheckOut() {
  const missing = checkoutExercises.value.some(ex => {
    const log = coForm.logs[ex.exerciseId]
    if (ex.reps) return log.repsCompleted === null || log.repsCompleted === undefined
    if (ex.durationSeconds) return log.durationCompleted === null || log.durationCompleted === undefined
    return false
  })
  if (missing) {
    ElMessage.warning('Vui lòng nhập kết quả thực hiện cho tất cả bài tập!')
    return
  }
  if (coForm.isLastSessionOfWeek && !coForm.checkoutWeight) {
    ElMessage.warning('Vui lòng nhập cân nặng để cập nhật chỉ số cơ thể!')
    return
  }

  const exerciseLogs = Object.entries(coForm.logs).map(([exerciseId, v]) => ({
    exerciseId: Number(exerciseId),
    repsCompleted: v.repsCompleted,
    durationCompleted: v.durationCompleted,
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

    checkScheduleSelection(r.data)
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Hoàn thành buổi tập thất bại')
  } finally {
    checkingOut.value = false
  }
}

// ====================== UTILITY FUNCTIONS ======================
function fmtDate(d) {
  return d ? dayjs(d).format('DD/MM/YYYY') : ''
}

function openExDetail(ex) {
  selEx.value = ex
  baseWeightInput.value = null
  exDetailDialog.value = true
}

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

// SỬA (Patch 1): đã bỏ FLEXIBILITY
function goalLabel(g) {
  return {
    WEIGHT_LOSS: '🔥 Giảm cân',
    MUSCLE_GAIN: '💪 Tăng cơ',
    ENDURANCE: '🏃 Sức bền',
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

function fitnessLevelLabel(level) {
  return {
    EXCELLENT: 'Xuất sắc',
    GOOD: 'Tốt',
    AVERAGE: 'Trung bình',
    WEAK: 'Yếu'
  }[level] || null
}

function fitnessScoreText(p) {
  const score = Math.round(p.fitnessScore)
  const levelLabel = fitnessLevelLabel(p.fitnessLevel)
  return levelLabel ? `${levelLabel} (${score}/100)` : `${score}/100`
}

function bodyTypeLabel(bt) {
  return {
    CAO_GAY: 'Cao gầy',
    GAY_CAN_DOI: 'Hơi gầy',
    CAN_DOI: 'Bình thường',
    CO_BAP: 'Cơ bắp',
    VAN_DONG_VIEN: 'Vận động viên',
    THUA_CAN: 'Thừa cân'
  }[bt] || bt
}

// MỚI (Patch 7-8): Tiến độ mục tiêu — LUÔN dùng dữ liệu Backend trả về, không tự tính
function hasTarget(p) {
  return p && p.targetGoalValue != null
}

function targetUnit(p) {
  if (p.goal === 'ENDURANCE') {
    return { PUSHUP_REPS: 'reps', PLANK_SECONDS: 'giây', SQUAT_REPS: 'reps' }[p.targetMetricType] || ''
  }
  return 'kg'
}

function targetBaselineText(p) {
  return p.targetBaselineValue != null ? `${p.targetBaselineValue} ${targetUnit(p)}` : '--'
}
function targetGoalText(p) {
  return p.targetGoalValue != null ? `${p.targetGoalValue} ${targetUnit(p)}` : '--'
}
function targetCurrentText(p) {
  return p.targetCurrentValue != null ? `${p.targetCurrentValue} ${targetUnit(p)}` : '--'
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

function scheduleLabel(idx) {
  return String.fromCharCode(65 + idx)
}

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
        await performCheckIn(sessionId, true)
      } catch {
        ElMessage.info('Đã hủy check-in. Hãy nghỉ ngơi để hồi phục thể lực nhé 💤')
      }
      return
    }

    ElMessage.success('Check-in thành công! Hãy tập luyện hết mình 💪')
    if (result?.session?.dayMismatchWarning) {
      ElMessage({ message: result.session.dayMismatchWarning, type: 'warning', duration: 8000 })
    }
    checkScheduleSelection(result?.session)
    await load()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Check-in thất bại')
  }
}

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

/* MỚI: mục tiêu đo lường được + endurance test */
.target-progress-box {
  background:#eff6ff; border:1px solid #bfdbfe; padding:12px 14px; border-radius:8px; margin-top:8px;
}
.endurance-test-result {
  background:var(--c-card2); padding:12px 14px; border-radius:8px; display:flex; flex-direction:column; gap:6px; font-size:0.875rem;
}
.endurance-test-form {
  background:var(--c-card2); padding:12px 14px; border-radius:8px;
}
.recommended-weight-box {
  margin-top:14px; padding:10px 12px; background:#f0fdf4; border-left:4px solid #22c55e;
  font-size:0.85rem; border-radius:4px;
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

.schedule-option-card {
  border:2px solid var(--c-border2); border-radius:var(--radius-lg);
  padding:12px 14px; cursor:pointer; transition:all var(--transition); background:var(--c-card2);
}
.schedule-option-card:hover { border-color:var(--c-accent); }
.schedule-option-card.selected { border-color:var(--c-accent); background:#FFF8F0; }
</style>