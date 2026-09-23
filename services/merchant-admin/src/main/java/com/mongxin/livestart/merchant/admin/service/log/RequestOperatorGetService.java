package com.mongxin.livestart.merchant.admin.service.log;

import com.mzt.logapi.beans.Operator;
import com.mzt.logapi.service.IOperatorGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 覆盖 mzt-biz-log 默认的演示操作人 111。
 */
@Service
@RequiredArgsConstructor
public class RequestOperatorGetService implements IOperatorGetService {

    private final RequestOperatorContext operatorContext;

    @Override
    public Operator getUser() {
        return new Operator(operatorContext.getOperatorId());
    }
}
