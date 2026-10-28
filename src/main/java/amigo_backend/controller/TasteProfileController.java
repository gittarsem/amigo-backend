package amigo_backend.controller;

import amigo_backend.dto.tasteprofile.SubmitTasteAnswersRequestDTO;
import amigo_backend.dto.tasteprofile.TasteProfileResponseDTO;
import amigo_backend.dto.tasteprofile.TasteQuestionDTO;
import amigo_backend.service.TasteProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/taste-profile")
@RequiredArgsConstructor
public class TasteProfileController {

    private final TasteProfileService tasteProfileService;

    @GetMapping
    public ResponseEntity<TasteProfileResponseDTO> getTasteProfile(
            org.springframework.security.core.Authentication authentication
    ) {

        return ResponseEntity.ok(
                tasteProfileService.getTasteProfile(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/questions")
    public ResponseEntity<List<TasteQuestionDTO>> getInitialQuestions(
            org.springframework.security.core.Authentication authentication
    ) {

        return ResponseEntity.ok(
                tasteProfileService.getInitialQuestions(
                        authentication.getName()
                )
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<TasteProfileResponseDTO> generateTasteProfile(
            org.springframework.security.core.Authentication authentication,
            @Valid @RequestBody SubmitTasteAnswersRequestDTO request
    ) {

        return ResponseEntity.ok(
                tasteProfileService.generateTasteProfile(
                        authentication.getName(),
                        request
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TasteProfileErrorResponse> handleBadRequest(
            IllegalArgumentException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TasteProfileErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {

        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TasteProfileErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<TasteProfileErrorResponse> handleDatabaseConflict(
            DataIntegrityViolationException exception
    ) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Taste profile could not be saved because of a database constraint"
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<TasteProfileErrorResponse> handleIllegalState(
            IllegalStateException exception
    ) {

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TasteProfileErrorResponse> handleUnexpectedError(
            Exception exception
    ) {

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the taste profile"
        );
    }

    private ResponseEntity<TasteProfileErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message
    ) {

        TasteProfileErrorResponse response =
                new TasteProfileErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message == null || message.isBlank()
                                ? status.getReasonPhrase()
                                : message
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    public record TasteProfileErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message
    ) {
    }
}