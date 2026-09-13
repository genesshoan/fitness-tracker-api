package dev.genesshoan.fitnesstrackerapi.stats.domain;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRecordHolders {

    private PersonalRecordHolder maxWeight;
    private PersonalRecordHolder max1RM;
    private Map<Double, PersonalRecordHolder> repsPerWeight = new HashMap<>();
    private PersonalRecordHolder maxDistance;
    private PersonalRecordHolder maxDuration;
}
