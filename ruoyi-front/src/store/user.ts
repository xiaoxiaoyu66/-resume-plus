import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

interface LoginParams {
  username: string
  password: string
  code: string
  uuid: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    name: '',
    avatar: '',
    roles: [] as string[],
    permissions: [] as string[],
    phonenumber: '',
    email: '',
    createTime: ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    async login(userInfo: LoginParams) {
      const { username, password, code, uuid } = userInfo
      try {
        const res: any = await login(username, password, code, uuid)
        const data = res.token ? res : res.data
        const { token } = data
        this.token = token
        setToken(token)
        return Promise.resolve()
      } catch (error) {
        return Promise.reject(error)
      }
    },

    async getInfo() {
      try {
        const res: any = await getInfo()
        const data = res.user ? res : res.data
        const { user, roles, permissions } = data
        this.name = user.userName
        this.avatar = user.avatar || ''
        this.roles = roles || []
        this.permissions = permissions || []
        this.phonenumber = user.phonenumber || ''
        this.email = user.email || ''
        this.createTime = user.createTime || ''
        return Promise.resolve(data)
      } catch (error) {
        console.error('getInfo 错误:', error)
        return Promise.reject(error)
      }
    },

    async logout() {
      try {
        await logout()
      } finally {
        this.token = ''
        this.name = ''
        this.avatar = ''
        this.roles = []
        this.permissions = []
        this.phonenumber = ''
        this.email = ''
        this.createTime = ''
        removeToken()
      }
    },

    resetToken() {
      this.token = ''
      removeToken()
    }
  }
})
