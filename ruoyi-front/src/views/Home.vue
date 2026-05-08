<template>
  <div class="home-page">
    <div class="hero-section">
      <h1 class="title">若依 AI 助手</h1>
      <p class="subtitle">基于水墨画风格的智能对话系统</p>
      
      <div class="action-buttons">
        <SmButton type="primary" size="large" @click="$router.push('/chat')">
          <el-icon><ChatDotRound /></el-icon>
          开始对话
        </SmButton>
        <SmButton type="secondary" size="large" @click="$router.push('/profile')">
          <el-icon><User /></el-icon>
          完善档案
        </SmButton>
        <SmButton type="secondary" size="large" @click="$router.push('/resume')">
          <el-icon><Document /></el-icon>
          简历编辑器
        </SmButton>
      </div>
    </div>
    
    <div class="features">
      <div class="feature-item">
        <div class="icon">
          <el-icon><ChatLineRound /></el-icon>
        </div>
        <h3>智能对话</h3>
        <p>基于 DeepSeek AI 的智能对话，解答您的各种问题</p>
      </div>
      <div class="feature-item">
        <div class="icon">
          <el-icon><Document /></el-icon>
        </div>
        <h3>实习档案</h3>
        <p>完善个人信息和项目经历，助力实习申请</p>
      </div>
      <div class="feature-item" @click="$router.push('/resume')" style="cursor: pointer;">
        <div class="icon">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <h3>简历编辑器</h3>
        <p>3分钟填完，直接投递。支持多套模板，一键导出PDF</p>
      </div>
      <div class="feature-item" @click="$router.push('/resume')" style="cursor: pointer;">
        <div class="icon">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <h3>简历编辑器</h3>
        <p>3分钟填完，直接投递。支持多套模板，一键导出PDF</p>
      </div>
      <div class="feature-item" @click="$router.push('/resume')" style="cursor: pointer;">
        <div class="icon">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <h3>简历编辑器</h3>
        <p>3分钟填完，直接投递。支持多套模板，一键导出PDF</p>
      </div>
      <div class="feature-item" @click="$router.push('/resume')" style="cursor: pointer;">
        <div class="icon">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <h3>简历编辑器</h3>
        <p>3分钟填完，直接投递。支持多套模板，一键导出PDF</p>
      </div>
      <div class="feature-item">
        <div class="icon">
          <el-icon><Brush /></el-icon>
        </div>
        <h3>水墨风格</h3>
        <p>独特的中国传统水墨画风格界面设计</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/components/ShuimoUI/styles/variables.scss';
@import '@/components/ShuimoUI/styles/mixins.scss';

.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

.hero-section {
  text-align: center;
  margin-bottom: 80px;

  .title {
    font-size: 48px;
    font-weight: 700;
    color: $ink-deep;
    font-family: $font-family;
    letter-spacing: 8px;
    margin-bottom: 16px;
  }

  .subtitle {
    font-size: 20px;
    color: $ink-light;
    margin-bottom: 40px;
    letter-spacing: 2px;
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 20px;
  }
}

.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;

  .feature-item {
    text-align: center;
    padding: 40px 30px;
    background: $paper-white;
    border: 1px solid $border-light;
    border-radius: $radius-lg;
    box-shadow: 0 2px 8px $shadow-soft;
    transition: all $transition-base;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px $shadow-mid;
    }

    .icon {
      width: 64px;
      height: 64px;
      margin: 0 auto 20px;
      background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      .el-icon {
        font-size: 32px;
        color: $paper-white;
      }
    }

    h3 {
      font-size: $font-size-xl;
      color: $ink-deep;
      margin-bottom: 12px;
      font-family: $font-family;
    }

    p {
      font-size: $font-size-base;
      color: $ink-light;
      line-height: 1.6;
    }
  }
}

@media (max-width: 768px) {
  .features {
    grid-template-columns: 1fr;
  }
}
</style>
