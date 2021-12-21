package uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdsPatchRequest {
    private String op = "replace";
    private String path = "/managingOrganization";
    private PdsPatchValue value;
}
