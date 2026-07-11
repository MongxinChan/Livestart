import { message, Modal } from 'ant-design-vue'
import type { ImportResult } from '@/types'

export function useExcelImport(importer: (file: File) => Promise<ImportResult>, onImported: () => void | Promise<void>) {
  const beforeUpload = async (file: File) => {
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      message.warning('请上传 .xlsx 或 .xls 格式的 Excel 文件')
      return false
    }
    if (file.size <= 0) {
      message.warning('上传的 Excel 文件不能为空')
      return false
    }

    try {
      const result = await importer(file)
      const errors = result.errors || []

      if (result.fail > 0) {
        const errorText = errors
          .slice(0, 10)
          .map((item) => `第 ${item.rowIndex} 行：${item.message || '导入失败'}`)
          .join('\n')
        Modal.warning({
          title: `导入完成：成功 ${result.success} 条，失败 ${result.fail} 条`,
          content: errorText || '请检查 Excel 数据后重试。',
        })
      } else {
        message.success(`导入成功，共 ${result.success} 条`)
      }

      if (errors.length > 10) {
        message.info(`仅展示前 10 条错误，另有 ${errors.length - 10} 条请调整后重新导入`)
      }
      await onImported()
    } catch (error) {
      console.error('Excel import failed', error)
      message.error(getImportErrorMessage(error))
    }
    return false
  }

  return {
    beforeUpload,
  }
}

function getImportErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '导入失败，请检查文件内容或稍后重试'
}
