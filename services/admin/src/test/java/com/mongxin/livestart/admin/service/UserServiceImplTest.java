package com.mongxin.livestart.admin.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mongxin.livestart.admin.common.biz.user.UserContext;
import com.mongxin.livestart.admin.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.admin.common.convention.exception.ClientException;
import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dao.entity.UserPhoneMappingDO;
import com.mongxin.livestart.admin.dao.entity.VenueDO;
import com.mongxin.livestart.admin.dao.mapper.UserMapper;
import com.mongxin.livestart.admin.dao.mapper.UserPhoneMappingMapper;
import com.mongxin.livestart.admin.dao.mapper.UserProfileMapper;
import com.mongxin.livestart.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.admin.service.impl.UserServiceImpl;
import com.mongxin.livestart.admin.toolkit.MinioUtil;
import com.mongxin.livestart.admin.toolkit.OssUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private RBloomFilter<String> bloomFilter;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserPhoneMappingMapper userPhoneMappingMapper;
    @Mock
    private VenueMapper venueMapper;
    @Mock
    private OssUtil ossUtil;
    @Mock
    private MinioUtil minioUtil;
    @Mock
    private UserMapper userMapper;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), VenueDO.class);
        service = new UserServiceImpl(bloomFilter, stringRedisTemplate,
                userPhoneMappingMapper, userProfileMapper, venueMapper, ossUtil, minioUtil);
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        UserContext.setUser(UserInfoDTO.builder().userId("7").phone("13800138000").build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldUseUnpredictablePasswordPlaceholderForCodeRegisteredUser() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("login_code:13800138000", "login_code:attempt:13800138000")),
                eq("123456"), eq("5"), eq("300"))).thenReturn(1L);
        when(valueOperations.get("live-start:login:user:13800138000")).thenReturn(null);
        when(userPhoneMappingMapper.selectById("13800138000")).thenReturn(null);
        when(userPhoneMappingMapper.insert(any(UserPhoneMappingDO.class))).thenReturn(1);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
            invocation.<UserDO>getArgument(0).setId(7L);
            return 1;
        });
        when(userProfileMapper.insert(any())).thenReturn(1);

        service.loginByCode("13800138000", "123456");

        ArgumentCaptor<UserDO> userCaptor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).insert(userCaptor.capture());
        String storedPassword = userCaptor.getValue().getPassword();
        assertNotNull(storedPassword);
        assertFalse(BCrypt.checkpw("LiveStart123", storedPassword));
    }

    @Test
    void shouldRouteExistingLoginByGlobalPhoneMapping() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("login_code:13800138000", "login_code:attempt:13800138000")),
                eq("123456"), eq("5"), eq("300"))).thenReturn(1L);
        UserPhoneMappingDO mapping = new UserPhoneMappingDO();
        mapping.setPhone("13800138000");
        mapping.setUserId(7L);
        when(userPhoneMappingMapper.selectById("13800138000")).thenReturn(mapping);
        when(userMapper.selectById(7L)).thenReturn(user("13800138000"));

        service.loginByCode("13800138000", "123456");

        verify(userMapper).selectById(7L);
        verify(userMapper, never()).selectOne(any());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void shouldFallbackToPhoneLookupForLegacyMisroutedUser() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("login_code:13800138000", "login_code:attempt:13800138000")),
                eq("123456"), eq("5"), eq("300"))).thenReturn(1L);
        UserPhoneMappingDO mapping = new UserPhoneMappingDO();
        mapping.setPhone("13800138000");
        mapping.setUserId(7L);
        when(userPhoneMappingMapper.selectById("13800138000")).thenReturn(mapping);
        when(userMapper.selectById(7L)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user("13800138000"));

        service.loginByCode("13800138000", "123456");

        verify(userMapper).selectById(7L);
        verify(userMapper).selectOne(any());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void shouldRejectInvalidOrConsumedLoginCode() {
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("login_code:13800138000", "login_code:attempt:13800138000")),
                eq("123456"), eq("5"), eq("300"))).thenReturn(0L);

        assertThrows(ClientException.class,
                () -> service.loginByCode("13800138000", "123456"));
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void shouldApplyAtomicPhoneAndIpRateLimitsBeforeStoringCode() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), eq("60")))
                .thenReturn(1L);

        service.sendCode("13800138000", "127.0.0.1");

        verify(stringRedisTemplate, times(2))
                .execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), eq("60"));
        verify(valueOperations).set(
                eq("login_code:13800138000"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldRejectRateLimitedPhoneBeforeStoringCode() {
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("login_code:send:phone:13800138000")),
                eq("60"))).thenReturn(2L);

        assertThrows(ClientException.class,
                () -> service.sendCode("13800138000", "127.0.0.1"));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldRejectSmsRequestInProductionWithoutProvider() {
        ReflectionTestUtils.setField(service, "activeProfiles", "prod");

        ClientException ex = assertThrows(ClientException.class,
                () -> service.sendCode("13800138000", "127.0.0.1"));

        assertEquals("生产环境短信通道未配置，暂无法发送验证码", ex.getMessage());
        verify(stringRedisTemplate, never()).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldBindLoginCheckToCurrentUser() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("live-start:login:token-1"))
                .thenReturn(JSON.toJSONString(user("13800138000")));
        when(valueOperations.get("live-start:login:token-2"))
                .thenReturn(JSON.toJSONString(user("13900139000")));

        assertTrue(service.checkLogin("token-1"));
        assertFalse(service.checkLogin("token-2"));
    }

    @Test
    void shouldOnlyLogoutCurrentUsersToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("live-start:login:token-1"))
                .thenReturn(JSON.toJSONString(user("13800138000")));

        service.logout("token-1");

        verify(stringRedisTemplate).delete("live-start:login:token-1");
        verify(stringRedisTemplate).delete("live-start:login:user:13800138000");
    }

    @Test
    void shouldRejectLogoutForAnotherUser() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("live-start:login:token-2"))
                .thenReturn(JSON.toJSONString(user("13900139000")));

        assertThrows(ClientException.class, () -> service.logout("token-2"));
        verify(stringRedisTemplate, never()).delete("live-start:login:token-2");
    }

    @Test
    void shouldRejectSimpleUserLookupWithInvalidInternalToken() {
        ReflectionTestUtils.setField(service, "internalToken", "internal-secret");

        assertThrows(ClientException.class,
                () -> service.listSimpleUsersByIds(List.of(7L), "wrong-secret"));
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void shouldAllowSimpleUserLookupWithValidInternalToken() {
        ReflectionTestUtils.setField(service, "internalToken", "internal-secret");
        UserDO storedUser = user("13800138000");
        storedUser.setUsername("tester");
        when(userMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(storedUser));

        var result = service.listSimpleUsersByIds(List.of(7L), "internal-secret");

        assertEquals(1, result.size());
        assertEquals("tester", result.get(0).getUsername());
    }

    @Test
    void shouldRejectConcurrentVenueBindingWhenVenueWasClaimed() {
        UserDO superAdmin = user("13800138000");
        superAdmin.setUserType(4);
        UserDO targetUser = user("13900139000");
        targetUser.setId(8L);
        targetUser.setUserType(1);
        targetUser.setStatus(1);
        VenueDO venue = new VenueDO();
        venue.setId(100L);

        when(userMapper.selectOne(any())).thenReturn(superAdmin, targetUser);
        when(venueMapper.selectById(100L)).thenReturn(venue);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(venueMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(0, 0);

        assertThrows(ClientException.class, () -> service.bindVenueAdmin(8L, 100L));
    }

    private UserDO user(String phone) {
        UserDO user = new UserDO();
        user.setId(7L);
        user.setPhone(phone);
        return user;
    }
}
