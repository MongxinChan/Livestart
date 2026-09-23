package com.mongxin.livestart.admin.service;

import com.mongxin.livestart.admin.common.biz.user.UserContext;
import com.mongxin.livestart.admin.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.admin.common.convention.exception.ClientException;
import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dao.mapper.UserMapper;
import com.mongxin.livestart.admin.dao.mapper.UserVisitorMapper;
import com.mongxin.livestart.admin.service.impl.VisitorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorServiceImplTest {

    @Mock
    private UserVisitorMapper userVisitorMapper;
    @Mock
    private UserMapper userMapper;

    private VisitorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VisitorServiceImpl(userMapper);
        ReflectionTestUtils.setField(service, "baseMapper", userVisitorMapper);
        UserContext.setUser(UserInfoDTO.builder().userId("7").build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldRejectNonAdminQueryingAnotherUsersVisitors() {
        when(userMapper.selectOne(any())).thenReturn(user(1));

        assertThrows(ClientException.class, () -> service.listVisitorsByUserId(8L));
        verify(userVisitorMapper, never()).selectList(any());
    }

    @Test
    void shouldAllowSuperAdminQueryingAnotherUsersVisitors() {
        when(userMapper.selectOne(any())).thenReturn(user(4));
        when(userVisitorMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(service.listVisitorsByUserId(8L).isEmpty());
        verify(userVisitorMapper).selectList(any());
    }

    private UserDO user(int userType) {
        UserDO user = new UserDO();
        user.setId(7L);
        user.setUserType(userType);
        user.setDelFlag(0);
        return user;
    }
}
