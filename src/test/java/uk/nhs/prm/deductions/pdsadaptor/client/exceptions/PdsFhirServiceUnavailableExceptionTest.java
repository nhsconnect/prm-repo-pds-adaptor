package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PdsFhirServiceUnavailableExceptionTest {

    @Test
    public void shouldPropagateA503BackToCallerSinceLooksLikeServiceUnavilableAtTheMoment() {
        var responseStatusAnnotation = PdsFhirServiceUnavailableException.class.getAnnotationsByType(ResponseStatus.class)[0];

        assertThat(responseStatusAnnotation.value()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}