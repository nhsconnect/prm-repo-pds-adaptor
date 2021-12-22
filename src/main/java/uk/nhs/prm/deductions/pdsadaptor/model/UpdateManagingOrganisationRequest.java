package uk.nhs.prm.deductions.pdsadaptor.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateManagingOrganisationRequest {
    @NotNull
    @NotBlank
    @Parameter(required = true)
    private String previousGp;

    @NotNull
    @NotBlank
    @Parameter(required = true)
    private String recordETag;
}
