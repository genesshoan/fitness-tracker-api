package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import java.util.UUID;
import net.datafaker.Faker;

public class MuscleBuilder {

    private String name;
    private String slug;
    private BodyRegion bodyRegion = BodyRegion.ARMS;

    public MuscleBuilder(Faker faker) {
        this.name = faker.funnyName().name() + UUID.randomUUID().toString();
        this.slug = faker.internet().slug() + UUID.randomUUID().toString();
    }

    public static MuscleBuilder aMuscle(Faker faker) {
        return new MuscleBuilder(faker);
    }

    public MuscleBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public MuscleBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public MuscleBuilder withBodyRegion(BodyRegion bodyRegion) {
        this.bodyRegion = bodyRegion;
        return this;
    }

    public Muscle build() {
        return Muscle.builder()
            .name(name)
            .slug(slug)
            .bodyRegion(bodyRegion)
            .build();
    }
}
