export interface BaseInfoFields {
  name: string
  phone: string
  email: string
  avatar: string
  gender: string
  birth: string
  city: string
}

export interface IntentionFields {
  position: string
  city: string
  salary: string
  entryTime: string
}

export interface EducationEntry {
  school: string
  major: string
  degree: string
  start: string
  end: string
  gpa: string
}

export interface ExperienceEntry {
  company: string
  position: string
  start: string
  end: string
  desc: string
}

export interface ProjectEntry {
  name: string
  role: string
  start: string
  end: string
  desc: string
}

export interface SkillEntry {
  name: string
  level: string
}

export interface ResumeContent {
  baseInfo: BaseInfoFields
  intention: IntentionFields
  education: EducationEntry[]
  experience: ExperienceEntry[]
  projects: ProjectEntry[]
  skills: SkillEntry[]
  evaluation: string
}

export type ModuleKey = 'baseInfo' | 'intention' | 'education' | 'experience' | 'projects' | 'skills' | 'evaluation'

export type ModuleVisibility = Record<ModuleKey, boolean>

export interface ResumeStyle {
  fontFamily: string
  fontSize: number
  lineHeight: number
  primaryColor: string
  color: string
}

export interface ResumeSnapshot {
  content: ResumeContent
  moduleOrder: ModuleKey[]
  moduleVisibility: ModuleVisibility
  style: ResumeStyle
}

export const DEFAULT_MODULE_ORDER: ModuleKey[] = [
  'baseInfo', 'intention', 'education', 'experience', 'projects', 'skills', 'evaluation'
]

export const ARRAY_MODULE_TEMPLATES: Record<string, Record<string, string>> = {
  education: { school: '', major: '', degree: '', start: '', end: '', gpa: '' },
  experience: { company: '', position: '', start: '', end: '', desc: '' },
  projects: { name: '', role: '', start: '', end: '', desc: '' }
}
