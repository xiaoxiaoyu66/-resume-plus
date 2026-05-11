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
  schoolId: number | null
  major: string
  degree: string
  start: string
  end: string
  gpa: string
  courses: string[]
  _courseInputVisible?: boolean
  _courseValue?: string
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

export interface CampusEntry {
  organization: string
  position: string
  start: string
  end: string
  desc: string
}

export interface AwardEntry {
  name: string
  date: string
  level: string
}

export interface CertificateEntry {
  name: string
  date: string
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
  campus: CampusEntry[]
  projects: ProjectEntry[]
  awards: AwardEntry[]
  certificates: CertificateEntry[]
  skills: string[]
  evaluation: string
}

export type ModuleKey = 'baseInfo' | 'intention' | 'education' | 'experience' | 'campus' | 'projects' | 'awards' | 'certificates' | 'skills' | 'evaluation'

export type ModuleVisibility = Record<ModuleKey, boolean>

export interface ResumeStyle {
  fontFamily: string
  fontSize: number
  lineHeight: number
  primaryColor: string
  color: string
  paperBackground: string
}

export interface ResumeSnapshot {
  content: ResumeContent
  moduleOrder: ModuleKey[]
  moduleVisibility: ModuleVisibility
  style: ResumeStyle
}

export const DEFAULT_MODULE_ORDER: ModuleKey[] = [
  'baseInfo', 'intention', 'education', 'experience', 'campus', 'projects', 'skills', 'evaluation', 'awards', 'certificates'
]

export const DEFAULT_MODULE_VISIBILITY: ModuleVisibility = {
  baseInfo: true,
  intention: true,
  education: true,
  experience: true,
  campus: false,
  projects: false,
  awards: true,
  certificates: true,
  skills: true,
  evaluation: false
}

export const ARRAY_MODULE_TEMPLATES: Record<string, Record<string, any>> = {
  education: { school: '', schoolId: null, major: '', degree: '', start: '', end: '', gpa: '', courses: [] },
  experience: { company: '', position: '', start: '', end: '', desc: '' },
  campus: { organization: '', position: '', start: '', end: '', desc: '' },
  projects: { name: '', role: '', start: '', end: '', desc: '' },
  awards: { name: '', date: '', level: '' },
  certificates: { name: '', date: '' }
}
