package com.mongxin.livestart.distribution.service;

import com.mongxin.livestart.distribution.common.biz.user.UserContext;
import com.mongxin.livestart.distribution.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dao.mapper.UserTicketMapper;
import com.mongxin.livestart.distribution.dto.req.TicketGrabReqDTO;
import com.mongxin.livestart.distribution.service.impl.UserTicketServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTicketServiceImplTest {

    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private UserTicketMapper userTicketMapper;

    @Spy
    @InjectMocks
    private UserTicketServiceImpl userTicketService;

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldRollbackRedisWhenDatabaseDecrementFails() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("1001")
                .username("tester")
                .phone("13800000000")
                .build());

        TicketSkuDO sku = TicketSkuDO.builder()
                .id(11L)
                .eventId(22L)
                .remainingStock(10)
                .limitNum(2)
                .build();

        TicketGrabReqDTO request = new TicketGrabReqDTO();
        request.setTicketSkuId(11L);
        request.setArtistPromoCode("ART-001");

        List<String> redisKeys = List.of(
                "livestart:distribution:ticket:stock:11",
                "livestart:distribution:ticket:limit:11:user:1001"
        );

        when(ticketSkuMapper.selectById(11L)).thenReturn(sku);
        when(stringRedisTemplate.execute(any(RedisScript.class), eq(redisKeys),
                eq("1"), eq("2"), eq(String.valueOf(7 * 24 * 3600L))))
                .thenReturn(1L);
        when(ticketSkuMapper.update(eq(null), any())).thenReturn(0);
        when(stringRedisTemplate.execute(any(RedisScript.class), eq(redisKeys), eq("1"), eq("1")))
                .thenReturn(0L);

        ServiceException ex = assertThrows(ServiceException.class, () -> userTicketService.grabTicket(request));

        assertEquals("秒杀库存不足，冲突退回，请重试", ex.getMessage());
        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(redisKeys), eq("1"), eq("1"));
    }

    @Test
    void shouldResolveTicketStatusDescCorrectly() throws Exception {
        java.lang.reflect.Method method = UserTicketServiceImpl.class.getDeclaredMethod("resolveTicketStatusDesc", Integer.class);
        method.setAccessible(true);

        assertEquals("未使用", method.invoke(userTicketService, 0));
        assertEquals("已使用", method.invoke(userTicketService, 1));
        assertEquals("已退票", method.invoke(userTicketService, 2));
        assertEquals("未知状态", method.invoke(userTicketService, (Integer) null));
        assertEquals("未知状态", method.invoke(userTicketService, 99));
    }
}
