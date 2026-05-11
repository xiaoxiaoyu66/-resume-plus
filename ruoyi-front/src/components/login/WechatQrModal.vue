<template>
  <div v-if="visible" class="qr-modal" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>微信扫码登录</h3>
        <button class="close-btn" @click="$emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <div class="modal-body">
        <div class="qr-large-frame">
          <div class="qr-ink-border"></div>
          <div v-if="!qrCodeUrl" class="qr-loading">
            <div class="ink-loading">
              <div class="brush-stroke"></div>
              <div class="brush-stroke"></div>
              <div class="brush-stroke"></div>
            </div>
            <p>正在研墨...</p>
          </div>
          <img v-else :src="qrCodeUrl" alt="微信扫码" class="qr-large">
          <div class="qr-corner tl"></div>
          <div class="qr-corner tr"></div>
          <div class="qr-corner bl"></div>
          <div class="qr-corner br"></div>
        </div>
        <p class="qr-modal-tip">请使用微信扫一扫登录</p>
      </div>
      <div class="modal-footer">
        <div class="seal-mark">简历+</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'

defineProps<{
  visible: boolean
  qrCodeUrl: string
}>()

defineEmits<{ close: [] }>()
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #404040;
$accent-red: #8b1a1a;

.qr-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: modal-fade-in 0.3s ease;

  .modal-content {
    background: rgba(252, 252, 250, 0.98);
    border-radius: 20px;
    padding: 32px;
    width: 90%;
    max-width: 400px;
    box-shadow: 0 25px 80px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.5);
    position: relative;
    animation: modal-scale-in 0.4s cubic-bezier(0.4, 0, 0.2, 1);

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.03'/%3E%3C/svg%3E");
      border-radius: 20px;
      pointer-events: none;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      position: relative;
      z-index: 2;

      h3 {
        font-size: 20px;
        font-weight: 600;
        color: $ink-black;
        font-family: 'Noto Serif SC', 'STSong', serif;
      }
      .close-btn {
        width: 36px; height: 36px;
        border: none; background: transparent;
        border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
        cursor: pointer; color: $ink-pale;
        transition: all 0.3s ease;
        &:hover { background: rgba(0, 0, 0, 0.05); color: $ink-black; }
        .el-icon { font-size: 20px; }
      }
    }

    .modal-body {
      display: flex; flex-direction: column; align-items: center;
      position: relative; z-index: 2;

      .qr-large-frame {
        position: relative; padding: 20px;
        background: white; border-radius: 12px;
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
        margin-bottom: 20px;

        .qr-ink-border {
          position: absolute; inset: 12px;
          border: 1px solid rgba(0, 0, 0, 0.06); border-radius: 8px;
          pointer-events: none;
        }

        .qr-loading {
          width: 200px; height: 200px;
          display: flex; flex-direction: column;
          align-items: center; justify-content: center;

          .ink-loading {
            width: 60px; height: 60px; position: relative; margin-bottom: 16px;
            .brush-stroke {
              position: absolute; background: $ink-mid;
              border-radius: 2px; animation: brush-paint 1.5s ease-in-out infinite;
              &:nth-child(1) { width: 40px; height: 4px; top: 20px; left: 10px; }
              &:nth-child(2) { width: 30px; height: 4px; top: 30px; left: 15px; animation-delay: 0.2s; }
              &:nth-child(3) { width: 35px; height: 4px; top: 40px; left: 12px; animation-delay: 0.4s; }
            }
          }
          p { color: $ink-pale; font-family: 'Noto Serif SC', 'STSong', serif; font-size: 14px; }
        }

        .qr-large { width: 200px; height: 200px; display: block; }

        .qr-corner {
          position: absolute; width: 20px; height: 20px;
          border: 3px solid $ink-black;
          &.tl { top: 12px; left: 12px; border-right: none; border-bottom: none; }
          &.tr { top: 12px; right: 12px; border-left: none; border-bottom: none; }
          &.bl { bottom: 12px; left: 12px; border-right: none; border-top: none; }
          &.br { bottom: 12px; right: 12px; border-left: none; border-top: none; }
        }
      }

      .qr-modal-tip { font-size: 14px; color: $ink-mid; font-family: 'Noto Serif SC', 'STSong', serif; }
    }

    .modal-footer {
      margin-top: 24px; display: flex; justify-content: center;
      position: relative; z-index: 2;
      .seal-mark {
        width: 48px; height: 48px;
        border: 2px solid $accent-red; color: $accent-red;
        display: flex; align-items: center; justify-content: center;
        font-size: 14px; font-weight: bold;
        font-family: 'Noto Serif SC', 'STSong', serif;
        transform: rotate(-8deg); opacity: 0.8; border-radius: 4px;
      }
    }
  }
}

@keyframes modal-fade-in { from { opacity: 0; } to { opacity: 1; } }

@keyframes modal-scale-in {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes brush-paint {
  0%, 100% { transform: scaleX(0); opacity: 0; }
  50% { transform: scaleX(1); opacity: 1; }
}
</style>
