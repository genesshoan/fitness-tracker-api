package dev.genesshoan.fitnesstrackerapi.stats.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PersonalRecordHolder {

    private UUID setId;
    private UUID sessionId;
    private Double value;
}
