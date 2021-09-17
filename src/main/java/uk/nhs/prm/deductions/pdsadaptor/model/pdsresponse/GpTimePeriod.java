package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GpTimePeriod {
    private LocalDate start;
    private LocalDate end;
}
