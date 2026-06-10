<template>
  <main class="page-shell">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">Admin Console</div>
        <h1 class="page-title">会员信息管理</h1>
        <p class="page-subtitle">查看、导出、修改和删除会员信息。此页面仅限管理员访问。</p>
        <div class="hero-actions">
          <el-button plain @click="goBack">返回采集页面</el-button>
          <el-button type="danger" plain @click="logout">退出管理员</el-button>
        </div>
      </div>

      <div class="hero-card">
        <div class="hero-card-header">
          <span>管理员会话</span>
          <span class="status-pill success">已登录</span>
        </div>
        <div class="hero-stat-grid">
          <div class="hero-stat">
            <span class="stat-label">当前账号</span>
            <strong>{{ adminUsername || 'admin' }}</strong>
          </div>
          <div class="hero-stat">
            <span class="stat-label">当前页条数</span>
            <strong>{{ rows.length }}</strong>
          </div>
          <div class="hero-stat wide">
            <span class="stat-label">数据概览</span>
            <strong>共 {{ pagination.total }} 条会员记录</strong>
          </div>
        </div>
      </div>
    </section>

    <div class="content-stack">
      <section class="section-panel form-panel">
        <div class="section-head">
          <div>
            <span class="section-kicker">Admin</span>
            <h2 class="section-title">检索与导出</h2>
            <p class="section-desc">支持按姓名、邮箱、单位关键词检索，并按当前筛选结果导出 CSV。</p>
          </div>
        </div>

        <div class="admin-filter-grid">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索姓名、邮箱、手机号、所在单位"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="filters.memberStatus" clearable placeholder="会员状态">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
          </el-select>
          <div class="admin-filter-actions">
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" :loading="tableLoading" @click="handleSearch">查询</el-button>
            <el-button type="success" plain :loading="exporting" @click="handleExport">导出 CSV</el-button>
          </div>
        </div>
      </section>

      <section class="section-panel form-panel">
        <div class="section-head">
          <div>
            <span class="section-kicker">Records</span>
            <h2 class="section-title">会员信息列表</h2>
            <p class="section-desc">管理员可以查看详细内容、修改字段、删除记录。</p>
          </div>
        </div>

        <el-table v-loading="tableLoading" :data="rows" border class="admin-table">
          <el-table-column prop="profileId" label="档案ID" min-width="96" />
          <el-table-column prop="memberName" label="姓名" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="organization" label="所在单位" min-width="220" />
          <el-table-column prop="memberType" label="会员类型" min-width="120" />
          <el-table-column prop="memberStatus" label="会员状态" min-width="120">
            <template #default="{ row }">
              <span :class="['status-pill', memberStatusClass(row.memberStatus)]">{{ memberStatusText(row.memberStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="resumeParseStatus" label="解析状态" min-width="120">
            <template #default="{ row }">
              <span :class="['status-pill', parseStatusClass(row.resumeParseStatus)]">{{ parseStatusText(row.resumeParseStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="220">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openViewDialog(row.profileId)">查看</el-button>
                <el-button link type="primary" @click="openEditDialog(row.profileId)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="pagination.total"
            :current-page="pagination.page"
            :page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="viewDialogVisible" title="会员信息详情" width="960px">
      <el-descriptions v-if="detailData" :column="2" border class="admin-descriptions">
        <el-descriptions-item label="档案ID">{{ detailData.profileId }}</el-descriptions-item>
        <el-descriptions-item label="会员状态">{{ memberStatusText(detailData.memberStatus) }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detailData.memberName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailData.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detailData.gender || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detailData.birthDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="国家或地区">{{ detailData.countryRegion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在单位">{{ detailData.organization || '-' }}</el-descriptions-item>
        <el-descriptions-item label="院系或部门">{{ detailData.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职务或职称">{{ detailData.positionTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最高学历">{{ detailData.highestDegree || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业领域">{{ detailData.professionalField || '-' }}</el-descriptions-item>
        <el-descriptions-item label="会员类型">{{ detailData.memberType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="研究方向" :span="2">{{ detailData.researchDirection || '-' }}</el-descriptions-item>
        <el-descriptions-item label="教育背景" :span="2">{{ detailData.educationBackground || '-' }}</el-descriptions-item>
        <el-descriptions-item label="代表性成果" :span="2">
          <div class="dialog-list">
            <div v-for="(item, index) in detailData.representativeAchievements || []" :key="index">
              {{ index + 1 }}. {{ item }}
            </div>
            <div v-if="!(detailData.representativeAchievements || []).length">-</div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="个人主页">{{ detailData.homepage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ORCID">{{ detailData.orcid || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学术主页">{{ detailData.scholarProfile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入会日期">{{ detailData.joinDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="简历解析状态">{{ parseStatusText(detailData.resumeParseStatus) }}</el-descriptions-item>
        <el-descriptions-item label="简历文件" :span="2">
          <a v-if="detailData.resumeFileUrl" class="file-link" :href="normalizeResumeUrl(detailData.resumeFileUrl)" target="_blank" rel="noreferrer">
            {{ detailData.resumeOriginalName || '打开简历 PDF' }}
          </a>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑会员信息" width="1080px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="118px" class="member-form">
        <div class="form-group">
          <div class="form-group-title">基础身份信息</div>
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item label="姓名" prop="memberName">
                <el-input v-model="editForm.memberName" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="editForm.email" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="手机号">
                <el-input v-model="editForm.phone" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="性别">
                <el-select v-model="editForm.gender" clearable>
                  <el-option label="男" value="男" />
                  <el-option label="女" value="女" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="出生日期">
                <el-date-picker
                  v-model="editForm.birthDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  :disabled-date="disableFutureDate"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="国家或地区">
                <el-input v-model="editForm.countryRegion" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-group">
          <div class="form-group-title">学术与职业信息</div>
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item label="所在单位" prop="organization">
                <el-input v-model="editForm.organization" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="院系或部门">
                <el-input v-model="editForm.department" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="职务或职称">
                <el-input v-model="editForm.positionTitle" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="最高学历">
                <el-input v-model="editForm.highestDegree" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="专业领域">
                <el-input v-model="editForm.professionalField" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="会员类型">
                <el-select v-model="editForm.memberType">
                  <el-option label="普通会员" value="REGULAR" />
                  <el-option label="学生会员" value="STUDENT" />
                  <el-option label="专家会员" value="EXPERT" />
                  <el-option label="理事会员" value="COUNCIL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="会员状态">
                <el-select v-model="editForm.memberStatus">
                  <el-option label="草稿" value="DRAFT" />
                  <el-option label="已提交" value="SUBMITTED" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="研究方向">
                <el-input v-model="editForm.researchDirection" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-group">
          <div class="form-group-title">学术主页与成果</div>
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item label="个人主页">
                <el-input v-model="editForm.homepage" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="ORCID">
                <el-input v-model="editForm.orcid" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="学术主页">
                <el-input v-model="editForm.scholarProfile" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="教育背景">
                <el-input v-model="editForm.educationBackground" type="textarea" :rows="5" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="代表性成果">
                <div class="achievement-list">
                  <div v-for="(_, index) in editForm.representativeAchievements" :key="index" class="achievement-item">
                    <el-input
                      v-model="editForm.representativeAchievements[index]"
                      type="textarea"
                      :rows="2"
                      :placeholder="`代表性成果 ${index + 1}`"
                    />
                    <el-button
                      class="achievement-remove"
                      type="danger"
                      plain
                      :disabled="editForm.representativeAchievements.length === 1"
                      @click="removeAchievement(index)"
                    >删除</el-button>
                  </div>
                  <el-button
                    type="primary"
                    plain
                    :disabled="editForm.representativeAchievements.length >= 10"
                    @click="addAchievement"
                  >新增成果</el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="editForm.remark" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  deleteMemberProfile,
  exportMemberProfiles,
  getAdminSession,
  getMemberProfileDetail,
  getMemberProfiles,
  updateMemberProfile
} from '../../api/admin'
import { clearAdminAuth, getAdminUsername } from '../../utils/adminAuth'

const router = useRouter()
const uploadBaseUrl = (import.meta.env.VITE_UPLOAD_BASE_URL || '/member-uploads').replace(/\/$/, '')

const adminUsername = ref(getAdminUsername())
const tableLoading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const rows = ref([])
const detailData = ref(null)
const viewDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref(null)

const filters = reactive({
  keyword: '',
  memberStatus: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const editForm = reactive(createEmptyForm())

const editRules = {
  memberName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  organization: [{ required: true, message: '请输入所在单位', trigger: 'blur' }]
}

const queryParams = computed(() => ({
  keyword: filters.keyword || undefined,
  memberStatus: filters.memberStatus || undefined,
  page: pagination.page,
  pageSize: pagination.pageSize
}))

onMounted(async () => {
  try {
    const session = await getAdminSession()
    adminUsername.value = session.username || adminUsername.value
    await loadTable()
  } catch (error) {
    clearAdminAuth()
    await router.replace('/society/member-entry')
  }
})

function createEmptyForm() {
  return {
    profileId: null,
    memberName: '',
    email: '',
    phone: '',
    gender: '',
    birthDate: '',
    countryRegion: '',
    organization: '',
    department: '',
    positionTitle: '',
    highestDegree: '',
    professionalField: '',
    researchDirection: '',
    educationBackground: '',
    representativeAchievements: [''],
    homepage: '',
    orcid: '',
    scholarProfile: '',
    memberType: 'REGULAR',
    memberStatus: 'DRAFT',
    remark: ''
  }
}

async function loadTable() {
  tableLoading.value = true
  try {
    const response = await getMemberProfiles(queryParams.value)
    rows.value = response.items || []
    pagination.total = response.total || 0
    pagination.page = response.page || pagination.page
    pagination.pageSize = response.pageSize || pagination.pageSize
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadTable()
}

function resetFilters() {
  filters.keyword = ''
  filters.memberStatus = ''
  pagination.page = 1
  loadTable()
}

function handlePageChange(page) {
  pagination.page = page
  loadTable()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadTable()
}

async function openViewDialog(profileId) {
  const response = await getMemberProfileDetail(profileId)
  detailData.value = normalizeProfile(response)
  viewDialogVisible.value = true
}

async function openEditDialog(profileId) {
  const response = await getMemberProfileDetail(profileId)
  Object.assign(editForm, normalizeProfile(response))
  editDialogVisible.value = true
}

async function submitEdit() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    const response = await updateMemberProfile(editForm.profileId, buildUpdatePayload())
    Object.assign(editForm, normalizeProfile(response))
    ElMessage.success('会员信息已更新')
    editDialogVisible.value = false
    if (detailData.value?.profileId === editForm.profileId) {
      detailData.value = normalizeProfile(response)
    }
    await loadTable()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确认删除会员记录「${row.memberName || row.profileId}」吗？此操作不可恢复。`,
    '删除确认',
    { type: 'warning' }
  )

  await deleteMemberProfile(row.profileId)
  ElMessage.success('会员记录已删除')
  if (detailData.value?.profileId === row.profileId) {
    detailData.value = null
    viewDialogVisible.value = false
  }
  if (editForm.profileId === row.profileId) {
    editDialogVisible.value = false
    Object.assign(editForm, createEmptyForm())
  }
  await loadTable()
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportMemberProfiles({
      keyword: filters.keyword || undefined,
      memberStatus: filters.memberStatus || undefined
    })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `member_profiles_${Date.now()}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

function normalizeProfile(data) {
  return {
    ...createEmptyForm(),
    ...data,
    representativeAchievements: splitAchievements(data.representativeAchievements)
  }
}

function splitAchievements(value) {
  if (Array.isArray(value)) {
    const items = value.map(item => String(item || '').trim()).filter(Boolean).slice(0, 10)
    return items.length ? items : ['']
  }
  if (!value) {
    return ['']
  }
  const items = String(value).split(/[;；]\s*/).map(item => item.trim()).filter(Boolean).slice(0, 10)
  return items.length ? items : ['']
}

function buildUpdatePayload() {
  return {
    memberName: editForm.memberName,
    email: editForm.email,
    phone: editForm.phone,
    gender: editForm.gender,
    birthDate: editForm.birthDate,
    countryRegion: editForm.countryRegion,
    organization: editForm.organization,
    department: editForm.department,
    positionTitle: editForm.positionTitle,
    highestDegree: editForm.highestDegree,
    professionalField: editForm.professionalField,
    researchDirection: editForm.researchDirection,
    educationBackground: editForm.educationBackground,
    representativeAchievements: editForm.representativeAchievements
      .map(item => String(item || '').trim())
      .filter(Boolean)
      .slice(0, 10),
    homepage: editForm.homepage,
    orcid: editForm.orcid,
    scholarProfile: editForm.scholarProfile,
    memberType: editForm.memberType,
    memberStatus: editForm.memberStatus,
    remark: editForm.remark
  }
}

function addAchievement() {
  if (editForm.representativeAchievements.length < 10) {
    editForm.representativeAchievements.push('')
  }
}

function removeAchievement(index) {
  if (editForm.representativeAchievements.length <= 1) {
    editForm.representativeAchievements[0] = ''
    return
  }
  editForm.representativeAchievements.splice(index, 1)
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function parseStatusText(value) {
  const map = {
    NOT_PARSED: '未解析',
    PARSING: '解析中',
    PARSED: '解析成功',
    PARSE_FAILED: '解析失败',
    SKIPPED: '已跳过解析',
    SUBMITTED: '已提交'
  }
  return map[value] || value || '-'
}

function memberStatusText(value) {
  const map = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    SKIPPED: '已跳过解析',
    PARSED: '解析成功',
    PARSE_FAILED: '解析失败'
  }
  return map[value] || value || '-'
}

function memberStatusClass(value) {
  const map = {
    DRAFT: 'neutral',
    SUBMITTED: 'success'
  }
  return map[value] || 'neutral'
}

function parseStatusClass(value) {
  const map = {
    NOT_PARSED: 'neutral',
    PARSING: 'processing',
    PARSED: 'success',
    PARSE_FAILED: 'danger',
    SKIPPED: 'warning',
    SUBMITTED: 'success'
  }
  return map[value] || 'neutral'
}

function normalizeResumeUrl(value) {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  const normalized = value.replace(/\\/g, '/').replace(/^\.\/?/, '')

  if (normalized.startsWith('/uploads/')) {
    return normalized.replace(/^\/uploads/, uploadBaseUrl)
  }

  if (normalized.startsWith('uploads/')) {
    return `${uploadBaseUrl}/${normalized.replace(/^uploads\//, '')}`
  }

  return normalized
}

function disableFutureDate(value) {
  return value.getTime() > Date.now()
}

async function logout() {
  clearAdminAuth()
  ElMessage.success('已退出管理员登录')
  await router.replace('/society/member-entry')
}

async function goBack() {
  await router.push('/society/member-entry')
}
</script>
