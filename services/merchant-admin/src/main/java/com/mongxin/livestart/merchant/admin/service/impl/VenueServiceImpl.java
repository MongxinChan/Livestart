package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import org.springframework.stereotype.Service;

@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueDO> implements VenueService {
}
