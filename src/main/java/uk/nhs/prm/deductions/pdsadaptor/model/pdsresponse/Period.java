package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Period {
    private LocalDate start;
    private LocalDate end;

    public boolean isCurrent() {
        return getStart().isBefore(LocalDate.now()) && (getEnd() == null || getEnd().isAfter(LocalDate.now()));
    }
}
