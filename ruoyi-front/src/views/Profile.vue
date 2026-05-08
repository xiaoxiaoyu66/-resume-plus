<template>
  <div class="profile-page">
    <div class="page-header">
      <div class="header-title">
        <h1>实习档案</h1>
        <p>填写个人信息，开启实习之旅</p>
      </div>
      <button class="save-btn" :disabled="saving" @click="handleSave">
        <el-icon v-if="saving"><Loading /></el-icon>
        <el-icon v-else><Check /></el-icon>
        <span>{{ saving ? '保存中...' : '保存档案' }}</span>
      </button>
    </div>

    <div class="profile-content">
      <!-- 基本信息 -->
      <div class="section">
        <div class="section-title">
          <span class="title-icon">
            <el-icon><User /></el-icon>
          </span>
          <span class="title-text">基本信息</span>
          <div class="title-line"></div>
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label>姓名 <span class="required">*</span></label>
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </div>
          <div class="form-item">
            <label>学校 <span class="required">*</span></label>
            <el-input v-model="form.school" placeholder="请输入学校名称" />
          </div>
          <div class="form-item">
            <label>专业 <span class="required">*</span></label>
            <el-input v-model="form.major" placeholder="请输入专业" />
          </div>
          <div class="form-item">
            <label>学历 <span class="required">*</span></label>
            <el-select v-model="form.education" placeholder="请选择学历" style="width: 100%">
              <el-option label="博士" value="博士" />
              <el-option label="硕士" value="硕士" />
              <el-option label="本科" value="本科" />
              <el-option label="大专" value="大专" />
            </el-select>
          </div>
          <div class="form-item">
            <label>毕业年份</label>
            <el-date-picker
              v-model="form.graduationYear"
              type="year"
              placeholder="选择毕业年份"
              style="width: 100%"
              value-format="YYYY"
            />
          </div>
          <div class="form-item">
            <label>联系电话</label>
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </div>
          <div class="form-item full-width">
            <label>邮箱</label>
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </div>
        </div>
      </div>

      <!-- 个人简介 -->
      <div class="section">
        <div class="section-title">
          <span class="title-icon">
            <el-icon><Document /></el-icon>
          </span>
          <span class="title-text">个人简介</span>
          <div class="title-line"></div>
        </div>
        <div class="form-item">
          <el-input
            v-model="form.selfIntro"
            type="textarea"
            :rows="5"
            placeholder="请简要介绍自己，包括个人特长、兴趣爱好、职业规划等..."
            maxlength="1000"
            show-word-limit
          />
        </div>
      </div>

      <!-- 项目经历 -->
      <div class="section">
        <div class="section-title">
          <span class="title-icon">
            <el-icon><FolderOpened /></el-icon>
          </span>
          <span class="title-text">项目经历</span>
          <div class="title-line"></div>
          <button class="add-btn" @click="addProject">
            <el-icon><Plus /></el-icon>
            添加项目
          </button>
        </div>

        <div v-for="(project, index) in form.projects" :key="index" class="project-card">
          <div class="project-header">
            <span class="project-number">项目 {{ index + 1 }}</span>
            <button class="delete-btn" @click="removeProject(index)">
              <el-icon><Delete /></el-icon>
            </button>
          </div>
          <div class="form-grid">
            <div class="form-item">
              <label>项目名称</label>
              <el-input v-model="project.projectName" placeholder="项目名称" />
            </div>
            <div class="form-item">
              <label>开始时间</label>
              <el-input v-model="project.startDate" placeholder="例如：2024.03" />
            </div>
            <div class="form-item">
              <label>角色</label>
              <el-select v-model="project.role" placeholder="担任角色" style="width: 100%">
                <el-option label="项目负责人" value="项目负责人" />
                <el-option label="核心开发" value="核心开发" />
                <el-option label="前端开发" value="前端开发" />
                <el-option label="后端开发" value="后端开发" />
                <el-option label="全栈开发" value="全栈开发" />
              </el-select>
            </div>
            <div class="form-item full-width">
              <label>项目描述</label>
              <el-input
                v-model="project.description"
                type="textarea"
                :rows="3"
                placeholder="描述项目内容、你的贡献和成果..."
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 技能标签 -->
      <div class="section">
        <div class="section-title">
          <span class="title-icon">
            <el-icon><CollectionTag /></el-icon>
          </span>
          <span class="title-text">技能标签</span>
          <div class="title-line"></div>
        </div>
        <div class="tags-wrapper">
          <el-tag
            v-for="(tag, index) in form.skills"
            :key="index"
            closable
            @close="handleCloseTag(index)"
            class="skill-tag"
            effect="dark"
            type="info"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="inputVisible"
            ref="inputRef"
            v-model="inputValue"
            size="small"
            class="tag-input"
            @keyup.enter="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <button
            v-else
            class="add-tag-btn"
            @click="showInput"
          >
            <el-icon><Plus /></el-icon>
            添加技能
          </button>
        </div>
      </div>

      <!-- 简历上传 -->
      <div class="section">
        <div class="section-title">
          <span class="title-icon">
            <el-icon><Upload /></el-icon>
          </span>
          <span class="title-text">简历附件</span>
          <div class="title-line"></div>
        </div>
        <div class="upload-area">
          <el-upload
            class="resume-uploader"
            action="/api/profile/upload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept=".pdf,.doc,.docx"
            :limit="1"
            drag
          >
            <div class="upload-content">
              <el-icon class="upload-icon"><Upload /></el-icon>
              <div class="upload-text">
                <span>点击或拖拽文件到此处上传</span>
                <p>支持 PDF、Word 格式，文件大小不超过 5MB</p>
              </div>
            </div>
          </el-upload>
          <div v-if="form.resumeUrl" class="file-info">
            <el-icon><Document /></el-icon>
            <span>{{ form.resumeName }}</span>
            <button class="delete-file-btn" @click="removeResume">
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, saveProfile } from '@/api/profile'

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  name: '',
  school: '',
  major: '',
  education: '',
  graduationYear: '',
  phone: '',
  email: '',
  selfIntro: '',
  projects: [],
  skills: [],
  resumeUrl: '',
  resumeName: ''
})

// 技能标签
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref(null)

function handleCloseTag(index) {
  form.skills.splice(index, 1)
}

function showInput() {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

function handleInputConfirm() {
  const value = inputValue.value.trim()
  if (value && !form.skills.includes(value)) {
    form.skills.push(value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 项目经历
function addProject() {
  form.projects.push({
    projectName: '',
    startDate: '',
    endDate: '',
    role: '',
    description: ''
  })
}

function removeProject(index) {
  form.projects.splice(index, 1)
}

// 文件上传
function beforeUpload(file) {
  const isValidType = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'].includes(file.type)
  const isValidSize = file.size / 1024 / 1024 < 5

  if (!isValidType) {
    ElMessage.error('请上传 PDF 或 Word 格式的文件')
    return false
  }
  if (!isValidSize) {
    ElMessage.error('文件大小不能超过 5MB')
    return false
  }
  return true
}

function handleUploadSuccess(res) {
  if (res.code === 200) {
    form.resumeUrl = res.data.url
    form.resumeName = res.data.name
    ElMessage.success('上传成功')
  }
}

function handleUploadError() {
  ElMessage.error('上传失败')
}

function removeResume() {
  form.resumeUrl = ''
  form.resumeName = ''
}

// 加载档案
async function loadProfile() {
  try {
    const res = await getProfile()
    if (res.data) {
      // 处理技能标签：如果是字符串则解析为数组
      if (res.data.skills) {
        if (typeof res.data.skills === 'string') {
          try {
            form.skills = JSON.parse(res.data.skills)
          } catch (e) {
            form.skills = []
          }
        } else if (Array.isArray(res.data.skills)) {
          form.skills = res.data.skills
        }
      }
      // 复制其他字段
      Object.assign(form, {
        ...res.data,
        skills: form.skills // 保持处理后的 skills
      })
    }
  } catch (e) {
    console.error('加载档案失败', e)
  }
}

// 保存档案
async function handleSave() {
  if (!form.name || !form.school || !form.major || !form.education) {
    ElMessage.warning('请完善基本信息')
    return
  }

  saving.value = true
  try {
    await saveProfile({ ...form })
    ElMessage.success('保存成功')
  } catch (e) {
    console.error('保存档案失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
// 水墨黑主题色
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$paper-gray: #f5f5f5;
$accent-red: #8b1a1a;

.profile-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  .header-title {
    h1 {
      font-size: 28px;
      font-weight: 600;
      color: $ink-black;
      font-family: 'Noto Serif SC', 'STSong', serif;
      margin-bottom: 8px;
    }

    p {
      font-size: 14px;
      color: $ink-pale;
    }
  }

  .save-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    background: $ink-black;
    color: $paper-white;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      background: $ink-deep;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

// 内容区域
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

// 区块
.section {
  background: $paper-white;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(0, 0, 0, 0.04);

  .section-title {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;

    .title-icon {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: $paper-gray;
      border-radius: 8px;
      color: $ink-mid;

      .el-icon {
        font-size: 18px;
      }
    }

    .title-text {
      font-size: 18px;
      font-weight: 600;
      color: $ink-black;
      font-family: 'Noto Serif SC', 'STSong', serif;
    }

    .title-line {
      flex: 1;
      height: 1px;
      background: linear-gradient(90deg, rgba(0,0,0,0.06) 0%, transparent 100%);
    }

    .add-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 16px;
      background: transparent;
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 6px;
      color: $ink-mid;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: $paper-gray;
        border-color: rgba(0, 0, 0, 0.15);
      }

      .el-icon {
        font-size: 14px;
      }
    }
  }
}

// 表单网格
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;

  .form-item {
    &.full-width {
      grid-column: 1 / -1;
    }

    label {
      display: block;
      font-size: 14px;
      color: $ink-mid;
      margin-bottom: 8px;
      font-weight: 500;

      .required {
        color: $accent-red;
        margin-left: 4px;
      }
    }

    :deep(.el-input__wrapper),
    :deep(.el-textarea__inner) {
      background: $paper-gray;
      border: 1px solid transparent;
      box-shadow: none;
      border-radius: 8px;
      transition: all 0.3s ease;

      &:hover {
        border-color: rgba(0, 0, 0, 0.1);
      }

      &.is-focus {
        border-color: $ink-black;
        background: $paper-white;
      }
    }

    :deep(.el-textarea__inner) {
      padding: 12px 16px;
      font-family: inherit;
    }
  }
}

// 项目卡片
.project-card {
  background: $paper-gray;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }

  .project-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .project-number {
      font-size: 14px;
      font-weight: 600;
      color: $ink-mid;
      font-family: 'Noto Serif SC', 'STSong', serif;
    }

    .delete-btn {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      border-radius: 6px;
      color: $ink-pale;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(139, 26, 26, 0.1);
        color: $accent-red;
      }
    }
  }
}

// 技能标签
.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;

  .skill-tag {
    font-size: 14px;
    padding: 6px 12px;
    border-radius: 6px;
    background: $ink-black;
    border: none;
  }

  .tag-input {
    width: 120px;
  }

  .add-tag-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    background: transparent;
    border: 1px dashed rgba(0, 0, 0, 0.2);
    border-radius: 6px;
    color: $ink-pale;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      border-color: $ink-mid;
      color: $ink-mid;
      background: rgba(0, 0, 0, 0.02);
    }
  }
}

// 上传区域
.upload-area {
  .upload-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 40px;

    .upload-icon {
      font-size: 48px;
      color: $ink-pale;
    }

    .upload-text {
      text-align: center;

      span {
        font-size: 15px;
        color: $ink-mid;
        font-weight: 500;
      }

      p {
        font-size: 13px;
        color: $ink-pale;
        margin-top: 4px;
      }
    }
  }

  :deep(.el-upload-dragger) {
    background: $paper-gray;
    border: 2px dashed rgba(0, 0, 0, 0.08);
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      border-color: $ink-mid;
      background: rgba(0, 0, 0, 0.02);
    }
  }

  .file-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    padding: 12px 16px;
    background: $paper-gray;
    border-radius: 8px;
    font-size: 14px;
    color: $ink-mid;

    .delete-file-btn {
      margin-left: auto;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      border-radius: 4px;
      color: $ink-pale;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(139, 26, 26, 0.1);
        color: $accent-red;
      }
    }
  }
}
</style>
