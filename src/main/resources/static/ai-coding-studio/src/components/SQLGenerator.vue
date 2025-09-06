<template>
  <div class="sql-generator">
    <!-- 需求输入区域 -->
    <div class="requirement-input">
      <a-card title="📝 需求描述" class="input-card">
        <a-form layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="作业名称" :required="true">
                <a-input 
                  v-model:value="requirement.jobName" 
                  placeholder="例如：用户日活跃度统计作业"
                  @change="onRequirementChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="业务域">
                <a-select 
                  v-model:value="requirement.businessDomain" 
                  placeholder="选择业务域"
                  @change="onRequirementChange"
                >
                  <a-select-option value="wrongbook">错题本</a-select-option>
                  <a-select-option value="answer">答题</a-select-option>
                  <a-select-option value="user">用户</a-select-option>
                  <a-select-option value="learning">学习分析</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          
          <a-form-item label="自然语言描述" :required="true">
            <a-textarea 
              v-model:value="requirement.naturalLanguageDescription"
              :rows="6"
              placeholder="请详细描述您的业务需求，例如：&#10;我需要统计每个用户在错题本中的订正情况，包括订正次数、成功率、涉及的知识点等，按天进行聚合，并且需要关联用户信息和知识点信息，结果保存到ODPS表中。"
              @change="onRequirementChange"
              @input="onInputChange"
            />
          </a-form-item>
          
          <!-- AI实时建议 -->
          <div v-if="aiSuggestions.length > 0" class="ai-suggestions">
            <a-alert 
              message="🤖 AI建议" 
              type="info" 
              show-icon
              :description="aiSuggestions.join('、')"
            />
          </div>
        </a-form>
        
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <a-button 
            type="primary" 
            size="large"
            :loading="generating"
            @click="generateSQL"
            :disabled="!canGenerate"
          >
            <template #icon><ThunderboltOutlined /></template>
            智能生成SQL
          </a-button>
          
          <a-button @click="analyzeRequirement" :loading="analyzing">
            <template #icon><EyeOutlined /></template>
            分析需求
          </a-button>
          
          <a-button @click="showTemplates">
            <template #icon><FileTextOutlined /></template>
            选择模板
          </a-button>
        </div>
      </a-card>
    </div>

    <!-- SQL生成结果区域 -->
    <div v-if="generationResult" class="generation-result">
      <a-card title="🎯 生成结果" class="result-card">
        <a-tabs v-model:activeKey="activeTab">
          <!-- SQL代码标签页 -->
          <a-tab-pane key="sql" tab="生成的SQL">
            <div class="sql-editor-container">
              <div class="editor-toolbar">
                <a-space>
                  <a-button size="small" @click="formatSQL">
                    <template #icon><FormatPainterOutlined /></template>
                    格式化
                  </a-button>
                  <a-button size="small" @click="copySQL">
                    <template #icon><CopyOutlined /></template>
                    复制
                  </a-button>
                  <a-button size="small" @click="downloadSQL">
                    <template #icon><DownloadOutlined /></template>
                    下载
                  </a-button>
                </a-space>
                
                <a-tag :color="getConfidenceColor(generationResult.aiAnalysis.confidence)">
                  置信度: {{ Math.round(generationResult.aiAnalysis.confidence * 100) }}%
                </a-tag>
              </div>
              
              <!-- Monaco编辑器 -->
              <div id="sql-editor" class="sql-editor"></div>
            </div>
          </a-tab-pane>
          
          <!-- 质量检查标签页 -->
          <a-tab-pane key="quality" tab="质量检查">
            <QualityReport :quality-result="generationResult.qualityResult" />
          </a-tab-pane>
          
          <!-- 优化建议标签页 -->
          <a-tab-pane key="optimization" tab="优化建议">
            <OptimizationSuggestions :suggestions="generationResult.optimizations" />
          </a-tab-pane>
          
          <!-- 配置文件标签页 -->
          <a-tab-pane key="config" tab="配置文件">
            <ConfigFileViewer :config="generationResult.generatedConfig" />
          </a-tab-pane>
          
          <!-- AI分析标签页 -->
          <a-tab-pane key="analysis" tab="AI分析">
            <AIAnalysisReport :analysis="generationResult.aiAnalysis" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>

    <!-- 需求分析结果 -->
    <a-modal 
      v-model:visible="analysisModalVisible" 
      title="📊 需求分析结果"
      width="800px"
      :footer="null"
    >
      <RequirementAnalysis v-if="analysisResult" :analysis="analysisResult" />
    </a-modal>

    <!-- 模板选择器 -->
    <a-modal 
      v-model:visible="templateModalVisible" 
      title="📋 选择模板"
      width="1000px"
      :footer="null"
    >
      <TemplateSelector @select="onTemplateSelect" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { 
  ThunderboltOutlined, 
  EyeOutlined, 
  FileTextOutlined,
  FormatPainterOutlined,
  CopyOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import * as monaco from 'monaco-editor'
import { aiCodingApi } from '@/api/ai-coding'
import QualityReport from './QualityReport.vue'
import OptimizationSuggestions from './OptimizationSuggestions.vue'
import ConfigFileViewer from './ConfigFileViewer.vue'
import AIAnalysisReport from './AIAnalysisReport.vue'
import RequirementAnalysis from './RequirementAnalysis.vue'
import TemplateSelector from './TemplateSelector.vue'

// 响应式数据
const requirement = reactive({
  jobName: '',
  businessDomain: '',
  naturalLanguageDescription: '',
  userId: 'current-user', // 实际应用中从用户上下文获取
  type: 'NATURAL_LANGUAGE',
  priority: 'MEDIUM'
})

const generating = ref(false)
const analyzing = ref(false)
const generationResult = ref(null)
const analysisResult = ref(null)
const activeTab = ref('sql')
const analysisModalVisible = ref(false)
const templateModalVisible = ref(false)
const aiSuggestions = ref([])

let sqlEditor = null

// 计算属性
const canGenerate = computed(() => {
  return requirement.jobName.trim() && 
         requirement.naturalLanguageDescription.trim().length > 10
})

// 生命周期
onMounted(() => {
  initializeSQLEditor()
  loadUserHistory()
})

// 监听器
watch(() => requirement.naturalLanguageDescription, (newValue) => {
  if (newValue.length > 20) {
    debounceGetAISuggestions(newValue)
  }
}, { immediate: false })

// 方法
const onRequirementChange = () => {
  // 需求变更时的处理
  saveToLocalStorage()
}

const onInputChange = () => {
  // 实时输入变化
  if (requirement.naturalLanguageDescription.length > 50) {
    // 触发实时分析
    debounceAnalyzeInput()
  }
}

const generateSQL = async () => {
  if (!canGenerate.value) {
    message.warning('请完善需求信息')
    return
  }

  generating.value = true
  
  try {
    const response = await aiCodingApi.generateSQL(requirement)
    
    if (response.success) {
      generationResult.value = response.data
      
      // 更新SQL编辑器内容
      if (sqlEditor) {
        sqlEditor.setValue(response.data.generatedSQL)
      }
      
      // 显示成功消息
      message.success(`SQL生成成功！置信度: ${Math.round(response.data.aiAnalysis.confidence * 100)}%`)
      
      // 自动切换到SQL标签页
      activeTab.value = 'sql'
      
      // 如果质量分数较低，提示用户查看优化建议
      if (response.data.qualityResult.overallScore < 0.8) {
        message.info('检测到可优化项，建议查看优化建议标签页')
      }
      
    } else {
      throw new Error(response.message)
    }
    
  } catch (error) {
    console.error('SQL生成失败:', error)
    message.error(`SQL生成失败: ${error.message}`)
  } finally {
    generating.value = false
  }
}

const analyzeRequirement = async () => {
  if (!requirement.naturalLanguageDescription.trim()) {
    message.warning('请先输入需求描述')
    return
  }

  analyzing.value = true
  
  try {
    const response = await aiCodingApi.analyzeRequirement({
      description: requirement.naturalLanguageDescription,
      businessDomain: requirement.businessDomain
    })
    
    if (response.success) {
      analysisResult.value = response.data
      analysisModalVisible.value = true
      
      // 根据分析结果更新AI建议
      updateAISuggestions(response.data)
      
    } else {
      throw new Error(response.message)
    }
    
  } catch (error) {
    console.error('需求分析失败:', error)
    message.error(`需求分析失败: ${error.message}`)
  } finally {
    analyzing.value = false
  }
}

const showTemplates = () => {
  templateModalVisible.value = true
}

const onTemplateSelect = (template) => {
  // 应用选中的模板
  requirement.jobName = template.name
  requirement.naturalLanguageDescription = template.description
  templateModalVisible.value = false
  
  message.success('模板已应用')
}

const initializeSQLEditor = () => {
  const editorContainer = document.getElementById('sql-editor')
  if (editorContainer) {
    sqlEditor = monaco.editor.create(editorContainer, {
      value: '-- 生成的SQL将显示在这里',
      language: 'sql',
      theme: 'vs-dark',
      fontSize: 14,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      automaticLayout: true
    })
  }
}

const formatSQL = () => {
  if (sqlEditor) {
    sqlEditor.getAction('editor.action.formatDocument').run()
  }
}

const copySQL = async () => {
  if (sqlEditor) {
    const sql = sqlEditor.getValue()
    try {
      await navigator.clipboard.writeText(sql)
      message.success('SQL已复制到剪贴板')
    } catch (error) {
      message.error('复制失败')
    }
  }
}

const downloadSQL = () => {
  if (sqlEditor) {
    const sql = sqlEditor.getValue()
    const blob = new Blob([sql], { type: 'text/sql' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${requirement.jobName || 'generated'}.sql`
    a.click()
    URL.revokeObjectURL(url)
  }
}

const getConfidenceColor = (confidence) => {
  if (confidence >= 0.9) return 'green'
  if (confidence >= 0.7) return 'orange'
  return 'red'
}

// 防抖函数
const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

const debounceGetAISuggestions = debounce(async (text) => {
  try {
    // 获取AI实时建议
    const suggestions = await aiCodingApi.getRealtimeSuggestions(text)
    aiSuggestions.value = suggestions
  } catch (error) {
    console.error('获取AI建议失败:', error)
  }
}, 1000)

const debounceAnalyzeInput = debounce(() => {
  // 实时分析输入内容
  console.log('实时分析输入内容')
}, 2000)

const saveToLocalStorage = () => {
  localStorage.setItem('ai-coding-requirement', JSON.stringify(requirement))
}

const loadUserHistory = () => {
  try {
    const saved = localStorage.getItem('ai-coding-requirement')
    if (saved) {
      Object.assign(requirement, JSON.parse(saved))
    }
  } catch (error) {
    console.error('加载历史数据失败:', error)
  }
}

const updateAISuggestions = (analysis) => {
  const suggestions = []
  
  if (analysis.extractedTables.length > 0) {
    suggestions.push(`识别到表: ${analysis.extractedTables.join(', ')}`)
  }
  
  if (analysis.estimatedComplexity) {
    suggestions.push(`复杂度评估: ${analysis.estimatedComplexity}`)
  }
  
  if (analysis.suggestedTemplates.length > 0) {
    suggestions.push(`推荐模板: ${analysis.suggestedTemplates[0].name}`)
  }
  
  aiSuggestions.value = suggestions
}
</script>

<style scoped>
.sql-generator {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.input-card, .result-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.action-buttons {
  margin-top: 20px;
  text-align: center;
}

.action-buttons .ant-btn {
  margin: 0 8px;
}

.ai-suggestions {
  margin-top: 16px;
}

.sql-editor-container {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #fafafa;
  border-bottom: 1px solid #d9d9d9;
}

.sql-editor {
  height: 400px;
}

.generation-result {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
