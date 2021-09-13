package uk.nhs.prm.deductions.pdsadaptor.client.auth.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(value= HttpStatus.INTERNAL_SERVER_ERROR)
public class SignedJwtException extends RuntimeException {
    public SignedJwtException(Exception e) {
        super(String.format("Unable to sign JWT: reason %s, cause %s", e.getMessage(), e.getCause()));
        log.info("Unable to sign JWT: reason {}, cause {}", e.getMessage(), e.getCause());
    }

}
