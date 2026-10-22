/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.common;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> business(BusinessException exception) { return ApiResponse.error(exception.getMessage()); }

    @ExceptionHandler(ImportValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<java.util.List<ImportValidationException.RowError>> importValidation(ImportValidationException exception) {
        return new ApiResponse<>(false, exception.getMessage(), exception.getErrors());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiResponse<Void> credentials() { return ApiResponse.error("用户名或密码错误"); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
            .map(error -> error.getField() + "：" + error.getDefaultMessage()).orElse("请求参数不正确");
        return ApiResponse.error(message);
    }

    @ExceptionHandler({IllegalStateException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiResponse<Void> conflict(RuntimeException exception) { return ApiResponse.error(exception.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiResponse<Void> duplicate() { return ApiResponse.error("数据重复或被其他业务引用，请刷新后重试"); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    ApiResponse<Void> uploadSize() { return ApiResponse.error("上传文件超过大小限制"); }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiResponse<Void> unexpected(Exception exception) {
        log.error("Unhandled request error", exception);
        return ApiResponse.error("服务处理失败，请联系管理员并提供请求时间");
    }
}
