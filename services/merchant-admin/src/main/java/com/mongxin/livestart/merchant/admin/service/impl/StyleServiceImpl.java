package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import org.springframework.stereotype.Service;

@Service
public class StyleServiceImpl extends ServiceImpl<StyleMapper, StyleDO> implements StyleService {
}
