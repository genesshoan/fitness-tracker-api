package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.time.LocalDate;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import net.datafaker.Faker;

public class ProgressRecordBuilder {

    private UUID id;
    private LocalDate recordedAt;
    private Double weightKg;
    private Double bodyFatPercentage;
    private User user;

    public ProgressRecordBuilder(Faker faker) {
        this.id = UUID.randomUUID();
        this.recordedAt = LocalDate.now();
        this.weightKg = faker.number().randomDouble(1, 50, 150);
        this.bodyFatPercentage = faker.number().randomDouble(1, 5, 40);
    }

    public static ProgressRecordBuilder aProgressRecord(Faker faker) {
        return new ProgressRecordBuilder(faker);
    }

    public ProgressRecordBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProgressRecordBuilder withRecordedAt(LocalDate recordedAt) {
        this.recordedAt = recordedAt;
        return this;
    }

    public ProgressRecordBuilder withWeightKg(Double weightKg) {
        this.weightKg = weightKg;
        return this;
    }

    public ProgressRecordBuilder withBodyFatPercentage(Double bodyFatPercentage) {
        this.bodyFatPercentage = bodyFatPercentage;
        return this;
    }

    public ProgressRecordBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public ProgressRecord build() {
        if (user == null) {
            throw new IllegalStateException("User must be set to ProgressRecord");
        }

        return ProgressRecord.builder()
                .id(id)
                .recordedAt(recordedAt)
                .weightKg(weightKg)
                .bodyFatPercentage(bodyFatPercentage)
                .user(user)
                .build();
    }
}
