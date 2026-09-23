package com.mongxin.livestart.admin.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.admin.common.biz.user.UserContext;
import com.mongxin.livestart.admin.common.convention.exception.ClientException;
import com.mongxin.livestart.admin.common.enums.UserErrorCodeEnum;
import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dao.entity.UserPhoneMappingDO;
import com.mongxin.livestart.admin.dao.entity.UserProfileDO;
import com.mongxin.livestart.admin.dao.entity.VenueDO;
import com.mongxin.livestart.admin.dao.mapper.UserMapper;
import com.mongxin.livestart.admin.dao.mapper.UserPhoneMappingMapper;
import com.mongxin.livestart.admin.dao.mapper.UserProfileMapper;
import com.mongxin.livestart.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.mongxin.livestart.admin.service.UserService;
import com.mongxin.livestart.admin.toolkit.MinioUtil;
import com.mongxin.livestart.admin.toolkit.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.mongxin.livestart.admin.common.constant.RedisCacheConstant.USER_LOGIN_PHONE_INDEX_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private static final String LOGIN_CODE_KEY_PREFIX = "login_code:";
    private static final String LOGIN_CODE_SEND_PHONE_KEY_PREFIX = "login_code:send:phone:";
    private static final String LOGIN_CODE_SEND_IP_KEY_PREFIX = "login_code:send:ip:";
    private static final String LOGIN_CODE_ATTEMPT_KEY_PREFIX = "login_code:attempt:";
    private static final String RATE_LIMIT_LUA_PATH = "lua/login_code_rate_limit.lua";
    private static final String VERIFY_CODE_LUA_PATH = "lua/login_code_verify.lua";
    private static final int CODE_MAX_ATTEMPTS = 5;
    private static final long CODE_EXPIRE_MINUTES = 5L;
    private static final long LOGIN_SESSION_DAYS = 30L;

    @Value("${livestart.sms.mock-log-enabled:true}")
    private boolean mockSmsLogEnabled;
    @Value("${livestart.sms.mock-fixed-code:}")
    private String mockFixedCode;
    @Value("${spring.profiles.active:}")
    private String activeProfiles;
    @Value("${livestart.admin.internal-token:change-me}")
    private String internalToken;

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserPhoneMappingMapper userPhoneMappingMapper;
    private final UserProfileMapper userProfileMapper;
    private final VenueMapper venueMapper;
    private final OssUtil ossUtil;
    private final MinioUtil minioUtil;

    private static final int USER_TYPE_FAN = 1;
    private static final int USER_TYPE_ARTIST = 2;
    private static final int USER_TYPE_VENUE_ADMIN = 3;
    private static final int USER_TYPE_SUPER_ADMIN = 4;
    private static final int USER_STATUS_BANNED = 0;
    private static final int USER_STATUS_NORMAL = 1;

    @Override
    public UserRespDTO getUserByPhone(String phone) {
        UserDO userDO = findMappedUser(normalizePhone(phone));
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        UserProfileDO userProfileDO = userProfileMapper.selectById(userDO.getId());
        if (userProfileDO != null) {
            BeanUtils.copyProperties(userProfileDO, result);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        String currentPhone = UserContext.getPhone();
        if (StrUtil.isBlank(currentPhone)) {
            throw new ClientException("当前用户未登录，请重新登录");
        }
        if (StrUtil.isNotBlank(requestParam.getPhone()) && !currentPhone.equals(requestParam.getPhone().trim())) {
            throw new ClientException("只能修改当前登录用户的资料");
        }
        String targetPhone = currentPhone;

        Long currentUserId = currentUserId();
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, currentUserId)
                .eq(UserDO::getPhone, targetPhone)
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }

        UserDO userUpdate = new UserDO();
        userUpdate.setId(userDO.getId());
        if (StrUtil.isNotBlank(requestParam.getUsername())) {
            userUpdate.setUsername(requestParam.getUsername().trim());
        }
        if (StrUtil.isNotBlank(requestParam.getRealName())) {
            userUpdate.setRealName(requestParam.getRealName().trim());
        }
        baseMapper.updateById(userUpdate);

        UserProfileDO profileUpdate = new UserProfileDO();
        profileUpdate.setUserId(userDO.getId());
        profileUpdate.setMail(trimToNull(requestParam.getMail()));
        profileUpdate.setAvatar(trimToNull(requestParam.getAvatar()));
        profileUpdate.setGender(requestParam.getGender());
        profileUpdate.setBirthday(requestParam.getBirthday());
        profileUpdate.setSignature(trimToNull(requestParam.getSignature()));

        UserProfileDO existingProfile = userProfileMapper.selectById(userDO.getId());
        if (existingProfile == null) {
            userProfileMapper.insert(profileUpdate);
        } else {
            userProfileMapper.updateById(profileUpdate);
        }
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    @Override
    public Boolean checkLogin(String token) {
        String currentPhone = UserContext.getPhone();
        if (StrUtil.isBlank(token) || StrUtil.isBlank(currentPhone)) {
            return false;
        }
        String payload = stringRedisTemplate.opsForValue().get(USER_LOGIN_KEY + token);
        if (StrUtil.isBlank(payload)) {
            return false;
        }
        UserDO user = JSON.parseObject(payload, UserDO.class);
        return user != null && currentPhone.equals(user.getPhone());
    }

    @Override
    public void logout(String token) {
        String currentPhone = UserContext.getPhone();
        if (StrUtil.isBlank(currentPhone) || StrUtil.isBlank(token)) {
            throw new ClientException("当前用户未登录");
        }
        String payload = stringRedisTemplate.opsForValue().get(USER_LOGIN_KEY + token);
        UserDO user = StrUtil.isBlank(payload) ? null : JSON.parseObject(payload, UserDO.class);
        if (user == null || !currentPhone.equals(user.getPhone())) {
            throw new ClientException("登录凭证与当前用户不匹配");
        }
        stringRedisTemplate.delete(USER_LOGIN_KEY + token);
        stringRedisTemplate.delete(USER_LOGIN_PHONE_INDEX_KEY + currentPhone);
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
        log.info("【用户服务】开始通过本地 MinIO 上传用户头像, 文件名: {}", file.getOriginalFilename());
        if (file == null || file.isEmpty()) {
            throw new ClientException("上传的头像文件不能为空");
        }
        return minioUtil.upload(file);
    }

    @Override
    public IPage<UserRespDTO> pageUser(int current, int size, String sortField, String sortOrder, Integer userType, String phone) {
        assertSuperAdmin();
        Page<UserDO> page = new Page<>(current, size);
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getDelFlag, 0);
        if (userType != null) {
            if (userType < USER_TYPE_FAN || userType > USER_TYPE_SUPER_ADMIN) {
                throw new ClientException("\u7528\u6237\u7c7b\u578b\u5fc5\u987b\u5728 1-4 \u4e4b\u95f4");
            }
            queryWrapper.eq(UserDO::getUserType, userType);
        }
        if (StrUtil.isNotBlank(phone)) {
            String phoneKeyword = phone.trim();
            if (!phoneKeyword.matches("\\d{3,11}")) {
                throw new ClientException("\u624b\u673a\u53f7\u641c\u7d22\u4ec5\u652f\u6301\u8f93\u5165 3-11 \u4f4d\u6570\u5b57");
            }
            queryWrapper.like(UserDO::getPhone, phoneKeyword);
        }
        if ("id".equalsIgnoreCase(sortField)) {
            boolean asc = "ascend".equalsIgnoreCase(sortOrder) || "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy(true, asc, UserDO::getId);
        } else {
            queryWrapper.orderByDesc(UserDO::getId);
        }

        Page<UserDO> userPage = baseMapper.selectPage(page, queryWrapper);
        Page<UserRespDTO> resultPage = new Page<>(current, size, userPage.getTotal());
        List<UserRespDTO> records = userPage.getRecords().stream().map(userDO -> {
            UserRespDTO resp = new UserRespDTO();
            BeanUtils.copyProperties(userDO, resp);
            UserProfileDO profile = userProfileMapper.selectById(userDO.getId());
            if (profile != null) {
                BeanUtils.copyProperties(profile, resp);
            }
            return resp;
        }).toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void sendCode(String phone, String clientIp) {
        phone = normalizePhone(phone);
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new ClientException("请输入有效的手机号");
        }
        if (isProductionProfile()) {
            throw new ClientException("生产环境短信通道未配置，暂无法发送验证码");
        }
        if (StrUtil.isBlank(clientIp)) {
            clientIp = "unknown";
        }
        enforceRateLimit(LOGIN_CODE_SEND_PHONE_KEY_PREFIX + phone, 1, 60, "该手机号操作频繁，请 60 秒后再试");
        enforceRateLimit(LOGIN_CODE_SEND_IP_KEY_PREFIX + clientIp, 20, 60, "请求过于频繁，请稍后再试");

        String code = mockSmsLogEnabled
                && mockFixedCode != null && mockFixedCode.matches("^\\d{6}$")
                ? mockFixedCode
                : RandomUtil.randomNumbers(6);
        String codeKey = LOGIN_CODE_KEY_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        stringRedisTemplate.delete(LOGIN_CODE_ATTEMPT_KEY_PREFIX + phone);
        if (mockSmsLogEnabled) {
            log.info("【模拟短信通道】已向手机号 {} 发送登录验证码: {}", phone, code);
        } else {
            log.info("【短信通道】验证码已发送，phone={}", maskPhone(phone));
        }
    }

    private String normalizePhone(String phone) {
        return StrUtil.trimToEmpty(phone);
    }

    private void enforceRateLimit(String key, int maxCount, long expireSeconds, String message) {
        Long count = stringRedisTemplate.execute(
                loadLongRedisScript(RATE_LIMIT_LUA_PATH),
                Collections.singletonList(key),
                String.valueOf(expireSeconds));
        if (count == null) {
            throw new IllegalStateException("验证码限流执行失败");
        }
        if (count > maxCount) {
            throw new ClientException(message);
        }
    }

    private void verifyLoginCode(String phone, String code) {
        phone = normalizePhone(phone);
        if (!phone.matches("^1[3-9]\\d{9}$") || code == null || !code.matches("^\\d{6}$")) {
            throw new ClientException("手机号或验证码格式错误");
        }
        Long result = stringRedisTemplate.execute(
                loadLongRedisScript(VERIFY_CODE_LUA_PATH),
                List.of(LOGIN_CODE_KEY_PREFIX + phone, LOGIN_CODE_ATTEMPT_KEY_PREFIX + phone),
                code,
                String.valueOf(CODE_MAX_ATTEMPTS),
                String.valueOf(TimeUnit.MINUTES.toSeconds(CODE_EXPIRE_MINUTES)));
        if (result == null) {
            throw new IllegalStateException("验证码校验执行失败");
        }
        if (result != 1L) {
            throw new ClientException("验证码错误或已失效");
        }
    }

    private DefaultRedisScript<Long> loadLongRedisScript(String classpath) {
        return Singleton.get(classpath, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setLocation(new ClassPathResource(classpath));
            script.setResultType(Long.class);
            return script;
        });
    }

    private boolean isProductionProfile() {
        return StrUtil.splitTrim(activeProfiles, ',').stream()
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private String maskPhone(String phone) {
        return phone.length() == 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "****";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserLoginRespDTO loginByCode(String phone, String code) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new ClientException("手机号和验证码不能为空");
        }
        phone = normalizePhone(phone);
        verifyLoginCode(phone, code);

        UserDO userDO = findOrMigrateUser(phone);

        if (userDO == null) {
            long userId = IdWorker.getId();
            UserPhoneMappingDO mapping = claimPhone(phone, userId);
            if (!Long.valueOf(userId).equals(mapping.getUserId())) {
                userDO = requireMappedUser(mapping, phone);
            } else {
                userDO = new UserDO();
                userDO.setId(userId);
                userDO.setPhone(phone);
                userDO.setUsername("Live_" + RandomUtil.randomString(4));
                userDO.setPassword(BCrypt.hashpw(UUID.fastUUID().toString(true), BCrypt.gensalt()));
                userDO.setIsVerified(0);
                userDO.setStatus(1);
                userDO.setUserType(1);

                int inserted = baseMapper.insert(userDO);
                if (inserted < 1) {
                    throw new ClientException("自动注册插入失败");
                }
                UserProfileDO userProfileDO = new UserProfileDO();
                userProfileDO.setUserId(userDO.getId());
                userProfileMapper.insert(userProfileDO);
                userRegisterCachePenetrationBloomFilter.add(phone);
            }
        }

        if (Integer.valueOf(0).equals(userDO.getStatus())) {
            throw new ClientException("该账户已被封禁，请联系超级管理员");
        }

        return issueToken(userDO, phone);
    }

    private UserDO findOrMigrateUser(String phone) {
        UserPhoneMappingDO mapping = userPhoneMappingMapper.selectById(phone);
        if (mapping != null) {
            return requireMappedUser(mapping, phone);
        }

        UserDO legacyUser = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, phone)
                .eq(UserDO::getDelFlag, 0));
        if (legacyUser == null) {
            return null;
        }
        UserPhoneMappingDO claimed = claimPhone(phone, legacyUser.getId());
        if (!legacyUser.getId().equals(claimed.getUserId())) {
            return requireMappedUser(claimed, phone);
        }
        return legacyUser;
    }

    private UserDO findMappedUser(String phone) {
        UserPhoneMappingDO mapping = userPhoneMappingMapper.selectById(phone);
        if (mapping == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        return requireMappedUser(mapping, phone);
    }

    private UserPhoneMappingDO claimPhone(String phone, Long userId) {
        UserPhoneMappingDO candidate = new UserPhoneMappingDO();
        candidate.setPhone(phone);
        candidate.setUserId(userId);
        try {
            if (userPhoneMappingMapper.insert(candidate) < 1) {
                throw new ClientException("手机号全局路由创建失败");
            }
            return candidate;
        } catch (DuplicateKeyException ex) {
            UserPhoneMappingDO existing = userPhoneMappingMapper.selectById(phone);
            if (existing == null) {
                throw new ClientException("手机号注册并发异常，请稍后重试");
            }
            return existing;
        }
    }

    private UserDO requireMappedUser(UserPhoneMappingDO mapping, String phone) {
        UserDO user = baseMapper.selectById(mapping.getUserId());
        if (user == null) {
            user = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getPhone, phone));
        }
        if (user == null || !phone.equals(user.getPhone())) {
            throw new ClientException("手机号路由与用户数据不一致，请联系管理员");
        }
        if (Integer.valueOf(1).equals(user.getDelFlag())) {
            throw new ClientException("该账户已注销");
        }
        return user;
    }

    private Long currentUserId() {
        try {
            return Long.valueOf(UserContext.getUserId());
        } catch (NumberFormatException ex) {
            throw new ClientException("当前用户身份无效，请重新登录");
        }
    }

    private UserLoginRespDTO issueToken(UserDO userDO, String phone) {
        String oldToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + phone);
        if (StrUtil.isNotBlank(oldToken)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + oldToken);
        }

        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(
                USER_LOGIN_KEY + uuid, JSON.toJSONString(userDO), LOGIN_SESSION_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(
                USER_LOGIN_PHONE_INDEX_KEY + phone, uuid, LOGIN_SESSION_DAYS, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserType(Long userId, Integer userType) {
        assertSuperAdmin();
        if (userId == null || userType == null) {
            throw new ClientException("\u7528\u6237ID\u548c\u7528\u6237\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (userType != USER_TYPE_FAN && userType != USER_TYPE_ARTIST && userType != USER_TYPE_VENUE_ADMIN) {
            throw new ClientException("\u7528\u6237\u7c7b\u578b\u53ea\u80fd\u5207\u6362\u4e3a\u666e\u901a\u7528\u6237\u3001\u827a\u4eba\u6216\u573a\u5730\u7ba1\u7406\u5458");
        }

        UserDO existingUser = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (existingUser == null) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664");
        }
        assertNotSelf(existingUser.getId(), "\u4e0d\u80fd\u4fee\u6539\u81ea\u5df1\u7684\u7528\u6237\u7c7b\u578b");
        assertTargetNotSuperAdmin(existingUser);
        if (userType == USER_TYPE_VENUE_ADMIN) {
            throw new ClientException("\u8bbe\u7f6e\u573a\u5730\u7ba1\u7406\u5458\u5fc5\u987b\u5148\u7ed1\u5b9a\u573a\u9986");
        }

        UserDO userDO = new UserDO();
        userDO.setUserType(userType);
        int updated = baseMapper.update(userDO, Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (updated < 1) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u66f4\u65b0\u5931\u8d25");
        }

        clearLoginCache(existingUser.getPhone());
        unbindOwnedVenues(existingUser.getId());
        log.info("Updated user type and cleared login cache, userId={}, userType={}", userId, userType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        assertSuperAdmin();
        if (userId == null || status == null) {
            throw new ClientException("\u7528\u6237ID\u548c\u8d26\u53f7\u72b6\u6001\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (status != USER_STATUS_BANNED && status != USER_STATUS_NORMAL) {
            throw new ClientException("\u8d26\u53f7\u72b6\u6001\u5fc5\u987b\u4e3a 0 \u6216 1");
        }

        UserDO existingUser = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (existingUser == null) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664");
        }
        assertNotSelf(existingUser.getId(), "\u4e0d\u80fd\u5c01\u7981\u6216\u89e3\u5c01\u81ea\u5df1");
        assertTargetNotSuperAdmin(existingUser);

        UserDO userDO = new UserDO();
        userDO.setStatus(status);
        int updated = baseMapper.update(userDO, Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (updated < 1) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u66f4\u65b0\u5931\u8d25");
        }

        clearLoginCache(existingUser.getPhone());
        log.info("Updated user status and cleared login cache, userId={}, status={}", userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindVenueAdmin(Long userId, Long venueId) {
        assertSuperAdmin();
        if (userId == null || venueId == null) {
            throw new ClientException("\u7528\u6237ID\u548c\u573a\u9986ID\u4e0d\u80fd\u4e3a\u7a7a");
        }

        UserDO existingUser = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (existingUser == null) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664");
        }
        assertNotSelf(existingUser.getId(), "\u4e0d\u80fd\u628a\u81ea\u5df1\u8bbe\u7f6e\u4e3a\u573a\u5730\u7ba1\u7406\u5458");
        assertTargetNotSuperAdmin(existingUser);
        if (Integer.valueOf(USER_STATUS_BANNED).equals(existingUser.getStatus())) {
            throw new ClientException("\u5c01\u7981\u7528\u6237\u4e0d\u80fd\u8bbe\u7f6e\u4e3a\u573a\u5730\u7ba1\u7406\u5458");
        }

        VenueDO venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new ClientException("\u573a\u9986\u4e0d\u5b58\u5728");
        }
        if (venue.getOwnerUserId() != null && !venue.getOwnerUserId().equals(userId)) {
            throw new ClientException("\u8be5\u573a\u9986\u5df2\u7ed1\u5b9a\u5176\u4ed6\u573a\u5730\u7ba1\u7406\u5458");
        }

        UserDO userDO = new UserDO();
        userDO.setUserType(USER_TYPE_VENUE_ADMIN);
        int updated = baseMapper.update(userDO, Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDelFlag, 0));
        if (updated < 1) {
            throw new ClientException("\u7528\u6237\u4e0d\u5b58\u5728\u6216\u66f4\u65b0\u5931\u8d25");
        }

        unbindOwnedVenues(userId);
        int bound = venueMapper.update(null, Wrappers.lambdaUpdate(VenueDO.class)
                .eq(VenueDO::getId, venueId)
                .and(each -> each.isNull(VenueDO::getOwnerUserId)
                        .or()
                        .eq(VenueDO::getOwnerUserId, userId))
                .set(VenueDO::getOwnerUserId, userId));
        if (bound < 1) {
            throw new ClientException("该场馆已绑定其他场地管理员，请刷新后重试");
        }
        clearLoginCache(existingUser.getPhone());
        log.info("Bound venue admin and cleared login cache, userId={}, venueId={}", userId, venueId);
    }

    private void unbindOwnedVenues(Long userId) {
        if (userId == null) {
            return;
        }
        venueMapper.update(null, Wrappers.lambdaUpdate(VenueDO.class)
                .eq(VenueDO::getOwnerUserId, userId)
                .set(VenueDO::getOwnerUserId, null));
    }

    private void clearLoginCache(String phone) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        String oldToken = stringRedisTemplate.opsForValue().get(USER_LOGIN_PHONE_INDEX_KEY + phone);
        if (StrUtil.isNotBlank(oldToken)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + oldToken);
        }
        stringRedisTemplate.delete(USER_LOGIN_PHONE_INDEX_KEY + phone);
    }


    private void assertSuperAdmin() {
        String currentUserId = UserContext.getUserId();
        if (StrUtil.isBlank(currentUserId)) {
            throw new ClientException("\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u64cd\u4f5c\u7528\u6237\u7ba1\u7406");
        }
        UserDO currentUser = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, Long.valueOf(currentUserId))
                .eq(UserDO::getDelFlag, 0));
        if (currentUser == null || !Integer.valueOf(USER_TYPE_SUPER_ADMIN).equals(currentUser.getUserType())) {
            throw new ClientException("\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u64cd\u4f5c\u7528\u6237\u7ba1\u7406");
        }
    }

    private void assertNotSelf(Long targetUserId, String message) {
        String currentUserId = UserContext.getUserId();
        if (targetUserId != null && targetUserId.toString().equals(currentUserId)) {
            throw new ClientException(message);
        }
    }

    private void assertTargetNotSuperAdmin(UserDO existingUser) {
        if (existingUser != null && Integer.valueOf(USER_TYPE_SUPER_ADMIN).equals(existingUser.getUserType())) {
            throw new ClientException("\u4e0d\u80fd\u5728\u7528\u6237\u7ba1\u7406\u4e2d\u4fee\u6539\u8d85\u7ea7\u7ba1\u7406\u5458\u8d26\u53f7");
        }
    }

    @Override
    public List<UserRespDTO> listSimpleUsersByIds(List<Long> userIds, String requestInternalToken) {
        if (StrUtil.isBlank(requestInternalToken) || !requestInternalToken.equals(internalToken)) {
            throw new ClientException("内部调用凭证无效");
        }
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
