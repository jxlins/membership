<template>
  <main class="page-shell">
    <header class="page-header">
      <div>
        <h1 class="page-title">学会会员信息采集</h1>
        <p class="page-subtitle">上传当前 PDF 简历，确认识别结果后提交会员信息。</p>
      </div>
      <el-button v-if="memberNo" type="primary" plain @click="loadProfile(memberNo)">刷新会员信息</el-button>
    </header>

    <div class="content-stack">
      <section class="section-panel">
        <h2 class="section-title">简历上传</h2>
        <div class="upload-grid">
          <div class="resume-meta">
            <div>会员编号：{{ memberNo || '上传简历后生成' }}</div>
            <div>当前文件：{{ resumeName || '未上传' }}</div>
            <div>识别状态：{{ parseStatusText }}</div>
            <a v-if="resumeUrl" :href="resumeUrl" target="_blank" rel="noreferrer">打开当前简历</a>
          </div>

          <div>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="application/pdf,.pdf"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :file-list="fileList"
            >
              <el-button>选择 PDF 简历</el-button>
            </el-upload>
            <div class="resume-actions">
              <el-button type="primary" :loading="uploading" @click="handleUpload(true)">上传并解析</el-button>
              <el-button :loading="uploading" @click="handleUpload(false)">仅上传不解析</el-button>
            </div>
          </div>
        </div>
      </section>

      <section class="section-panel">
        <h2 class="section-title">会员信息</h2>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="128px">
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="姓名" prop="memberName">
                <el-input v-model="form.memberName" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="手机号">
                <el-input v-model="form.phone" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="性别">
                <el-select v-model="form.gender" clearable>
                  <el-option label="男" value="MALE" />
                  <el-option label="女" value="FEMALE" />
                  <el-option label="其他" value="OTHER" />
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
                <el-input v-model="form.countryRegion" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="所在单位" prop="organization">
                <el-input v-model="form.organization" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="院系或部门">
                <el-input v-model="form.department" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="职务或职称">
                <el-input v-model="form.positionTitle" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="最高学历">
                <el-input v-model="form.highestDegree" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="专业领域">
                <el-input v-model="form.professionalField" />
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
            <el-col :xs="24" :md="12">
              <el-form-item label="入会日期">
                <el-date-picker v-model="form.joinDate" type="date" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="个人主页">
                <el-input v-model="form.homepage" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="ORCID">
                <el-input v-model="form.orcid" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="学术主页">
                <el-input v-model="form.scholarProfile" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="研究方向">
                <el-input v-model="form.researchDirection" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="个人简介">
                <el-input v-model="form.personalBio" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="代表性成果">
                <el-input v-model="form.representativeAchievements" type="textarea" :rows="4" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="form-actions">
            <el-button @click="resetForm">清空表单</el-button>
            <el-button type="primary" :loading="submitting" @click="submitProfile">提交会员信息</el-button>
          </div>
        </el-form>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMemberProfile, submitMemberProfile, uploadResume } from '../../api/societyMember'

const formRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)
const uploading = ref(false)
const submitting = ref(false)
const memberNo = ref(localStorage.getItem('societyMemberNo') || '')
const resumeName = ref('')
const resumeUrl = ref('')
const parseStatus = ref('NOT_PARSED')

const form = reactive(createEmptyForm())

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
    SUCCESS: '解析成功',
    FAILED: '解析失败'
  }
  return map[parseStatus.value] || parseStatus.value || '未上传'
})

onMounted(() => {
  if (memberNo.value) {
    loadProfile(memberNo.value)
  }
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
    joinDate: '',
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
  if (memberNo.value) {
    data.append('memberNo', memberNo.value)
  }
  data.append('resumeFile', selectedFile.value)
  data.append('needParse', String(needParse))

  uploading.value = true
  try {
    const response = await uploadResume(data)
    memberNo.value = response.memberNo
    localStorage.setItem('societyMemberNo', response.memberNo)
    resumeName.value = response.resumeOriginalName
    resumeUrl.value = response.resumeFileUrl
    parseStatus.value = response.parseStatus

    if (response.parseStatus === 'SUCCESS' && response.parsedData) {
      try {
        await ElMessageBox.confirm('系统已识别新简历内容，是否使用识别结果更新当前表单？', '识别成功', {
          confirmButtonText: '使用识别结果',
          cancelButtonText: '暂不使用',
          type: 'info'
        })
        applyParsedData(response.parsedData)
      } catch (error) {
        ElMessage.info('已保留当前表单内容')
      }
    } else if (response.parseStatus === 'NOT_PARSED') {
      ElMessage.success('简历已上传，未执行解析')
    } else if (response.parseStatus === 'FAILED') {
      ElMessage.info('简历已上传，可继续手动填写表单')
    }
  } finally {
    uploading.value = false
  }
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
  if (!memberNo.value) {
    ElMessage.warning('请先上传简历')
    return
  }
  await formRef.value.validate()
  submitting.value = true
  try {
    await submitMemberProfile({ memberNo: memberNo.value, ...form })
    ElMessage.success('会员信息提交成功')
  } finally {
    submitting.value = false
  }
}

async function loadProfile(no) {
  const profile = await getMemberProfile(no)
  memberNo.value = profile.memberNo
  localStorage.setItem('societyMemberNo', profile.memberNo)
  resumeName.value = profile.resumeOriginalName || ''
  resumeUrl.value = profile.resumeFileUrl || ''
  parseStatus.value = profile.resumeParseStatus || 'NOT_PARSED'
  Object.keys(form).forEach(key => {
    form[key] = profile[key] ?? createEmptyForm()[key]
  })
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}
</script>
