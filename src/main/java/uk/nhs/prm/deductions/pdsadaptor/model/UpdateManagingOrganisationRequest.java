package uk.nhs.prm.deductions.pdsadaptor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateManagingOrganisationRequest {
    private String previousGp;
    private String recordETag;
}
