package com.CityTop.kafka;

import com.CityTop.dto.UserDTO;
import com.CityTop.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KafkaOperationLogInterceptorTest {

    @AfterEach
    void cleanUserHolder() {
        UserHolder.removeUser();
    }

    @Test
    void shouldPublishSanitizedHttpOperationLog() throws Exception {
        KafkaOperationLogPublisher publisher = mock(KafkaOperationLogPublisher.class);
        KafkaOperationLogInterceptor interceptor = new KafkaOperationLogInterceptor(publisher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shop/1");
        request.addHeader("X-Request-Id", "trace-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        HandlerMethod handler = new HandlerMethod(this, getClass().getDeclaredMethod("sampleHandler"));

        interceptor.preHandle(request, response, handler);
        UserDTO user = new UserDTO();
        user.setId(7L);
        UserHolder.saveUser(user);
        interceptor.postHandle(request, response, handler, null);
        UserHolder.removeUser();
        interceptor.afterCompletion(request, response, handler, null);

        ArgumentCaptor<OperationLogEvent> eventCaptor =
                ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        OperationLogEvent event = eventCaptor.getValue();
        assertEquals("trace-001", event.getTraceId());
        assertEquals(Long.valueOf(7L), event.getUserId());
        assertEquals("GET", event.getMethod());
        assertEquals("/shop/1", event.getPath());
        assertEquals(200, event.getStatus());
        assertTrue(event.isSuccessful());
    }

    @Test
    void shouldIgnoreNonControllerHandler() {
        KafkaOperationLogPublisher publisher = mock(KafkaOperationLogPublisher.class);
        KafkaOperationLogInterceptor interceptor = new KafkaOperationLogInterceptor(publisher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/asset.js");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Object staticResourceHandler = new Object();

        interceptor.preHandle(request, response, staticResourceHandler);
        interceptor.afterCompletion(request, response, staticResourceHandler, null);

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    public void sampleHandler() {
        // 仅供 HandlerMethod 单元测试使用。
    }
}
