package com.mongxin.livestart.merchant.admin.service.log;

import cn.hutool.core.util.StrUtil;
import com.mongxin.livestart.merchant.admin.dao.entity.OperationLogDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.OperationLogMapper;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.ILogRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * mzt-biz-log 自定义日志持久化实现 —— 落库到 t_operation_log
 * <p>
 * 通过实现 {@link ILogRecordService} 接口，将 @LogRecord 注解拦截的操作日志
 * 异步写入 MySQL 审计日志表。按照 type 字段区分不同的业务模块（Event / TicketSku 等）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DBLogRecordServiceImpl implements ILogRecordService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(LogRecord logRecord) {
        try {
            OperationLogDO operationLogDO = OperationLogDO.builder()
                    .type(logRecord.getType())
                    .bizNo(logRecord.getBizNo())
                    .operatorId(logRecord.getOperator())
                    .operatorName(logRecord.getOperator()) // 暂与 operatorId 相同，后续对接网关可扩展
                    .operationLog(logRecord.getAction())
                    .originalData(Optional.ofNullable(LogRecordContext.getVariable("originalData"))
                            .map(Object::toString).orElse(null))
                    .modifiedData(StrUtil.isBlank(logRecord.getExtra()) ? null : logRecord.getExtra())
                    .build();

            operationLogMapper.insert(operationLogDO);
            log.info("[操作日志] 已落库 | type={} | bizNo={} | operator={}",
                    logRecord.getType(), logRecord.getBizNo(), logRecord.getOperator());
        } catch (Exception ex) {
            log.error("[操作日志] 记录 [{}] 操作日志失败", logRecord.getType(), ex);
        }
    }

    @Override
    public List<LogRecord> queryLog(String bizNo, String type) {
        return List.of();
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        return List.of();
    }
}
