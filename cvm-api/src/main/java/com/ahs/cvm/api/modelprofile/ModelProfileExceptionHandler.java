package com.ahs.cvm.api.modelprofile;

import com.ahs.cvm.application.modelprofile.ModelProfileService.ProfileKeyConflictException;
import com.ahs.cvm.application.modelprofile.ModelProfileService.VierAugenViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {
        ModelProfileController.class, LlmModelProfilesController.class })
public class ModelProfileExceptionHandler {

    @ExceptionHandler(VierAugenViolationException.class)
    public ResponseEntity<Map<String, Object>> vierAugen(
            VierAugenViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "vier_augen_violation",
                        "message", e.getMessage()));
    }

    @ExceptionHandler(ProfileKeyConflictException.class)
    public ResponseEntity<Map<String, Object>> keyKonflikt(
            ProfileKeyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "profile_key_conflict",
                        "message", e.getMessage(),
                        "profileKey", e.getProfileKey()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("nicht gefunden")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "model_profile_not_found",
                            "message", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "model_profile_bad_request",
                        "message", e.getMessage()));
    }
}
