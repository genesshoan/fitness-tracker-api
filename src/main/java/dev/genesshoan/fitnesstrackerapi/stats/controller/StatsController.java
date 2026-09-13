package dev.genesshoan.fitnesstrackerapi.stats.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPointsDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMaxDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolumeDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.StreakDTO;
import dev.genesshoan.fitnesstrackerapi.stats.service.StatsService;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/volume")
    public ResponseEntity<SessionVolumeDTO> getSessionVolume(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam UUID sessionId) {
        return ResponseEntity.ok(statsService.calculateSessionVolume(sessionId, principal.getId()));
    }

    @GetMapping("/1rm")
    public ResponseEntity<OneRepMaxDTO> getOneRepMax(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam UUID exerciseId) {
        return ResponseEntity.ok(statsService.getOneRepMax(principal.getId(), exerciseId));
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakDTO> getCurrentStreak(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(statsService.getCurrentStreak(
                principal.getId(), Instant.now(), principal.getUser().getTimezone()));
    }

    @GetMapping("/progress")
    public ResponseEntity<ExerciseProgressPointsDTO> getExerciseProgress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam UUID exerciseId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(statsService.getExerciseProgress(
                principal.getId(), exerciseId, from, to, principal.getUser().getTimezone()));
    }
}
