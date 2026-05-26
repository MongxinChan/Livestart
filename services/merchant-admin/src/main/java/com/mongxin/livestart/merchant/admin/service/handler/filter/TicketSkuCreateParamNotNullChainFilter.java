package com.mongxin.livestart.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuSaveReqDTO;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import static com.mongxin.livestart.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_TICKET_SKU_KEY;

/**
 * 验证票种创建接口参数是否正确 —— 必填参数非空校验
 */
@Component
public class TicketSkuCreateParamNotNullChainFilter implements MerchantAdminAbstractChainHandler<TicketSkuSaveReqDTO> {

    @Override
    public void handler(TicketSkuSaveReqDTO requestParam) {
        if (ObjectUtil.isEmpty(requestParam.getEventId())) {
            throw new ClientException("关联演出ID不能为空");
        }

        if (StrUtil.isEmpty(requestParam.getTitle())) {
            throw new ClientException("票种名称不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getSellingPrice())) {
            throw new ClientException("售价不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getTotalStock()) || requestParam.getTotalStock() <= 0) {
            throw new ClientException("总库存必须为正整数");
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CREATE_TICKET_SKU_KEY.name();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
