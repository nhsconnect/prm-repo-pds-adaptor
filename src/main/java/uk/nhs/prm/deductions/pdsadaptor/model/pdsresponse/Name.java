package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Name {
    private Period period;
    private String use;
    private List<String> given;
    private String family;

}
