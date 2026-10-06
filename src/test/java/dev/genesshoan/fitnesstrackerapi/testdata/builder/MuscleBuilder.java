package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;
import net.datafaker.Faker;

public class MuscleBuilder {

    private String name;
    private String slug;
    private BodyRegion bodyRegion = BodyRegion.ARMS;
    private Set<MuscleAsset> assets = new HashSet<>();

    public MuscleBuilder(Faker faker) {
        this.name = faker.funnyName().name() + UUID.randomUUID();
        this.slug = faker.internet().slug() + UUID.randomUUID();
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

    public MuscleBuilder withAssets(Set<MuscleAsset> assets) {
        this.assets = assets;
        return this;
    }

    public Muscle build() {
        return Muscle.builder()
                .name(name)
                .slug(slug)
                .bodyRegion(bodyRegion)
                .assets(assets)
                .build();
    }
}
