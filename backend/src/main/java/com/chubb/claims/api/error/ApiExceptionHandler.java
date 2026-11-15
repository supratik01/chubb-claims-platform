package com.chubb.claims.api.error;

import com.chubb.claims.domain.exception.BusinessRuleViolationException;
import com.chubb.claims.domain.exception.DomainException;
import com.chubb.claims.domain.exception.InvalidCredentialsException;
import com.chubb.claims.domain.exception.InvalidStatusTransitionException;
import com.chubb.claims.domain.exception.OperationNotPermittedException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single place where failures become HTTP responses. Every response is an RFC 9457 problem document
 * with a "code" and, for validation failures, an "errors" list.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomainException(DomainException exception) {
        return switch (exception) {
            case ResourceNotFoundException e -> problem(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, e.getMessage());
            case InvalidStatusTransitionException e ->
                    problem(HttpStatus.CONFLICT, ErrorCode.INVALID_STATUS_TRANSITION, e.getMessage());
            case BusinessRuleViolationException e ->
                    problem(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.BUSINESS_RULE_VIOLATED, e.getMessage());
            case OperationNotPermittedException e ->
                    problem(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, e.getMessage());
            case InvalidCredentialsException e ->
                    problem(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, e.getMessage());
            default -> unexpected(exception);
        };
    }

    /** Two people changed the same claim; the second one loses and should reload. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleConcurrentModification(OptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION,
                "The claim was changed by someone else. Reload it and try again.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        return unexpected(exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        return validationProblem(exception, errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<Map<String, String>> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .map(message -> Map.of(
                                "field", String.valueOf(result.getMethodParameter().getParameterName()),
                                "message", String.valueOf(message))))
                .toList();
        return validationProblem(exception, errors, headers, request);
    }

    /** Errors Spring MVC detects itself (unreadable JSON, wrong parameter type, unknown path) get a code too. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(exception, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            problem.setProperty("code", codeFor(status).name());
        }
        return response;
    }

    private ResponseEntity<Object> validationProblem(Exception exception, List<Map<String, String>> errors,
                                                     HttpHeaders headers, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");
        problem.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
        problem.setProperty("errors", errors);
        return super.handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.NOT_FOUND;
        }
        return status.is4xxClientError() ? ErrorCode.VALIDATION_FAILED : ErrorCode.INTERNAL_ERROR;
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        return ResponseEntity.status(status).body(problem);
    }

    /** Unexpected failures are logged in full and reported without internal details. */
    private static ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        log.error("Unexpected error while handling a request", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Something went wrong on our side. Please try again.");
    }
}
