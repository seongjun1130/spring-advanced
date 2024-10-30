package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.expert.domain.common.dto.AuthUser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j(topic = "ManagerLogAop")
@Aspect
@Component
@RequiredArgsConstructor
public class ManagerLogAop {
  @Pointcut(
      "execution(* org.example.expert.domain.comment.controller.CommentAdminController.*(..))")
  private void deleteComment() {}

  @Pointcut("execution(* org.example.expert.domain.user.controller.UserAdminController.*(..))")
  private void changeUserRole() {}

  @Around("deleteComment() || changeUserRole()")
  public Object execute(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    // 요청시각 측정
    LocalDateTime startTime = LocalDateTime.now();

    // 요청 정보를 가져옴
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes.getRequest();

    Object output = proceedingJoinPoint.proceed();

    // 요청 본문 캡쳐
    ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
    String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
    if (requestBody.isEmpty()) {
      requestBody = proceedingJoinPoint.getArgs()[0].toString();
    }

    // 접근 정보 가져오기
    Long userId = (Long) requestWrapper.getAttribute("userId");
    String url = requestWrapper.getRequestURI();

    // 응답 본문 캡쳐
    ContentCachingResponseWrapper responseWrapper =
        new ContentCachingResponseWrapper(attributes.getResponse());
    try {
      responseWrapper.copyBodyToResponse();
      return output;
    } finally {
      int status = responseWrapper.getStatus();
      log.info(
          "UserID : {} | URL : {} | RequestTime : {} | RequestBody OR PathVariable : {} | StatusCode : {}",
          userId,
          url,
          startTime,
          requestBody,
          status);
    }
  }
}
