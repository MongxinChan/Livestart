package com.mongxin.livestart.distribution.controller;

import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.framework.exception.ClientException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventControllerTest {

    @Test
    void rejectsPublishWithoutInternalToken() {
        EventService eventService = mock(EventService.class);
        EventController controller = new EventController(eventService);
        ReflectionTestUtils.setField(controller, "internalToken", "test-internal-token");

        assertThrows(ClientException.class, () -> controller.publishEvent(new EventPublishReqDTO(), null));
        assertThrows(ClientException.class, () -> controller.publishEvent(new EventPublishReqDTO(), "wrong-token"));
        verifyNoInteractions(eventService);
    }

    @Test
    void publishesWithInternalToken() {
        EventService eventService = mock(EventService.class);
        EventController controller = new EventController(eventService);
        ReflectionTestUtils.setField(controller, "internalToken", "test-internal-token");
        EventPublishReqDTO request = new EventPublishReqDTO();

        assertTrue(controller.publishEvent(request, "test-internal-token").isSuccess());
        verify(eventService).publishEvent(request);
    }
}
