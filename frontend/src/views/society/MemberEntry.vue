<template>
  <main class="page-shell">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">Society Membership Registry</div>
        <h1 class="page-title">学会会员信息采集</h1>
        <p class="page-subtitle">上传 PDF 简历，系统自动抽取关键资料并回填表单，用户确认后再提交入库。</p>
        <div class="hero-actions">
          <el-button v-if="profileId || memberNo" type="primary" plain @click="restoreDraft">恢复本地草稿</el-button>
          <el-button text @click="resetForm">清空当前表单</el-button>
        </div>
      </div>

      <div class="hero-card">
        <div class="hero-card-header">
          <span>当前档案</span>
          <span :class="['status-pill', parseStatusClass]">{{ parseStatusText }}</span>
        </div>
        <div class="hero-stat-grid">
          <div class="hero-stat">
            <span class="stat-label">档案 ID</span>
            <strong>{{ profileId || '待生成' }}</strong>
          </div>
          <div class="hero-stat">
            <span class="stat-label">会员编号</span>
            <strong>{{ memberNo || '待生成' }}</strong>
          </div>
          <div class="hero-stat wide">
            <span class="stat-label">当前文件</span>
            <strong>{{ resumeName || '尚未上传简历' }}</strong>
          </div>
        </div>
        <div class="completion-card">
          <div class="completion-row">
            <span>表单完整度</span>
            <strong>{{ completionPercent }}%</strong>
          </div>
          <el-progress :percentage="completionPercent" :stroke-width="10" :show-text="false" />
        </div>
      </div>
    </section>

    <div class="content-stack">
      <section class="section-panel upload-panel">
        <div class="section-head">
          <div>
            <span class="section-kicker">Step 01</span>
            <h2 class="section-title">上传与解析</h2>
            <p class="section-desc">先保存简历文件，再由用户决定是否进行智能解析。</p>
          </div>
          <span :class="['status-pill', parseStatusClass]">{{ parseStatusText }}</span>
        </div>

        <div class="upload-grid">
          <div class="resume-meta-card">
            <div class="meta-item">
              <span>档案 ID</span>
              <strong>{{ profileId || '上传简历后生成' }}</strong>
            </div>
            <div class="meta-item">
              <span>会员编号</span>
              <strong>{{ memberNo || '上传简历后生成' }}</strong>
            </div>
            <div class="meta-item">
              <span>当前文件</span>
              <strong>{{ resumeName || '未上传' }}</strong>
            </div>
            <div class="meta-item" v-if="resumeUrl">
              <span>文件预览</span>
              <a class="file-link" :href="resumeUrl" target="_blank" rel="noreferrer">打开 PDF</a>
            </div>
          </div>

          <div class="upload-box">
            <el-upload
              drag
              :auto-upload="false"
              :limit="1"
              accept="application/pdf,.pdf"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :file-list="fileList"
            >
              <div class="upload-visual">
                <div class="upload-icon">PDF</div>
                <div class="upload-title">拖拽 PDF 到此处，或点击选择文件</div>
                <div class="upload-hint">建议上传包含文本层的 PDF，解析速度会更快。</div>
              </div>
            </el-upload>

            <div class="resume-actions">
              <el-button type="primary" :loading="uploading" @click="handleUpload(false)">上传简历</el-button>
              <el-button type="success" :disabled="!profileId" :loading="parsing" @click="parseUploadedResume">解析当前简历</el-button>
              <el-button type="warning" plain :loading="uploading || parsing" @click="handleUpload(true)">上传并解析</el-button>
            </div>
          </div>
        </div>
      </section>

      <section class="section-panel form-panel">
        <div class="section-head">
          <div>
            <span class="section-kicker">Step 02</span>
            <h2 class="section-title">会员信息表单</h2>
            <p class="section-desc">解析结果会自动填入下列表单，请核对后提交。</p>
          </div>
          <el-tag effect="dark" round>{{ form.memberType || 'REGULAR' }}</el-tag>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="118px" class="member-form">
          <div class="form-group">
            <div class="form-group-title">基础身份信息</div>
            <el-row :gutter="18">
              <el-col :xs="24" :md="12">
                <el-form-item label="姓名" prop="memberName">
                  <el-input v-model="form.memberName" placeholder="请输入会员姓名" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入常用邮箱" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" placeholder="请输入联系电话" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="性别">
                  <el-select v-model="form.gender" clearable placeholder="请选择">
                    <el-option label="男" value="男" />
                    <el-option label="女" value="女" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="出生年份">
                  <el-input-number v-model="form.birthYear" :min="1900" :max="new Date().getFullYear()" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="国家或地区">
                  <el-input v-model="form.countryRegion" placeholder="例如：中国" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-group">
            <div class="form-group-title">学术与职业信息</div>
            <el-row :gutter="18">
              <el-col :xs="24" :md="12">
                <el-form-item label="所在单位" prop="organization">
                  <el-input v-model="form.organization" placeholder="请输入学校或机构" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="院系或部门">
                  <el-input v-model="form.department" placeholder="请输入院系或部门" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="职务或职称">
                  <el-input v-model="form.positionTitle" placeholder="例如：教授、研究员、博士生" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="最高学历">
                  <el-input v-model="form.highestDegree" placeholder="例如：博士、硕士、本科" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="专业领域">
                  <el-input v-model="form.professionalField" placeholder="请输入专业领域" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="会员类型">
                  <el-select v-model="form.memberType">
                    <el-option label="普通会员" value="REGULAR" />
                    <el-option label="学生会员" value="STUDENT" />
                    <el-option label="专家会员" value="EXPERT" />
                    <el-option label="理事会员" value="COUNCIL" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="研究方向">
                  <el-input v-model="form.researchDirection" type="textarea" :rows="3" placeholder="请输入研究方向，可由简历解析自动生成" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-group">
            <div class="form-group-title">学术主页与成果</div>
            <el-row :gutter="18">
              <el-col :xs="24" :md="12">
                <el-form-item label="个人主页">
                  <el-input v-model="form.homepage" placeholder="https://" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="ORCID">
                  <el-input v-model="form.orcid" placeholder="0000-0000-0000-0000" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="学术主页">
                  <el-input v-model="form.scholarProfile" placeholder="Google Scholar 或其他主页" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="个人简介">
                  <el-input v-model="form.personalBio" type="textarea" :rows="4" placeholder="请核对或补充个人简介" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="代表性成果">
                  <el-input v-model="form.representativeAchievements" type="textarea" :rows="5" placeholder="可填写项目、论文、获奖、代表性工作等" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注">
                  <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="其他补充说明" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-actions sticky-actions">
            <div class="action-note">提交前请确认邮箱、单位和研究方向等关键信息。</div>
            <div>
              <el-button @click="resetForm">清空表单</el-button>
              <el-button type="primary" :loading="submitting" @click="submitProfile">提交会员信息</el-button>
            </div>
          </div>
        </el-form>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { parseResume, skipParseResume, submitMemberProfile, uploadResume } from '../../api/societyMember'

const formRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const uploading = ref(false)
const parsing = ref(false)
const submitting = ref(false)
const profileId = ref(Number(localStorage.getItem('societyMemberProfileId') || 0) || null)
const memberNo = ref(localStorage.getItem('societyMemberNo') || '')
const resumeName = ref('')
const resumeUrl = ref('')
const parseStatus = ref('NOT_PARSED')

const form = reactive(createEmptyForm())
const draftStorageKey = 'societyMemberDraft'

const rules = {
  memberName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  organization: [{ required: true, message: '请输入所在单位', trigger: 'blur' }]
}

const parseStatusText = computed(() => {
  const map = {
    NOT_PARSED: '未解析',
    PARSING: '解析中',
    PARSED: '解析成功',
    PARSE_FAILED: '解析失败',
    SKIPPED: '已跳过解析',
    SUBMITTED: '已提交'
  }
  return map[parseStatus.value] || parseStatus.value || '未上传'
})

const parseStatusClass = computed(() => {
  const map = {
    NOT_PARSED: 'neutral',
    PARSING: 'processing',
    PARSED: 'success',
    PARSE_FAILED: 'danger',
    SKIPPED: 'warning',
    SUBMITTED: 'success'
  }
  return map[parseStatus.value] || 'neutral'
})

const completionPercent = computed(() => {
  const keys = [
    'memberName',
    'email',
    'phone',
    'organization',
    'highestDegree',
    'professionalField',
    'researchDirection',
    'personalBio'
  ]
  const filled = keys.filter(key => {
    const value = form[key]
    return value !== undefined && value !== null && String(value).trim() !== ''
  }).length
  return Math.round((filled / keys.length) * 100)
})

onMounted(() => {
  restoreDraft()
})

function createEmptyForm() {
  return {
    memberName: '',
    email: '',
    phone: '',
    gender: '',
    birthYear: null,
    countryRegion: '',
    organization: '',
    department: '',
    positionTitle: '',
    highestDegree: '',
    professionalField: '',
    researchDirection: '',
    personalBio: '',
    representativeAchievements: '',
    homepage: '',
    orcid: '',
    scholarProfile: '',
    memberType: 'REGULAR',
    remark: ''
  }
}

function onFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

function onFileRemove() {
  selectedFile.value = null
  fileList.value = []
}

async function handleUpload(needParse) {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 PDF 简历')
    return
  }

  const data = new FormData()
  data.append('file', selectedFile.value)

  uploading.value = true
  try {
    const uploadResponse = await uploadResume(data)
    applyUploadResponse(uploadResponse)

    if (needParse) {
      await parseUploadedResume()
    } else {
      await skipUploadedResume()
      ElMessage.success('简历已上传，未执行解析')
    }

    persistDraft()
  } finally {
    uploading.value = false
  }
}

function applyUploadResponse(response) {
  profileId.value = response.profileId
  memberNo.value = response.memberNo || ''
  resumeName.value = response.resumeOriginalName || ''
  resumeUrl.value = normalizeResumeUrl(response.resumeFileUrl || '')
  parseStatus.value = response.resumeParseStatus || 'NOT_PARSED'

  if (profileId.value) {
    localStorage.setItem('societyMemberProfileId', String(profileId.value))
  }
  if (memberNo.value) {
    localStorage.setItem('societyMemberNo', memberNo.value)
  }
}

async function parseUploadedResume() {
  if (!profileId.value) {
    ElMessage.warning('请先上传简历')
    return
  }

  parsing.value = true
  parseStatus.value = 'PARSING'
  persistDraft()

  try {
    const parsedResponse = await parseResume(profileId.value)
    parseStatus.value = parsedResponse.resumeParseStatus || 'PARSED'

    if (parsedResponse.form) {
      applyParsedData(parsedResponse.form)
      persistDraft()
    }

    ElMessage.success('简历解析成功，已自动填写表单')
  } catch (error) {
    parseStatus.value = 'PARSE_FAILED'
    persistDraft()
    ElMessage.error(error.message || '简历解析失败')
  } finally {
    parsing.value = false
  }
}

async function skipUploadedResume() {
  if (!profileId.value) {
    return
  }
  const response = await skipParseResume(profileId.value)
  parseStatus.value = response.resumeParseStatus || 'SKIPPED'
}

function applyParsedData(parsedData) {
  Object.keys(form).forEach(key => {
    const value = parsedData[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      form[key] = value
    }
  })
}

async function submitProfile() {
  if (!profileId.value) {
    ElMessage.warning('请先上传简历')
    return
  }

  await formRef.value.validate()
  submitting.value = true
  try {
    const response = await submitMemberProfile({ profileId: profileId.value, ...form })
    memberNo.value = response.memberNo || memberNo.value
    parseStatus.value = response.memberStatus || 'SUBMITTED'
    persistDraft()
    ElMessage.success('会员信息提交成功')
  } finally {
    submitting.value = false
  }
}

function persistDraft() {
  localStorage.setItem(draftStorageKey, JSON.stringify({
    profileId: profileId.value,
    memberNo: memberNo.value,
    resumeName: resumeName.value,
    resumeUrl: resumeUrl.value,
    parseStatus: parseStatus.value,
    form: { ...form }
  }))
}

function restoreDraft() {
  const draftText = localStorage.getItem(draftStorageKey)
  if (!draftText) {
    return
  }

  try {
    const draft = JSON.parse(draftText)
    profileId.value = draft.profileId || profileId.value
    memberNo.value = draft.memberNo || memberNo.value
    resumeName.value = draft.resumeName || ''
    resumeUrl.value = normalizeResumeUrl(draft.resumeUrl || '')
    parseStatus.value = draft.parseStatus || 'NOT_PARSED'
    Object.keys(form).forEach(key => {
      form[key] = draft.form?.[key] ?? createEmptyForm()[key]
    })
  } catch (error) {
    ElMessage.warning('本地草稿损坏，已忽略')
  }
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  persistDraft()
}

function normalizeResumeUrl(value) {
  if (!value) {
    return ''
  }

  const normalized = value.replace(/\\/g, '/').replace(/^\.\/?/, '')
  if (normalized.startsWith('uploads/')) {
    return `/${normalized}`
  }
  return normalized
}
</script>
