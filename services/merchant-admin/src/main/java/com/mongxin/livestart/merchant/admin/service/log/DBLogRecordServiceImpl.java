package com.mongxin.livestart.merchant.admin.service.log;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.merchant.admin.dao.entity.OperationLogDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.OperationLogMapper;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.ILogRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * mzt-biz-log 自定义日志持久化实现 —— 落库到 t_operation_log
 * <p>
 * 通过实现 {@link ILogRecordService} 接口，将 @LogRecord 注解拦截的操作日志
 * 写入 MySQL 审计日志表。按照 type 字段区分不同的业务模块（Event / TicketSku 等）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DBLogRecordServiceImpl implements ILogRecordService {

    private final OperationLogMapper operationLogMapper;
    private final RequestOperatorContext operatorContext;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LogRecord logRecord) {
        try {
            OperationLogDO operationLogDO = OperationLogDO.builder()
                    .tenant(logRecord.getTenant())
                    .type(logRecord.getType())
                    .subType(logRecord.getSubType())
                    .bizNo(logRecord.getBizNo())
                    .operatorId(logRecord.getOperator())
                    .operatorName(operatorContext.getOperatorName())
                    .operationLog(logRecord.getAction())
                    .originalData(Optional.ofNullable(LogRecordContext.getVariable("originalData"))
                            .map(Object::toString).orElse(null))
                    .modifiedData(StrUtil.isBlank(logRecord.getExtra()) ? null : logRecord.getExtra())
                    .fail(logRecord.isFail() ? 1 : 0)
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
        return queryLogs(bizNo, type, null);
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        return queryLogs(bizNo, type, subType);
    }

    private List<LogRecord> queryLogs(String bizNo, String type, String subType) {
        return operationLogMapper.selectList(Wrappers.lambdaQuery(OperationLogDO.class)
                        .eq(OperationLogDO::getBizNo, bizNo)
                        .eq(OperationLogDO::getType, type)
                        .eq(StrUtil.isNotBlank(subType), OperationLogDO::getSubType, subType)
                        .orderByDesc(OperationLogDO::getId))
                .stream()
                .map(this::toLogRecord)
                .collect(Collectors.toList());
    }

    private LogRecord toLogRecord(OperationLogDO source) {
        return LogRecord.builder()
                .id(source.getId())
                .tenant(source.getTenant())
                .type(source.getType())
                .subType(source.getSubType())
                .bizNo(source.getBizNo())
                .operator(source.getOperatorId())
                .action(source.getOperationLog())
                .extra(source.getModifiedData())
                .fail(Integer.valueOf(1).equals(source.getFail()))
                .createTime(source.getCreateTime())
                .build();
    }
}
