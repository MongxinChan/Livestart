package com.mongxin.livestart.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mongxin.livestart.admin.common.biz.user.UserContext;
import com.mongxin.livestart.admin.common.convention.exception.ClientException;
import com.mongxin.livestart.admin.common.enums.UserErrorCodeEnum;
import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dao.entity.UserProfileDO;
import com.mongxin.livestart.admin.dao.mapper.UserMapper;
import com.mongxin.livestart.admin.dao.mapper.UserProfileMapper;
import com.mongxin.livestart.admin.dto.req.UserLoginReqDTO;
import com.mongxin.livestart.admin.dto.req.UserRegisterReqDTO;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.mongxin.livestart.admin.service.UserService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.mongxin.livestart.admin.common.enums.UserErrorCodeEnum.*;

/**
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    // 注入 Profile 以开启双表连通
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserRespDTO getUserByPhone(String phone) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, phone);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        // 主副表拼装：追加社交资料档案
        UserProfileDO userProfileDO = userProfileMapper.selectById(userDO.getId());
        if (userProfileDO != null) {
            BeanUtils.copyProperties(userProfileDO, result);
        }
        return result;
    }

    @Override
    public Boolean availablePhone(String phone) {
        // 如果布隆过滤器存在 phone，说明不可以用
        return !userRegisterCachePenetrationBloomFilter.contains(phone);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (!availablePhone(requestParam.getPhone())) {
            throw new ClientException(UserErrorCodeEnum.PHONE_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getPhone());
        if (!lock.tryLock()) {
            throw new ClientException(PHONE_EXIST);
            // 这儿的逻辑是如果获取不到锁就抛用户名存在，有先者A先行，其大概率不会出错，而后者B,C如果刚好别人在获取锁，
            // 而且还没获取到，就证明比别人慢，避免其在此阻塞，直接抛异常
        }
        try {
            UserDO userDO = BeanUtil.toBean(requestParam, UserDO.class);
            
            // 盲盒逻辑：如果未传网名，系统默认按 Live_随机数 分发
            if (StrUtil.isBlank(userDO.getUsername())) {
                userDO.setUsername("Live_" + RandomUtil.randomString(4));
            }
            
            // 改写：生产级密文哈希加盐入库
            userDO.setPassword(BCrypt.hashpw(requestParam.getPassword(), BCrypt.gensalt()));
            int inserted = baseMapper.insert(userDO);
            if (inserted < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            // 连坐：顺滑插入初始空社交档案，保障 ID 底座一致！
            UserProfileDO userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(userDO.getId());
            userProfileMapper.insert(userProfileDO);

            userRegisterCachePenetrationBloomFilter.add(requestParam.getPhone());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 必须要先获取由于查询传来的 user 的唯一性主键ID
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, requestParam.getPhone());
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            return;
        }

        // 第一步：更新核心基座数据表 (DO)
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getPhone, requestParam.getPhone());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);

        // 第二步：将社交相关的例如性别，签名同步录入从表 (ProfileDO)
        UserProfileDO userProfileDO = BeanUtil.toBean(requestParam, UserProfileDO.class);
        userProfileDO.setUserId(userDO.getId());
        userProfileMapper.updateById(userProfileDO);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, requestParam.getPhone())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        // 安全拦截：禁止等值明文比对，强硬升级为 CheckPW 解密器校验
        if (userDO == null || !BCrypt.checkpw(requestParam.getPassword(), userDO.getPassword())) {
            throw new ClientException("该手机号绑定的用户不存在或密文校验失败！！！！");
        }
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash()
                .entries(USER_LOGIN_KEY + requestParam.getPhone());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getPhone(), 30L,
                    TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登陆错误"));
            return new UserLoginRespDTO(token);
        }
        /**
         * Hash
         * Key：login_用户名
         * Value：
         * Key：token标识
         * Val：JSON 字符串（用户信息）
         */
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash()
                .put(USER_LOGIN_KEY + requestParam.getPhone(), uuid,
                        JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getPhone(), 30L,
                TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String phone, String token) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + phone, token) != null;
    }

    @Override
    public void logout(String phone, String token) {
        if (checkLogin(phone, token)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + phone);
            return;
        }
        throw new ClientException("用户的缓存登录态存在异常或已下线");
    }
}
