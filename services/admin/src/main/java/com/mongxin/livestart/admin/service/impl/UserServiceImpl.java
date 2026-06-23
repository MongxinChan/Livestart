package com.mongxin.livestart.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_PHONE_INDEX_KEY;
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
    public UserLoginRespDTO register(UserRegisterReqDTO requestParam) {
        if (!availablePhone(requestParam.getPhone())) {
            throw new ClientException(UserErrorCodeEnum.PHONE_EXIST);
        }

        // 可选验证码校验：前端 client 端注册时传 code，后台管理端不传时跳过
        if (StrUtil.isNotBlank(requestParam.getCode())) {
            String cacheCode = stringRedisTemplate.opsForValue().get("login_code:" + requestParam.getPhone());
            if (cacheCode == null || !cacheCode.equals(requestParam.getCode())) {
                throw new ClientException("验证码错误或已失效");
            }
            // 校验通过，清理验证码缓存
            stringRedisTemplate.delete("login_code:" + requestParam.getPhone());
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
            // 注册即登录：直接为新用户签发 token
            return issueToken(userDO, requestParam.getPhone());
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
        // 复用未过期的旧会话：通过反向索引查到 token 后，确认主存仍存在再续期
        String existingToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + requestParam.getPhone());
        if (StrUtil.isNotBlank(existingToken)
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey(USER_LOGIN_KEY + existingToken))) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + existingToken, 30L, TimeUnit.MINUTES);
            stringRedisTemplate.expire(USER_LOGIN_PHONE_INDEX_KEY + requestParam.getPhone(), 30L, TimeUnit.MINUTES);
            return new UserLoginRespDTO(existingToken);
        }
        return issueToken(userDO, requestParam.getPhone());
    }

    @Override
    public Boolean checkLogin(String phone, String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(USER_LOGIN_KEY + token));
    }

    @Override
    public void logout(String phone, String token) {
        // 幂等退出：优先清理 token 维度主存，再清理反向索引；token 缺失时回退到反向索引查询
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + token);
        } else if (StrUtil.isNotBlank(phone)) {
            String existingToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + phone);
            if (StrUtil.isNotBlank(existingToken)) {
                stringRedisTemplate.delete(USER_LOGIN_KEY + existingToken);
            }
        }
        if (StrUtil.isNotBlank(phone)) {
            stringRedisTemplate.delete(USER_LOGIN_PHONE_INDEX_KEY + phone);
        }
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
        return issueToken(userDO, phone);
    }

    /**
     * 为已存在的用户签发 token：每次登录生成新 token，覆盖旧会话，确保单设备登录或多设备使用最新会话。
     */
    private UserLoginRespDTO issueToken(UserDO userDO, String phone) {
        // 删除旧会话（如果存在），确保每次登录都是全新的 token
        String oldToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + phone);
        if (StrUtil.isNotBlank(oldToken)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + oldToken);
        }

        // 生成新的 UUID token，主存 token→用户 JSON，反向索引 phone→token
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(USER_LOGIN_KEY + uuid, JSON.toJSONString(userDO), 30L, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(USER_LOGIN_PHONE_INDEX_KEY + phone, uuid, 30L, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public void updateUserType(String phone, Integer userType) {
        if (StrUtil.isBlank(phone) || userType == null) {
            throw new ClientException("手机号和用户类型不能为空");
        }
        if (userType < 1 || userType > 4) {
            throw new ClientException("用户类型必须在 1-4 之间");
        }

        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getPhone, phone)
                .eq(UserDO::getDelFlag, 0);

        UserDO userDO = new UserDO();
        userDO.setUserType(userType);

        int updated = baseMapper.update(userDO, updateWrapper);
        if (updated < 1) {
            throw new ClientException("用户不存在或更新失败");
        }

        // 更新后强制下次重新登录：通过反向索引清除 token 主存与索引本身
        String oldToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + phone);
        if (StrUtil.isNotBlank(oldToken)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + oldToken);
        }
        stringRedisTemplate.delete(USER_LOGIN_PHONE_INDEX_KEY + phone);
        log.info("已将手机号 {} 的用户类型更新为 {}，并清除登录缓存", phone, userType);
    }

    @Override
    public List<UserRespDTO> listSimpleUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectBatchIds(userIds).stream().map(userDO -> {
            UserRespDTO resp = new UserRespDTO();
            resp.setId(userDO.getId());
            resp.setUsername(userDO.getUsername());
            resp.setRealName(userDO.getRealName());
            resp.setPhone(userDO.getPhone());
            resp.setUserType(userDO.getUserType());
            resp.setStatus(userDO.getStatus());
            return resp;
        }).toList();
    }
}
