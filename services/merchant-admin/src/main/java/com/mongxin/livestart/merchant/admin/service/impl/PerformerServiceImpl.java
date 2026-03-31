package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 艺人/乐队服务实现层
 */
@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    /**
     * 创建艺人/乐队
     *
     * @param requestParam 艺人创建请求参数
     */
    @Override
    public void createPerformer(PerformerDO requestParam) {
        // 全局唯一性锁：确保艺人/乐队名称在系统中不冲突，为品牌IP护航
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .eq(PerformerDO::getName, requestParam.getName());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("艺人/乐队名称已存在，请勿重复录入");
        }
        save(requestParam);
    }

    /**
     * 获取所有艺人列表
     *
     * @return 艺人列表
     */
    @Override
    public List<PerformerDO> listAllPerformers() {
        return list();
    }

    /**
     * 更新艺人信息
     *
     * @param requestParam 艺人更新请求参数
     */
    @Override
    public void updatePerformer(PerformerDO requestParam) {
        updateById(requestParam);
    }

    /**
     * 删除艺人
     *
     * @param id 艺人ID
     */
    @Override
    public void deletePerformer(Long id) {
        removeById(id);
    }
}
