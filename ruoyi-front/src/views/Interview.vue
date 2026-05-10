<template>
  <div class="interview-page">
    <div class="interview-hero">
      <div class="hero-icon">
        <el-icon :size="48"><ChatDotSquare /></el-icon>
      </div>
      <h1>模拟面试</h1>
      <p class="hero-desc">
        没面过试？不知道面试会问什么？选一个模式，AI 模拟真实面试官对你提问。<br>
        答完有评价、有改进建议，练多了自然不怯场。
      </p>
    </div>

    <div class="mode-cards">
      <!-- HR 面试 -->
      <div class="mode-card hr" @click="startInterview('hr')">
        <div class="card-bg"></div>
        <div class="card-content">
          <div class="card-icon">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <h2>HR 行为面试</h2>
          <ul class="card-features">
            <li>考察性格、稳定性、沟通表达</li>
            <li>挖掘求职动机、职业规划</li>
            <li>模拟真实 HR 的追问节奏</li>
          </ul>
          <div class="sample-questions">
            <p class="sample-label">可能会问：</p>
            <p class="sample-text">"你为什么想做这个岗位？"</p>
            <p class="sample-text">"你遇到最棘手的项目冲突是什么？"</p>
          </div>
          <div class="card-action">
            <el-button type="primary" size="large" round>
              开始 HR 面试
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 专业面试 -->
      <div class="mode-card pro" @click="startInterview('pro')">
        <div class="card-bg"></div>
        <div class="card-content">
          <div class="card-icon">
            <el-icon :size="32"><Tools /></el-icon>
          </div>
          <h2>专业面试</h2>
          <ul class="card-features">
            <li>考察岗位硬技能、项目深度</li>
            <li>场景题 + 技术/业务追问</li>
            <li>按目标岗位定制出题方向</li>
          </ul>
          <div class="sample-questions">
            <p class="sample-label">可能会问：</p>
            <p class="sample-text">"你的项目里数据指标是怎么拆解的？"</p>
            <p class="sample-text">"如果重新设计这个功能你会改哪里？"</p>
          </div>
          <div class="card-action">
            <el-button type="success" size="large" round>
              开始专业面试
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="interview-tips">
      <div class="tip-item">
        <el-icon :size="20"><InfoFilled /></el-icon>
        <span>不需要选岗位、难度——AI 从你的简历自动判断</span>
      </div>
      <div class="tip-item">
        <el-icon :size="20"><InfoFilled /></el-icon>
        <span>面完一种可以接着练另一种，两种都试试效果更好</span>
      </div>
      <div class="tip-item">
        <el-icon :size="20"><InfoFilled /></el-icon>
        <span>先去 <router-link to="/resume">简历编辑器</router-link> 填好简历，面试会更针对你的情况</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ChatDotSquare, User, Tools, ArrowRight, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()

function startInterview(mode: 'hr' | 'pro') {
  const sceneMap = { hr: 'interview-hr', pro: 'interview-pro' }
  router.push({
    path: '/chat',
    query: { scene: sceneMap[mode] }
  })
}
</script>

<style scoped lang="scss">
$ink-black: #1a1a1a;
$ink-deep: #252525;
$ink-mid: #3a3a3a;
$ink-light: #4a4a4a;
$ink-pale: #888888;
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$accent-red: #c45c48;
$accent-teal: #2d8a7a;

.interview-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px 60px;
}

.interview-hero {
  text-align: center;
  margin-bottom: 48px;

  .hero-icon {
    margin-bottom: 16px;
    color: $accent-red;
  }

  h1 {
    font-size: 32px;
    font-weight: 700;
    color: $ink-black;
    margin: 0 0 12px;
  }

  .hero-desc {
    font-size: 15px;
    color: $ink-pale;
    line-height: 1.7;
    margin: 0;
    max-width: 520px;
    margin: 0 auto;
  }
}

.mode-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 40px;
}

.mode-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  .card-bg {
    position: absolute;
    inset: 0;
    opacity: 0.04;
    transition: opacity 0.3s ease;
  }

  .card-content {
    position: relative;
    padding: 32px;
    z-index: 1;
  }

  &.hr {
    background: #fff;
    border: 1px solid rgba(64, 158, 255, 0.2);
    box-shadow: 0 4px 20px rgba(64, 158, 255, 0.06);

    .card-bg {
      background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
    }

    .card-icon {
      color: #409eff;
    }

    &:hover {
      border-color: #409eff;
      box-shadow: 0 8px 32px rgba(64, 158, 255, 0.15);
      transform: translateY(-4px);

      .card-bg {
        opacity: 0.06;
      }
    }
  }

  &.pro {
    background: #fff;
    border: 1px solid rgba(103, 194, 58, 0.2);
    box-shadow: 0 4px 20px rgba(103, 194, 58, 0.06);

    .card-bg {
      background: linear-gradient(135deg, #67c23a 0%, #529b2e 100%);
    }

    .card-icon {
      color: #67c23a;
    }

    &:hover {
      border-color: #67c23a;
      box-shadow: 0 8px 32px rgba(103, 194, 58, 0.15);
      transform: translateY(-4px);

      .card-bg {
        opacity: 0.06;
      }
    }
  }

  .card-icon {
    margin-bottom: 16px;
    transition: transform 0.3s ease;
  }

  &:hover .card-icon {
    transform: scale(1.1);
  }

  h2 {
    font-size: 22px;
    font-weight: 600;
    color: $ink-black;
    margin: 0 0 16px;
  }

  .card-features {
    list-style: none;
    padding: 0;
    margin: 0 0 20px;

    li {
      position: relative;
      padding: 4px 0 4px 20px;
      font-size: 14px;
      color: $ink-mid;
      line-height: 1.6;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 12px;
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }
    }
  }

  &.hr .card-features li::before {
    background: #409eff;
  }

  &.pro .card-features li::before {
    background: #67c23a;
  }

  .sample-questions {
    background: rgba(0, 0, 0, 0.02);
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 20px;

    .sample-label {
      font-size: 12px;
      color: $ink-pale;
      margin: 0 0 6px;
      font-weight: 500;
    }

    .sample-text {
      font-size: 13px;
      color: $ink-light;
      margin: 0 0 4px;
      line-height: 1.5;

      &:last-child {
        margin-bottom: 0;
      }

      &::before {
        content: '"';
        color: $ink-pale;
      }

      &::after {
        content: '"';
        color: $ink-pale;
      }
    }
  }

  .card-action {
    text-align: center;

    .el-button {
      padding: 12px 28px;
      font-size: 15px;
      font-weight: 500;

      .el-icon {
        margin-left: 4px;
      }
    }
  }
}

.interview-tips {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .tip-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    color: $ink-light;
    line-height: 1.5;

    .el-icon {
      color: $accent-red;
      flex-shrink: 0;
    }

    a {
      color: $accent-red;
      text-decoration: none;
      font-weight: 500;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 720px) {
  .mode-cards {
    grid-template-columns: 1fr;
  }

  .interview-page {
    padding: 24px 16px 40px;
  }
}
</style>
