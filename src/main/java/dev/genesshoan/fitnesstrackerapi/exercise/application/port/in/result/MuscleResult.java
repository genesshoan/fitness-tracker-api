package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import java.util.Set;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.BodyRegion;

/**
 * A muscle of the catalog and the assets that illustrate it.
 *
 * @param name       the muscle name
 * @param slug       the URL-friendly muscle identifier
 * @param bodyRegion the body region the muscle belongs to
 * @param assets     the images of the muscle
 */
public record MuscleResult(String name, String slug, BodyRegion bodyRegion, Set<MuscleAssetResult> assets) {}
