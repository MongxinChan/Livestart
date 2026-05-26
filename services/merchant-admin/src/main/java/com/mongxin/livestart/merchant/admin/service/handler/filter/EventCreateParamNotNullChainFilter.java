package com.mongxin.livestart.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dto.req.EventSaveReqDTO;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import static com.mongxin.livestart.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_EVENT_KEY;

/**
 * 验证演出创建接口参数是否正确 —— 必填参数非空校验
 */
@Component
public class EventCreateParamNotNullChainFilter implements MerchantAdminAbstractChainHandler<EventSaveReqDTO> {

    @Override
    public void handler(EventSaveReqDTO requestParam) {
        if (StrUtil.isEmpty(requestParam.getTitle())) {
            throw new ClientException("演出标题不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getEventType())) {
            throw new ClientException("演出类型不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getVenueId())) {
            throw new ClientException("关联场馆ID不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getStartTime())) {
            throw new ClientException("演出开始时间不能为空");
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CREATE_EVENT_KEY.name();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
