package uk.nhs.prm.deductions.pdsadaptor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateManagingOrganisationRequest {
    private String previousGp;
    private String recordETag;
}
