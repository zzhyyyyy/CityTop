package com.CityTop.kafka;

import com.CityTop.dto.UserDTO;
import com.CityTop.utils.UserHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "citytop.kafka.operation-log", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class KafkaOperationLogInterceptor implements HandlerInterceptor {
    private static final String START_NANOS_ATTRIBUTE =
            KafkaOperationLogInterceptor.class.getName() + ".startNanos";
    private static final String USER_ID_ATTRIBUTE =
            KafkaOperationLogInterceptor.class.getName() + ".userId";

    private final KafkaOperationLogPublisher publisher;

    public KafkaOperationLogInterceptor(KafkaOperationLogPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            request.setAttribute(START_NANOS_ATTRIBUTE, System.nanoTime());
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        rememberCurrentUser(request);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        rememberCurrentUser(request);
        Object startValue = request.getAttribute(START_NANOS_ATTRIBUTE);
        if (!(startValue instanceof Long)) {
            return;
        }

        String eventId = UUID.randomUUID().toString();
        String traceId = safeTraceId(request.getHeader("X-Request-Id"), eventId);
        Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
        int status = response.getStatus();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(
                Math.max(0L, System.nanoTime() - (Long) startValue));

        publisher.publish(new OperationLogEvent(
                eventId, traceId, "CityTop", userId, request.getMethod(), request.getRequestURI(),
                status, durationMs, System.currentTimeMillis(), ex == null && status < 400,
                ex == null ? null : ex.getClass().getSimpleName()));
    }

    private static void rememberCurrentUser(HttpServletRequest request) {
        if (request.getAttribute(USER_ID_ATTRIBUTE) != null) {
            return;
        }
        UserDTO user = UserHolder.getUser();
        if (user != null) {
            request.setAttribute(USER_ID_ATTRIBUTE, user.getId());
        }
    }

    private static String safeTraceId(String candidate, String fallback) {
        if (candidate == null) {
            return fallback;
        }
        String value = candidate.trim();
        if (value.isEmpty() || value.length() > 64 || !value.matches("[A-Za-z0-9._:-]+")) {
            return fallback;
        }
        return value;
    }
}
