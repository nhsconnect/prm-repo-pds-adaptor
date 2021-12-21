package uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdsPatchIdentifier {
    private String path = "https://fhir.nhs.uk/Id/ods-organization-code";
    private String value;
}
