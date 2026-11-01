package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import lombok.RequiredArgsConstructor;

/**
 * Loads the state a user already has for a set of achievements.
 *
 * <p>The state is loaded in one query so that an evaluation does not need a
 * round trip per achievement.</p>
 */
@Component
@RequiredArgsConstructor
public class AchievementStateLoader {

    private final UserAchievementRepository userAchievementRepository;

    /**
     * Loads the state of the user for the specified achievements.
     *
     * @param userId the user the achievements belong to
     * @param definitions the achievements to look for
     * @return the state of the user, indexed by achievement and scope
     */
    public Map<AchievementKey, UserAchievement> load(UUID userId, Collection<AchievementDefinition> definitions) {

        Map<AchievementKey, UserAchievement> states = new HashMap<>();

        if (definitions.isEmpty()) {
            return states;
        }

        List<UUID> definitionIds =
                definitions.stream().map(AchievementDefinition::getId).toList();

        for (UserAchievement state : userAchievementRepository.findByUserIdAndDefinitionIdIn(userId, definitionIds)) {
            states.putIfAbsent(new AchievementKey(state.getDefinition().getId(), state.getExerciseId()), state);
        }
        return states;
    }
}
