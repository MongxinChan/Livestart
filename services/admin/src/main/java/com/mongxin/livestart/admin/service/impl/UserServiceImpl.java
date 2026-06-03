package com.mongxin.livestart.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;

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
import com.mongxin.livestart.admin.toolkit.MinioUtil;
import com.mongxin.livestart.admin.toolkit.OssUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import java.util.Map;

import java.util.concurrent.TimeUnit;

import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.mongxin.livestart.admin.common.enums.UserErrorCodeEnum.*;

/**
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    // 注入 Profile 以开启双表连通
    private final UserProfileMapper userProfileMapper;
    private final OssUtil ossUtil;
    private final MinioUtil minioUtil;

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

    @Override
    public String uploadAvatar(InputStream inputStream, String originalFilename) {
        if (inputStream == null) {
            throw new ClientException("文件不能为空");
        }
        return ossUtil.upload(inputStream, originalFilename);
    }

    @Override
    public String uploadAvatarByMinio(MultipartFile file) throws Exception {
        // 修正：将原先误写的 log.finalize 改为标准的日志输出
        log.info("【用户服务】开始通过本地 MinIO 上传用户头像, 文件名: {}", file.getOriginalFilename());

        // 1. 严谨的防御式判空，防止空文件进入业务流
        if (file == null || file.isEmpty()) {
            throw new ClientException("上传的头像文件不能为空");
        }

        // 2. 无需任何包装，直接把原生的 file 对象转发给底层工具类
        return minioUtil.upload(file);
    }

    @Override
    public IPage<UserRespDTO> pageUser(int current, int size) {
        Page<UserDO> page = new Page<>(current, size);
        Page<UserDO> userPage = baseMapper.selectPage(page, Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getDelFlag, 0));
        
        Page<UserRespDTO> resultPage = new Page<>(current, size, userPage.getTotal());
        java.util.List<UserRespDTO> records = userPage.getRecords().stream().map(userDO -> {
            UserRespDTO resp = new UserRespDTO();
            BeanUtils.copyProperties(userDO, resp);
            UserProfileDO profile = userProfileMapper.selectById(userDO.getId());
            if (profile != null) {
                BeanUtils.copyProperties(profile, resp);
            }
            return resp;
        }).collect(java.util.stream.Collectors.toList());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void sendCode(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new ClientException("手机号不能为空");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login_code:" + phone, code, 5, TimeUnit.MINUTES);
        log.info("【模拟短信通道】已向手机号 {} 发送登录验证码: {}", phone, code);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserLoginRespDTO loginByCode(String phone, String code) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new ClientException("手机号和验证码不能为空");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get("login_code:" + phone);
        if (cacheCode == null || !cacheCode.equals(code)) {
            throw new ClientException("验证码错误或已失效");
        }
        
        // 校验通过，清理验证码缓存
        stringRedisTemplate.delete("login_code:" + phone);

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, phone)
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);

        if (userDO == null) {
            // 用户不存在，隐式自动注册
            userDO = new UserDO();
            userDO.setPhone(phone);
            userDO.setUsername("Live_" + RandomUtil.randomString(4));
            // 自动注册的默认加密密码，安全占位
            userDO.setPassword(BCrypt.hashpw("LiveStart123", BCrypt.gensalt()));
            userDO.setIsVerified(0);
            userDO.setStatus(1);
            userDO.setUserType(1);
            
            try {
                int inserted = baseMapper.insert(userDO);
                if (inserted < 1) {
                    throw new ClientException("自动注册插入失败");
                }
                // 初始化社交档案
                UserProfileDO userProfileDO = new UserProfileDO();
                userProfileDO.setUserId(userDO.getId());
                userProfileMapper.insert(userProfileDO);

                // 同步加入布隆过滤器以保证全局判定正确
                userRegisterCachePenetrationBloomFilter.add(phone);
            } catch (DuplicateKeyException ex) {
                // 并发重复注册保护，重新查一次
                userDO = baseMapper.selectOne(queryWrapper);
                if (userDO == null) {
                    throw new ClientException("用户注册并发异常，请稍后重试");
                }
            }
        }

        // 签发 Token (复用之前的 Hash Token 设计)
        java.util.Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash()
                .entries(USER_LOGIN_KEY + phone);
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + phone, 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("登录会话生成失败"));
            return new UserLoginRespDTO(token);
        }

        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + phone, uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY + phone, 30L, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }
}
