package dev.genesshoan.fitnesstrackerapi.stats.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Identifies the set and session that currently hold one personal-record value. */
@Getter
@Setter
@AllArgsConstructor
public class PersonalRecordHolder {

    private UUID setId;
    private UUID sessionId;
    private Double value;
}
