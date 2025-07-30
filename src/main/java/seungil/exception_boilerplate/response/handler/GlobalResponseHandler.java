package seungil.exception_boilerplate.response.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import seungil.exception_boilerplate.response.CustomException;
import seungil.exception_boilerplate.response.dto.ApiResponse;
import seungil.exception_boilerplate.response.dto.ErrorCode;
import seungil.exception_boilerplate.response.dto.SuccessCode;

@Slf4j
@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // ResponseEntity로 직접 Response를 구성한 경우는 제외
        return !returnType.getParameterType().equals(ResponseEntity.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 이미 ApiResponse로 감싼 응답은 그대로 리턴
        if (body instanceof ApiResponse) {
            return body;
        }

        // HTTP 메서드에 따른 상태코드 결정
        String method = request.getMethod().name();
        HttpStatus status;
        switch (method) {
            case "POST" -> status = HttpStatus.CREATED;     // 201
            case "DELETE" -> status = HttpStatus.NO_CONTENT; // 204
            case "PUT", "PATCH", "GET" -> status = HttpStatus.OK; // 200
            default -> status = HttpStatus.OK;
        }

        // 상태 코드 설정
        response.setStatusCode(status);

        // 204 NO_CONTENT는 body가 없어야 하므로 null 반환
        if (status == HttpStatus.NO_CONTENT) {
            return null;
        }

        // ApiResponse.success()로 감싸기
        return ApiResponse.success(SuccessCode.SUCCESS_200_001, body);
    }

    /**
     * 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[CustomException] code={}, message={}", errorCode.getCode(), ex.getCustomMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, ex.getCustomMessage()));
    }

    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        ErrorCode errorCode = ErrorCode.ETC_520_001;
        log.error("[UnhandledException] message={}", ex.getMessage(), ex);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }
}
