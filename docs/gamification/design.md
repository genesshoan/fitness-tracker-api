# Diseño de gamificación: logros persistidos

> Estado: **propuesta**. No hay cambios de código en este documento.
> Decisiones de producto tomadas: solo logros (sin XP ni ranking), reglas híbridas
> (umbrales en base de datos, récords personales en código), notificación solo
> in-app (feed) y sin backfill retroactive.

---

## 1. Resumen ejecutivo

Hoy los logros se **calculan en el vuelo** en cada lectura o escritura de sesión
([`StatsService.calculateForSession`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/stats/service/StatsService.java#L326)
y `calculateForSet`). Eso significa que el estado de gamificación **no existe**: no
hay historial de desbloqueos, no hay progreso hacia el siguiente logro y no hay
forma de saber qué es nuevo para el usuario.

La propuesta sustituye ese modelo efímero por uno de tres capas persistidas:

| Capa | Tabla | Responde a |
| --- | --- | --- |
| **Definición** | `achievement_definitions` | ¿Qué logros existen y con qué regla se desbloquean? |
| **Progreso / desbloqueo** | `user_achievements` | ¿Qué ha desbloqueado el usuario y cuánto le falta? |
| **Récord personal** | `personal_records` | ¿Cuál es la mejor marca del usuario por ejercicio y métrica? |

El resultado es que el camino de lectura pasa a ser **estático**: el frontend pide
el catálogo con el estado del usuario y recibe filas ya calculadas, sin agregaciones
sobre el historial de entrenamientos.

De paso, esta separación **rompe el ciclo de dependencia actual** entre `stats` y
`workout` (hoy `stats` importa repositorios y dominio de `workout`, y `workout`
importa `StatsService`), lo que contradice el ADR-0009.

---

## 2. Estado actual y problemas concretos

### 2.1 El cálculo en el vuelo

`WorkoutSessionService` llama al cálculo en **cada** respuesta de sesión y de set:

- [`WorkoutSessionService.toWorkoutSessionResponseDTO`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/workout/WorkoutSessionService.java#L569)
- [`WorkoutSessionService.toSessionSetResponseDTO`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/workout/WorkoutSessionService.java#L574)

Detrás hay una consulta pesada:
[`StatsRepository.findRankedSets`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/stats/repository/StatsRepository.java#L51)
ejecuta **cinco funciones de ventana** (`ROW_NUMBER`) sobre *todo* el historial de
sets del usuario para los ejercicios tocados, y se recalcula en cada lectura. Un
`GET /workout-sessions/{id}` de una sesión vieja paga el coste de su propio historial
completo.

### 2.2 Los logros no son un estado, son un cálculo

`AchievementCalculator` es una clase estática con `if`s explícitos por métrica
([`AchievementCalculator.java`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/stats/calculator/AchievementCalculator.java)),
y `AchievementType` es un enum cerrado de 5 valores
([`AchievementType.java`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/stats/domain/AchievementType.java)).
Añadir "10 entrenamientos completados" o "racha de 3 días" exige tocar Java,
escribir tests y hacer un release.

### 2.3 Lo que hoy es imposible

Con el modelo actual no se puede construir, sin cambios de fondo:

- un **feed** de logros desbloqueados ("¡nuevo!" con badge no visto);
- **progreso** hacia el siguiente logro ("te faltan 3 entrenamientos");
- una **colección** por usuario (insignias por ejercicio);
- cualquier noción de **puntos o nivel**, por trivial que sea.

### 2.4 Semánticas que conviene corregir de paso

- `findRankedSets` no filtra por `ws.status = 'COMPLETED'`: un set de una sesión
  `IN_PROGRESS` ya puede establecer un récord personal.
- `getTrainedDatesBeforeAsc` (usada para las rachas) tampoco filtra por estado, de
  modo que una sesión sin terminar cuenta como día de entrenamiento.
- `updateSessionSet` y `markSetIncomplete` permiten cambiar métricas de un set ya
  completado mientras la sesión sigue `IN_PROGRESS`. Con estado persistido, eso
  invalida récords y exige recomputarlos (ver §7.4).

---

## 3. Decisiones de diseño

| # | Decisión | Motivo |
| --- | --- | --- |
| D1 | Los desbloqueos **se persisten** en `user_achievements` | Habilita feed, progreso y colección sin recalcular |
| D2 | Módulo nuevo `gamification`, no ampliar `stats` | `stats` es lectura/agregación; la gamificación tiene ciclo de vida propio (definición → progreso → desbloqueo → visto) |
| D3 | Reglas **híbridas**: umbrales como filas, récords en código | Un umbral (`TOTAL_WORKOUTS >= 10`) es un dato; comparar contra un récord necesita semántica que no cabe en un `threshold` |
| D4 | El progreso se calcula **en el evento, no en la lectura** | El coste se paga al completar un entrenamiento, no en cada `GET` |
| D5 | Sin contadores materializados: los agregados se leen de la fuente al evaluar | Evita un contador que se desincroniza con borrados y evita el problema del backfill (§11.3) |
| D6 | El estado de gamificación es **monótono**: un logro no se revoca | Revocar insignias es intratable; el logro ya ocurrió |
| D7 | Notificación **solo in-app** mediante el flag `seen` | No hay email de desbloqueo; el frontend consulta el feed |
| D8 | Sin backfill: no se otorgan logros retroactivos | Los desbloqueos ocurren solo en el momento de la evaluación |
| D9 | Sin XP, niveles ni ranking en esta versión | Fuera de alcance (§16) |

---

## 4. Modelo de dominio

### 4.1 Tres capas

```
achievement_definitions        user_achievements              personal_records
─────────────────────────       ──────────────────────         ─────────────────
qué logros existen    ───────►  estado por usuario             mejor marca por
y con qué regla                 (progreso, desbloqueo,         (usuario, ejercicio,
                                visto)                         métrica)
                                       ▲
                                       │ se evalúa al completar
                                       │ sesión o set
```

### 4.2 Tipos de regla

**`rule_type = THRESHOLD`** — regla declarada en base de datos. No requiere código
para añadir un logro nuevo: se inserta una fila y el evaluador la procesa.

```
metric    ∈ TOTAL_WORKOUTS | LONGEST_STREAK | TOTAL_SETS | TOTAL_REPS
          | TOTAL_VOLUME_KG | TOTAL_TRAINING_SECONDS | TOTAL_DISTANCE_KM
          | DISTINCT_EXERCISES
operator  ∈ GTE | GT
threshold = DOUBLE PRECISION
scope     = GLOBAL
```

Ejemplo: `code = WORKOUTS_10`, `metric = TOTAL_WORKOUTS`, `operator = GTE`,
`threshold = 10`.

**`rule_type = PERSONAL_RECORD`** — regla evaluada por código, porque "superar un
récord" necesita comparar contra el estado previo y porque la granularidad depende
de la métrica.

```
metric    ∈ MAX_WEIGHT | MAX_ESTIMATED_1RM | MAX_REPS_AT_WEIGHT
          | MAX_DISTANCE | MAX_DURATION
scope     = GLOBAL | PER_EXERCISE
```

`scope` define cuántas insignias produce la definición:

- `GLOBAL` → una sola insignia por usuario (`PR_FIRST_MAX_WEIGHT`: tu primer récord
  de peso, en cualquier ejercicio).
- `PER_EXERCISE` → una insignia por ejercicio (`PR_MAX_WEIGHT_PER_EXERCISE`: un
  récord de peso en cada ejercicio que entrenas).

### 4.3 Enumeraciones Java

| Enum | Valores |
| --- | --- |
| `AchievementCategory` | `MILESTONE`, `CONSISTENCY`, `VOLUME`, `EXPLORATION`, `PERSONAL_RECORD` |
| `AchievementTier` | `BRONZE`, `SILVER`, `GOLD` (presentacional, sin puntos) |
| `RuleType` | `THRESHOLD`, `PERSONAL_RECORD` |
| `AchievementMetric` | los 8 umbrales + los 5 récords |
| `ComparisonOperator` | `GTE`, `GT` |
| `AchievementScope` | `GLOBAL`, `PER_EXERCISE` |

---

## 5. Esquema de base de datos

Cuatro migraciones nuevas. La migración `V18` (alineación de los timestamps de
`muscle_assets` con el mapeo de entidades, ver §15.3) ya está aplicada, así que
estas empiezan en `V19`.

> **Flyway es la fuente de verdad del esquema.** El perfil de test corre con
> `spring.jpa.hibernate.ddl-auto: validate`
> ([`application-test.yml`](../../src/main/resources/application-test.yml)), así que
> el esquema lo construyen las migraciones y Hibernate valida el mapeo de las
> entidades contra él. Las constraints, los índices y las claves foráneas viven en
> las migraciones, y se puede usar todo lo que ofrece PostgreSQL 18, incluido
> `NULLS NOT DISTINCT`. Ver §15.3.

### 5.1 `V19__create_achievement_definitions.sql`

```sql
CREATE TABLE achievement_definitions
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    code        VARCHAR(64)              NOT NULL,
    name        VARCHAR(128)             NOT NULL,
    description TEXT,
    category    VARCHAR(32)              NOT NULL,
    tier        VARCHAR(16)              NOT NULL DEFAULT 'BRONZE',
    rule_type   VARCHAR(24)              NOT NULL,
    metric      VARCHAR(48)              NOT NULL,
    operator    VARCHAR(8)               NOT NULL DEFAULT 'GTE',
    threshold   DOUBLE PRECISION,
    scope       VARCHAR(16)              NOT NULL DEFAULT 'GLOBAL',
    icon_key    VARCHAR(128),
    sort_order  INT                      NOT NULL DEFAULT 0,
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_achievement_definitions PRIMARY KEY (id),
    CONSTRAINT uk_achievement_definitions_code UNIQUE (code),

    CONSTRAINT ck_achievement_definitions_category
        CHECK (category IN ('MILESTONE', 'CONSISTENCY', 'VOLUME', 'EXPLORATION', 'PERSONAL_RECORD')),
    CONSTRAINT ck_achievement_definitions_tier
        CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD')),
    CONSTRAINT ck_achievement_definitions_rule_type
        CHECK (rule_type IN ('THRESHOLD', 'PERSONAL_RECORD')),
    CONSTRAINT ck_achievement_definitions_scope
        CHECK (scope IN ('GLOBAL', 'PER_EXERCISE')),
    CONSTRAINT ck_achievement_definitions_operator
        CHECK (operator IN ('GTE', 'GT')),

    -- Un umbral necesita threshold; un récord personal no lo usa
    CONSTRAINT ck_achievement_definitions_threshold
        CHECK ((rule_type = 'THRESHOLD' AND threshold IS NOT NULL)
            OR (rule_type = 'PERSONAL_RECORD' AND threshold IS NULL)),

    -- La métrica debe ser coherente con el tipo de regla
    CONSTRAINT ck_achievement_definitions_metric
        CHECK ((rule_type = 'THRESHOLD' AND metric IN (
                    'TOTAL_WORKOUTS', 'LONGEST_STREAK', 'TOTAL_SETS', 'TOTAL_REPS',
                    'TOTAL_VOLUME_KG', 'TOTAL_TRAINING_SECONDS', 'TOTAL_DISTANCE_KM',
                    'DISTINCT_EXERCISES'))
            OR (rule_type = 'PERSONAL_RECORD' AND metric IN (
                    'MAX_WEIGHT', 'MAX_ESTIMATED_1RM', 'MAX_REPS_AT_WEIGHT',
                    'MAX_DISTANCE', 'MAX_DURATION'))),

    -- Solo los récords personales pueden acotarse a un ejercicio
    CONSTRAINT ck_achievement_definitions_scope_rule
        CHECK ((rule_type = 'THRESHOLD' AND scope = 'GLOBAL')
            OR (rule_type = 'PERSONAL_RECORD'))
);

CREATE INDEX idx_achievement_definitions_active
    ON achievement_definitions (active, category, sort_order);
```

### 5.2 `V20__create_personal_records.sql`

Los récords viven en **una sola tabla**. La métrica determina la granularidad: las
métricas absolutas (peso, 1RM, distancia, duración) dan una fila por ejercicio, y
`MAX_REPS_AT_WEIGHT` da una fila por peso usado. Un índice único con
`NULLS NOT DISTINCT` cubre los dos casos, porque trata el peso nulo como un valor
igual que cualquier otro.

```sql
CREATE TABLE personal_records
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    user_id     UUID                     NOT NULL,
    exercise_id UUID                     NOT NULL,
    metric      VARCHAR(32)              NOT NULL,
    weight_kg   DOUBLE PRECISION,
    value       DOUBLE PRECISION         NOT NULL,
    set_id      UUID,
    session_id  UUID,
    achieved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_personal_records PRIMARY KEY (id),
    CONSTRAINT fk_personal_records_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_personal_records_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_personal_records_set FOREIGN KEY (set_id) REFERENCES session_sets (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_personal_records_session FOREIGN KEY (session_id) REFERENCES workout_sessions (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_personal_records_metric
        CHECK (metric IN ('MAX_WEIGHT', 'MAX_ESTIMATED_1RM', 'MAX_REPS_AT_WEIGHT',
            'MAX_DISTANCE', 'MAX_DURATION')),
    CONSTRAINT ck_personal_records_value CHECK (value > 0),

    -- El récord de reps es por peso; el resto de métricas no usa peso
    CONSTRAINT ck_personal_records_weight CHECK (
        (metric = 'MAX_REPS_AT_WEIGHT' AND weight_kg IS NOT NULL AND weight_kg > 0)
            OR (metric <> 'MAX_REPS_AT_WEIGHT' AND weight_kg IS NULL))
);

-- Una fila por métrica y ejercicio; el récord de reps, una por peso.
-- NULLS NOT DISTINCT hace que el peso nulo de las métricas sin peso también sea único.
CREATE UNIQUE INDEX uk_personal_records_identity
    ON personal_records (user_id, exercise_id, metric, weight_kg) NULLS NOT DISTINCT;

CREATE INDEX idx_personal_records_user_exercise ON personal_records (user_id, exercise_id);
```

> En `MAX_REPS_AT_WEIGHT`, `value` contiene el número de repeticiones y
> `weight_kg` el peso con el que se maja. En las demás métricas, `value` contiene
> el valor de la métrica (kg, km, segundos o el 1RM estimado).

### 5.3 `V21__create_user_achievements.sql`

```sql
CREATE TABLE user_achievements
(
    id                        UUID                     NOT NULL DEFAULT uuidv7(),
    user_id                   UUID                     NOT NULL,
    achievement_definition_id UUID                     NOT NULL,
    exercise_id               UUID,
    progress_value            DOUBLE PRECISION,
    unlocked                  BOOLEAN                  NOT NULL DEFAULT FALSE,
    unlocked_at               TIMESTAMP WITH TIME ZONE,
    source_session_id         UUID,
    source_set_id             UUID,
    seen                      BOOLEAN                  NOT NULL DEFAULT FALSE,
    seen_at                   TIMESTAMP WITH TIME ZONE,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_achievements PRIMARY KEY (id),
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_definition
        FOREIGN KEY (achievement_definition_id) REFERENCES achievement_definitions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_session FOREIGN KEY (source_session_id) REFERENCES workout_sessions (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_user_achievements_set FOREIGN KEY (source_set_id) REFERENCES session_sets (id)
        ON DELETE SET NULL,

    -- Desbloqueado implica fecha de desbloqueo y viceversa
    CONSTRAINT ck_user_achievements_unlocked
        CHECK ((unlocked = FALSE AND unlocked_at IS NULL)
            OR (unlocked = TRUE AND unlocked_at IS NOT NULL))
);

-- Un usuario tiene como máximo una insignia por definición y ámbito.
-- exercise_id es nulo en el ámbito GLOBAL y referencia al ejercicio en PER_EXERCISE.
-- NULLS NOT DISTINCT hace que el nulo también sea único, de modo que no hace falta
-- una columna auxiliar para el ámbito.
CREATE UNIQUE INDEX uk_user_achievements_identity
    ON user_achievements (user_id, achievement_definition_id, exercise_id) NULLS NOT DISTINCT;

CREATE INDEX idx_user_achievements_feed
    ON user_achievements (user_id, unlocked_at DESC);
CREATE INDEX idx_user_achievements_unseen
    ON user_achievements (user_id, seen);
```

### 5.4 `V22__seed_achievement_definitions.sql`

Catálogo inicial. Los umbrales se añaden sin código; los récords personales son las
cinco plantillas por métrica y alcance.

| code | category | rule | metric | umbral | scope |
| --- | --- | --- | --- | --- | --- |
| `FIRST_WORKOUT` | MILESTONE | THRESHOLD | TOTAL_WORKOUTS | 1 | GLOBAL |
| `WORKOUTS_10` | MILESTONE | THRESHOLD | TOTAL_WORKOUTS | 10 | GLOBAL |
| `WORKOUTS_50` | MILESTONE | THRESHOLD | TOTAL_WORKOUTS | 50 | GLOBAL |
| `WORKOUTS_100` | MILESTONE | THRESHOLD | TOTAL_WORKOUTS | 100 | GLOBAL |
| `WORKOUTS_365` | MILESTONE | THRESHOLD | TOTAL_WORKOUTS | 365 | GLOBAL |
| `STREAK_3` | CONSISTENCY | THRESHOLD | LONGEST_STREAK | 3 | GLOBAL |
| `STREAK_7` | CONSISTENCY | THRESHOLD | LONGEST_STREAK | 7 | GLOBAL |
| `STREAK_30` | CONSISTENCY | THRESHOLD | LONGEST_STREAK | 30 | GLOBAL |
| `STREAK_100` | CONSISTENCY | THRESHOLD | LONGEST_STREAK | 100 | GLOBAL |
| `SETS_100` | VOLUME | THRESHOLD | TOTAL_SETS | 100 | GLOBAL |
| `SETS_1000` | VOLUME | THRESHOLD | TOTAL_SETS | 1000 | GLOBAL |
| `REPS_1K` | VOLUME | THRESHOLD | TOTAL_REPS | 1000 | GLOBAL |
| `REPS_10K` | VOLUME | THRESHOLD | TOTAL_REPS | 10000 | GLOBAL |
| `VOLUME_10K_KG` | VOLUME | THRESHOLD | TOTAL_VOLUME_KG | 10000 | GLOBAL |
| `VOLUME_100K_KG` | VOLUME | THRESHOLD | TOTAL_VOLUME_KG | 100000 | GLOBAL |
| `TRAINING_10H` | VOLUME | THRESHOLD | TOTAL_TRAINING_SECONDS | 36000 | GLOBAL |
| `DISTANCE_100_KM` | VOLUME | THRESHOLD | TOTAL_DISTANCE_KM | 100 | GLOBAL |
| `EXERCISES_10` | EXPLORATION | THRESHOLD | DISTINCT_EXERCISES | 10 | GLOBAL |
| `EXERCISES_50` | EXPLORATION | THRESHOLD | DISTINCT_EXERCISES | 50 | GLOBAL |
| `PR_FIRST_MAX_WEIGHT` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_WEIGHT | – | GLOBAL |
| `PR_FIRST_MAX_1RM` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_ESTIMATED_1RM | – | GLOBAL |
| `PR_FIRST_MAX_REPS` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_REPS_AT_WEIGHT | – | GLOBAL |
| `PR_FIRST_MAX_DISTANCE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_DISTANCE | – | GLOBAL |
| `PR_FIRST_MAX_DURATION` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_DURATION | – | GLOBAL |
| `PR_MAX_WEIGHT_PER_EXERCISE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_WEIGHT | – | PER_EXERCISE |
| `PR_MAX_1RM_PER_EXERCISE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_ESTIMATED_1RM | – | PER_EXERCISE |
| `PR_MAX_REPS_PER_EXERCISE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_REPS_AT_WEIGHT | – | PER_EXERCISE |
| `PR_MAX_DISTANCE_PER_EXERCISE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_DISTANCE | – | PER_EXERCISE |
| `PR_MAX_DURATION_PER_EXERCISE` | PERSONAL_RECORD | PERSONAL_RECORD | MAX_DURATION | – | PER_EXERCISE |

---

## 6. Métricas disponibles para las reglas de umbral

Se calculan con una única consulta de agregados sobre `workout_sessions` y
`session_sets` (solo `status = 'COMPLETED'` y `ss.completed = true`).

| Métrica | Definición |
| --- | --- |
| `TOTAL_WORKOUTS` | número de sesiones completadas |
| `TOTAL_SETS` | número de sets completados |
| `TOTAL_REPS` | suma de `reps` de sets completados |
| `TOTAL_VOLUME_KG` | suma de `weight_kg * reps` |
| `TOTAL_TRAINING_SECONDS` | suma de `duration_seconds` |
| `TOTAL_DISTANCE_KM` | suma de `distance_km` |
| `DISTINCT_EXERCISES` | número de ejercicios distintos entrenados |
| `LONGEST_STREAK` | racha máxima de días consecutivos de entrenamiento (zona horaria del usuario) |

La racha se calcula en Java reutilizando
[`StreakCalculator`](../../src/main/java/dev/genesshoan/fitnesstrackerapi/stats/calculator/StreakCalculator.java)
sobre las fechas locales distintas.

> `LONGEST_STREAK` (y no `CURRENT_STREAK`) es la métrica usada por las reglas de
> umbral porque el progreso de un logro debe ser **monótono**: la racha *actual*
> puede caer a cero y eso haría retroceder una insignia ya ganada.

---

## 7. Flujo de evaluación

### 7.1 Disparadores

| Evento | Dispara | Ámbito |
| --- | --- | --- |
| Sesión completada (`IN_PROGRESS` → `COMPLETED`) | umbrales + récords de los ejercicios de la sesión | usuario |
| Set marcado como completado | récords del ejercicio de ese set | usuario + ejercicio |
| Set editado / marcado como incompleto / borrado | recálculo de los récords del ejercicio | usuario + ejercicio |
| Ejercicio eliminado de la sesión | recálculo de los récords del ejercicio | usuario + ejercicio |
| Sesión eliminada | recálculo de los récords de los ejercicios afectados + reevaluación de umbrales | usuario |

### 7.2 Sesión completada

```text
WorkoutSessionService.completeWorkoutSession()
  └─ session.finish()
  └─ gamification.evaluateSessionCompleted(userId, sessionId)   ← misma transacción
       ├─ snapshot = statsPort.getSnapshot(userId)               ← 1 query de agregados + 1 de fechas
       ├─ for each definición THRESHOLD activa:
       │     value = snapshot.valueOf(def.metric())
       │     upsert(user_achievements, progress = GREATEST(actual, value),
       │            unlocked = unlocked OR value >= threshold)
       └─ personalRecordEvaluator.evaluateSession(userId, sessionId)
             └─ for each ejercicio de la sesión (orden por posición y número de set):
                  for each métrica del set:
                       compara contra personal_records
                       si supera → upsert del récord + desbloquea las insignias PR de esa métrica
```

### 7.3 Set completado (feedback en vivo)

Hoy el usuario ve el PR en la respuesta del set. Ese comportamiento se conserva:
la evaluación del récord ocurre al completar el set y la respuesta incluye las
insignias recién desbloqueadas.

El set se compara contra los récords persistidos, que ya incluyen los sets
completados antes en la misma sesión (porque cada uno se persistió al completarse).
Así se mantiene la semántica actual de "un set posterior puede superar a uno
anterior de la misma sesión".

### 7.4 Operaciones destructivas

Cuando una operación puede invalidar un récord (editar un set completado,
descompletarlo, borrarlo, borrar un ejercicio o la sesión), se recalcula el
récord de ese ejercicio con una consulta de ranking acotada a ese ejercicio
(equivalente a la actual `findRankedSets`, pero con el alcance de un ejercicio).

Consecuencias asumidas:

- El **valor mostrado** de un récord se actualiza (puede bajar).
- La **insignia no se revoca** (D6): el usuario ya la ganó.
- El `progress_value` de los logros de umbral **no baja** (`GREATEST`).

---

## 8. Módulo `gamification`

### 8.1 Estructura

Sigue la convención de los módulos ya refactorizados (`auth`, `exercise`):
`domain` / `application` / `infrastructure`.

```text
src/main/java/dev/genesshoan/fitnesstrackerapi/gamification/
├── domain/
│   ├── AchievementDefinition.java        (entidad JPA)
│   ├── UserAchievement.java               (entidad JPA)
│   ├── PersonalRecord.java                (entidad JPA)
│   ├── PersonalRepsRecord.java            (entidad JPA)
│   ├── AchievementCategory.java
│   ├── AchievementTier.java
│   ├── RuleType.java
│   ├── AchievementMetric.java
│   ├── ComparisonOperator.java
│   └── AchievementScope.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── EvaluateAchievementsUseCase.java
│   │   │   ├── AchievementCatalogUseCase.java
│   │   │   └── AchievementFeedUseCase.java
│   │   └── out/
│   │       ├── AchievementDefinitionRepository.java
│   │       ├── UserAchievementRepository.java
│   │       ├── PersonalRecordRepository.java
│   │       └── TrainingMetricsPort.java
│   ├── service/
│   │   ├── AchievementEvaluationService.java
│   │   ├── AchievementQueryService.java
│   │   └── PersonalRecordEvaluator.java    ← la parte de reglas en código
│   └── exception/
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/…
│   │   ├── repository/…
│   │   └── adapter/TrainingMetricsAdapter.java
│   └── web/
│       ├── AchievementController.java
│       ├── dto/…
│       └── mapper/…
```

### 8.2 Puertos

```java
public interface EvaluateAchievementsUseCase {

    /** Evalúa umbrales y récords al completar una sesión. */
    List<UnlockedAchievementResult> onSessionCompleted(UUID userId, UUID sessionId);

    /** Evalúa el récord del ejercicio de un set recién completado. */
    List<UnlockedAchievementResult> onSetCompleted(UUID userId, UUID sessionId, UUID setId);

    /** Recalcula los récords de un ejercicio tras una operación destructiva. */
    void refreshExerciseRecords(UUID userId, UUID exerciseId);
}
```

```java
public interface TrainingMetricsPort {

    /** Agregados del usuario en un instante. */
    TrainingSnapshot getSnapshot(UUID userId);
}
```

`TrainingMetricsPort` lo implementa un adaptador que delega en `stats`, que ya
tiene las consultas de volumen y racha. Para no duplicar SQL, `stats` expone un
port de entrada nuevo:

```java
package dev.genesshoan.fitnesstrackerapi.stats.application.port.in;

public interface TrainingMetricsUseCase {
    TrainingSnapshot getSnapshot(UUID userId);
}
```

### 8.3 Grafo de dependencias resultante

```text
        ┌──────────┐        ┌──────────────┐        ┌──────────┐
        │ workout  │───────►│ gamification │───────►│  stats   │
        └──────────┘  port  └──────────────┘  port  └──────────┘
              │                                   │
              │                                   └─► (SQL directo sobre
              └─► (SQL directo sobre                workout_sessions /
                  workout_sessions /                session_sets)
                  session_sets)
```

Desaparecen los dos imports cruzados actuales:
`workout → stats` (por `StatsService`) y `stats → workout` (por
`WorkoutSessionRepository` y las entidades `WorkoutSession`/`SessionSet`).

---

## 9. Cambios concretos en el código existente

| Archivo | Cambio |
| --- | --- |
| `stats/calculator/AchievementCalculator.java` | **Eliminar** |
| `stats/calculator/OneRepMaxCalculator.java` | **Mover** a `common/calculator/` (lo usan `stats` y `gamification`) |
| `stats/domain/Achievement.java` | **Eliminar** |
| `stats/domain/AchievementType.java` | **Eliminar** (sustituido por `AchievementMetric` + `RuleType`) |
| `stats/domain/PersonalRecordHolder.java`, `PersonalRecordHolders.java` | **Eliminar** (reemplazados por `personal_records`) |
| `stats/dto/AchievementDTO.java` | **Eliminar** |
| `stats/mapper/AchievementMapper.java` | **Eliminar** |
| `stats/service/StatsService.java` | Quitar `calculateForSession` y `calculateForSet`; añadir `getSnapshot` (implementa `TrainingMetricsUseCase`) |
| `stats/repository/StatsRepository.java` | Quitar `findRankedSets`; añadir la query de agregados; **corregir** el filtro de `status = 'COMPLETED'` en `getTrainedDatesBeforeAsc`; sustituir `workoutSessionRepository.existsByIdAndUserId` por una comprobación en SQL |
| `workout/WorkoutSessionService.java` | Sustituir `StatsService` por `EvaluateAchievementsUseCase`; invocar en §7.1; quitar el cálculo del DTO |
| `workout/dto/SessionSetResponseDTO.java` | `achievements` → `newAchievements` (insignias recién desbloqueadas: `code`, `name`, `tier`, `unlockedAt`) |
| `workout/mapper/SessionSetMapper.java`, `SessionExerciseMapper.java`, `WorkoutSessionMapper.java` | Quitar el `@Context Map<UUID, List<AchievementDTO>>` |
| `common/domain/ExerciseMetrics.java` | Se reutiliza en `gamification` sin cambios |
| `src/test/.../stats/calculator/AchievementCalculatorTest.java` | **Eliminar** (o reescribir contra el evaluador nuevo) |
| `src/test/.../stats/calculator/OneRepMaxCalculatorTest.java` | Actualizar el paquete |
| `docs/er.puml` | Añadir las tablas nuevas |
| `docs/endpoints.md` | Añadir los endpoints nuevos |
| `README.md` | Añadir el módulo `gamification` a la tabla de módulos y al árbol de estructura |

### 9.1 Nota sobre `SessionSetResponseDTO.achievements`

Hay dos opciones y conviene decidirla explícitamente:

1. **Mantener el campo, cambiar la fuente** — el set devuelve las insignias que
   desbloqueó. El frontend no cambia; sigue mostrando "¡nuevo récord!".
2. **Eliminarlo** — el frontend consulta `GET /api/v1/achievements/feed`.

La opción 1 es la recomendada: conserva el feedback en vivo sin obligar al
frontend a hacer una segunda llamada.

---

## 10. API propuesta

Base: `/api/v1/achievements`. Todas las respuestas están acotadas al usuario
autenticado (`@CurrentUserId`).

### `GET /api/v1/achievements`

Catálogo con el estado del usuario. Devuelve las definiciones **activas** unidas
con `user_achievements`.

```json
[
  {
    "code": "WORKOUTS_10",
    "name": "Diez entrenamientos",
    "description": "Completa 10 entrenamientos",
    "category": "MILESTONE",
    "tier": "BRONZE",
    "ruleType": "THRESHOLD",
    "metric": "TOTAL_WORKOUTS",
    "threshold": 10,
    "scope": "GLOBAL",
    "progressValue": 7,
    "progressRatio": 0.7,
    "unlocked": false,
    "unlockedAt": null,
    "seen": true
  },
  {
    "code": "PR_MAX_WEIGHT_PER_EXERCISE",
    "name": "Récord de peso",
    "description": "Supera tu mejor marca de peso en un ejercicio",
    "category": "PERSONAL_RECORD",
    "tier": "SILVER",
    "ruleType": "PERSONAL_RECORD",
    "metric": "MAX_WEIGHT",
    "threshold": null,
    "scope": "PER_EXERCISE",
    "exerciseId": "6f1c…",
    "exerciseName": "Press de banca",
    "progressValue": 92.5,
    "progressRatio": null,
    "unlocked": true,
    "unlockedAt": "2026-10-28T18:03:00Z",
    "seen": false
  }
]
```

Filtros opcionales: `?category=CONSISTENCY`, `?unlockedOnly=true`, `?unseenOnly=true`.

### `GET /api/v1/achievements/{code}`

Detalle de un logro. `404` si el `code` no existe o está desactivado.

### `GET /api/v1/achievements/feed`

Desbloqueos recientes, del más nuevo al más viejo.

```
GET /api/v1/achievements/feed?unseenOnly=true&limit=50
```

Respuesta: lista de objetos con `id`, `code`, `name`, `tier`, `category`,
`unlockedAt`, `seen`, `exerciseId`, `exerciseName`.

Es el mecanismo del "feed in-app" (D7): el frontend llama a este endpoint, muestra
los no vistos y luego los marca como vistos.

### `POST /api/v1/achievements/seen`

Marca insignias como vistas.

```json
{ "achievementIds": ["9b2e…", "1f77…"] }
```

→ `204 No Content`. Idempotente.

### `GET /api/v1/achievements/summary`

```json
{
  "totalAvailable": 30,
  "totalUnlocked": 12,
  "unseenCount": 2,
  "byCategory": { "MILESTONE": { "unlocked": 3, "total": 5 } },
  "next": [
    { "code": "WORKOUTS_10", "name": "Diez entrenamientos", "progressValue": 7, "threshold": 10, "remaining": 3 }
  ]
}
```

`next` ordena por `progressRatio` descendente entre los no desbloqueados: lo más
cercano a completarse.

---

## 11. Semánticas, invariantes y casos límite

### 11.1 Invariantes

1. Un usuario tiene **como máximo una fila** por `(definición, ámbito)`.
2. `unlocked = true` ⟺ `unlocked_at IS NOT NULL`.
3. `seen = true` ⟹ `unlocked = true` (solo se marca como vista una insignia desbloqueada).
4. `progress_value` de un logro de umbral **nunca decrece**.
5. Un logro desbloqueado **nunca se revoca** (D6).

### 11.2 Empates

Igual que hoy: solo se desbloquea con un valor **estrictamente mayor**. Un empate
no genera insignia nueva. Esto se conserva en `PersonalRecordEvaluator`.

### 11.3 Sin backfill: qué implica exactamente

Decisión D8: no se otorgan logros retroactivos. Pero los contadores **sí** se leen
de la fuente en el momento de evaluar (D5). La consecuencia práctica:

> Un usuario con 50 entrenamientos previos que completa su sesión número 51
> desbloquea `WORKOUTS_50` en ese instante, con `unlocked_at = ahora`.

No es un backfill (no hay proceso que revise el pasado ni una concesión masiva de
insignias el día del despliegue), sino el resultado natural de evaluar el estado
actual cuando ocurre un evento nuevo. Además evita el problema opuesto: si los
contadores empezaran en cero, la gamificación contradiría a `/stats/volume`, que
sí refleja todo el historial.

Lo que **no** ocurre: ninguna insignia aparece en el feed de un usuario que no
complete un entrenamiento nuevo después del despliegue.

### 11.4 Zona horaria

- La racha se calcula con las fechas locales del usuario (`users.timezone`), igual
  que hoy.
- La zona horaria se lee **en el momento de la evaluación**. Si el usuario cambia
  su zona horaria, las rachas ya calculadas no se recalculan automáticamente.
  Decisión:acceptable, porque `LONGEST_STREAK` es monotónica y un cambio de zona
  solo puede mover el resultado en un día. Si más adelante se quiere corregir, el
  `refreshExerciseRecords` es el mismo punto de entrada.

### 11.5 Borrado de sesiones y ejercicios

| Operación | Efecto |
| --- | --- |
| Eliminar un set | se recalcula el récord de su ejercicio; la insignia no se revoca |
| Eliminar un ejercicio de la sesión | ídem |
| Eliminar una sesión | se recalculan los récords de sus ejercicios; los logros de umbral no se revocan ni retroceden |
| Eliminar un ejercicio del catálogo | `ON DELETE CASCADE` sobre `personal_records` y sobre las filas `PER_EXERCISE`; las definiciones `GLOBAL` sobreviven |

### 11.6 Concurrencia

Dos completados simultáneos del mismo usuario podrían pisarse. Mitigación:

- **Récords**: `SELECT … FOR UPDATE` sobre las filas de `personal_records` del
  ejercicio afectado antes de comparar (el proyecto ya usa `findForUpdate…` en
  `workout/repository`).
- **Logros de usuario**: *upsert* atómico con
  `ON CONFLICT (user_id, achievement_definition_id, exercise_id) DO UPDATE`, que
  infiere el índice `uk_user_achievements_identity` del §5.3. PostgreSQL 18
  permite la inferencia sobre un índice `NULLS NOT DISTINCT`, así que la clave
  nula del ámbito global no necesita una columna auxiliar.

---

## 12. Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| Guardar solo un contador de "logros conseguidos" | No permite saber *qué* se desbloqueó ni el progreso |
| Recalcular los logros en cada lectura (status quo) | Es exactamente lo que se quiere eliminar |
| Tabla genérica `user_stats` con JSON | No se puede indexar ni garantizar unicidad; `${}` de PostgreSQL para columnas dinámicas |
| Un motor de reglas completo en base de datos | Excesivo para el caso; los récords personales no se expresan bien como `threshold` |
| Materialización recalculada en cada GET | reintroduce exactamente el coste por lectura que se quiere eliminar |
| XP y niveles desde el inicio | No se pidió; añadirlo después es un `points INT` en `user_achievements` más una tabla de niveles |

---

## 13. Plan de implementación por PR

Cada PR es vertical, revisable y migrable por separado.

| # | Rama | Contenido | Migración |
| --- | --- | --- | --- |
| 0 | `fix/test-schema-ownership` ✅ | Flyway como fuente de verdad del esquema en los tests, `V18` y alineación de las entidades | `V18` |
| 1 | `feature/gamification-schema` | Tablas, entidades, repositorios e índices únicos | `V19`–`V22` |
| 2 | `feature/gamification-metrics` | `TrainingMetricsUseCase` en `stats`, `TrainingSnapshot`, corrección del filtro de `status` en la racha | – |
| 3 | `feature/gamification-evaluator` | `AchievementEvaluationService`, `PersonalRecordEvaluator`, `EvaluateAchievementsUseCase` | – |
| 4 | `feature/gamification-wiring` | Invocación desde `workout` (sesión completada, set completado, operaciones destructivas) | – |
| 5 | `feature/gamification-api` | Controller, DTOs, feed, marcar vistos, summary | – |
| 6 | `feature/gamification-decouple-stats` | Borrado del camino de cálculo en vuelo; `stats` deja de depender de `workout` y `workout` de `stats` | – |
| 7 | `docs/gamification` | `docs/gamification/{README,api,business-rules,frontend}.md`, `docs/er.puml`, `docs/endpoints.md`, `README.md` | – |

El orden importa: la funcionalidad queda operativa en el PR 4-5, y el PR 6 es la
limpieza. Durante los PR 1-5 conviven los dos caminos de cálculo, lo que evita un
cambio de comportamiento todo-en-uno.

---

## 14. Estrategia de pruebas

| Tipo | Ubicación | Qué cubre |
| --- | --- | --- |
| Unitario | `gamification/…/AchievementEvaluationServiceTest` | Umbrales, `GREATEST`, idempotencia del desbloqueo, umbrales con `GT` |
| Unitario | `gamification/…/PersonalRecordEvaluatorTest` | Empates, orden dentro de la sesión, comparación por métrica y por peso |
| Repositorio | `gamification/repository/*Test` | Upsert, constraint de unicidad, consultas del feed y de "vistos" |
| Servicio | `workout/WorkoutSessionServiceTest` | Se evalúa al completar sesión, al completar set y en operaciones destructivas |
| Integración | `gamification/controller/AchievementControllerIT` | Endpoints, aislamiento por usuario, 401/404 |
| Integración | `stats/controller/StatsControllerIT` | El nuevo `TrainingSnapshot` no altera los endpoints actuales |

Los tests de integración y de repositorio corren contra el esquema que producen
las migraciones, con Hibernate en modo `validate` (§15.3). Una tabla nueva mal
migrada, o una entidad que no coincide con su migración, hace fallar el build.

---

## 15. Riesgos y observaciones

### 15.1 El catálogo es código de negocio en base de datos

Con D3, el seed `V22` es el contrato del producto: un error de umbral es un
error de datos. Mitigación: tratar el seed como fuente revisable y añadir un test
que valide que **toda** definición activa es evaluable por el evaluador (métrica
conocida, umbral coherente con el tipo de regla).

### 15.2 El coste de la evaluación crece con el historial

El snapshot aggregate escanea el historial del usuario. Con índices sobre
`(user_id, status)` en `workout_sessions` y `(session_exercise_id, completed)` en
`session_sets` es aceptable para el volumen esperado. Si el perfil de un usuario
crece mucho, el siguiente paso sería materializar los contadores — pero eso
reintroduce el problema de consistencia que D5 evita a propósito.

### 15.3 Flyway es la fuente de verdad del esquema (resuelto)

**Problema verificado ejecutando la suite.** El perfil de test corría con
`ddl-auto: create-drop`, de modo que Hibernate borraba el esquema migrado y lo
reconstruía a partir de las entidades. Las migraciones nunca se ejercitaban y una
columna que divergiera entre una migración y su entidad pasaba inadvertida.

**Estado actual.** El perfil de test usa `ddl-auto: validate`
([`application-test.yml`](../../src/main/resources/application-test.yml)), así que:

1. Flyway migra el esquema completo (`V1`…`V18`), con sus índices, sus claves
   foráneas y sus `CHECK`.
2. Hibernate valida el mapeo de las entidades contra ese esquema al arrancar el
   contexto. Cualquier divergencia hace que el contexto no cargue y el build
   falla.

La suite corre por tanto contra el mismo esquema que producción, con las claves
foráneas, los `CHECK` y los índices reales. El catálogo de referencia que siembran
las migraciones se limpia después, porque los tests construyen su propio catálogo
y afirman sobre contenidos exactos.

**Divergencias que el cambio destapó.** Al activar `validate` aparecieron cinco
columnas que nadie había visto:

| Tabla | Columna | Migración | Entidad |
| --- | --- | --- | --- |
| `muscle_assets` | `created_at`, `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `timestamptz` (`Instant`) |
| `exercises` | `description` | `text` | `varchar(255)` |
| `exercises` | `instructions` | `text[]` | `varchar(255)[]` |
| `routine_exercises` | `notes` | `text` | `varchar(255)` |

La de `muscle_assets` era un defecto real: la columna guardaba instantes en una
columna sin zona horaria, que se reinterpretaba en la zona de la sesión al
leerla. Se corrige con la migración `V18`. Las otras tres se corrigen alineando
las entidades con las migraciones, que son la fuente de verdad.

**Consecuencia para este diseño.** Al ser Flyway la fuente de verdad, las
constraints, los índices y las claves foráneas viven en las migraciones, y se
puede usar todo lo que ofrece PostgreSQL 18. Eso permite el índice único con
`NULLS NOT DISTINCT` del §5.3 y hace innecesaria la columna auxiliar `scope_key`.

### 15.4 Nomenclatura

`docs/adr/` tiene **dos** ADR con el número `0007`
(`0007-use-s3-compatible-object-storage-sdk.md` y
`0007-use-transactional-event-listener-for-post-commit.md`). Conviene renumerar
los duplicados antes de que se acumulen.

Además, el `CHECK` de reps de `session_sets` se llama `uk_session_sets_reps`, con
el prefijo de una unique key, por errata en `V11`. Es solo el nombre, pero induce
a leerlo como una restricción de unicidad.

---

## 16. Fuera de alcance

- **XP, puntos y niveles**: no se incluyen. Añadirlos después requiere una
  columna `points` en `user_achievements` (o una tabla de movimientos de puntos) y
  una tabla de niveles; el modelo de desbloqueo no cambia.
- **Ranking / leaderboard**: no se incluye. Requiere decidir alcance (global,
  amigos, mensual), coste de lectura y política de privacidad.
- **Insignias con assets (iconos)**: `icon_key` queda preparado, pero el
  almacenamiento de imágenes de insignia no está diseñado.
- **Desbloqueo por email**: solo feed in-app (D7). El módulo `notification` ya
  tiene el patrón (`@TransactionalEventListener(AFTER_COMMIT)` + Thymeleaf) por si
  en el futuro se quiere añadir.

---

## 17. Recomendación de flujo de trabajo

### 17.1 Recomendación: **no adoptar git flow**. Mantener trunk-based (GitHub Flow).

El repositorio ya funciona con GitHub Flow: `main` siempre desplegable, ramas
cortas `feature/*`, `fix/*`, `docs/*`, PR con CI, *conventional commits*, y
release por tags `v*.*.*` ([`release.yml`](../../.github/workflows/release.yml)
publica la imagen Docker y crea la GitHub Release desde `main`).

git flow añadiría `develop`, `release/*` y `hotfix/*` con merges dobles y
divergencia prolongada. En este proyecto concreto el coste es alto y el
beneficio casi nulo:

1. **Flyway y `develop` no se llevan bien.** Las migraciones se numeran de forma
   global. Con `develop` y `main` vivos a la vez, el mismo `V19` aplicado en
   `develop` y con otro contenido en `main` produce conflictos de checksum y
   errores de "version already applied" al integrar. Es el problema clásico de
   git flow + Flyway, y en este repo cada PR de gamificación añade migraciones.
2. **El release ya está resuelto por tags.** `release/*` de git flow duplicaría el
   mecanismo de tags que ya funciona.
3. **El tamaño del equipo no lo justifica.** El historial muestra un desarrollador
   principal con revisor ocasional. git flow rinde cuando hay varios integradores
   y releases coordinados; aquí añade ceremonia sin reducir conflictos.
4. **El CI ya está preparado para trunk-based.** [`ci.yml`](../../.github/workflows/ci.yml)
   corre `spotlessCheck` y `build` en cada PR y en cada push a `main`.

### 17.2 Ajustes concretos que sí valen la pena

| Acción | Motivo |
| --- | --- |
| Quitar `develop` de los triggers de `ci.yml` | No existe esa rama; hoy es configuración muerta |
| Eliminar la rama `feature/game` | Es idéntica a `main` (placeholder vacío) |
| Limpiar las ramas `backup/*` | Restos de rebases/stashes; ya no aportan información |
| Renumerar el ADR `0007` duplicado | Ver §15.4 |
| Adoptar *squash merge* en PRs | Mantiene `main` legible y el historial por PR, no por commit |

### 17.3 Cómo encaja el trabajo de gamificación

La parte 13 de este documento **ya es un plan de ramas cortas**. Los siete PRs
siguen el patrón que el proyecto ya usa, sin necesidad de `develop`.

Si en algún momento se quiere un periodo de estabilización antes de que la
gamificación llegue a producción, la alternativa compatible con git flow es
**una rama de RELEASE**, no `develop`:

```text
main ──●───────●───────────●────────►  (producción, releases por tag)
       \                /
        ●───●───●───●───●───►           release/gamification-1.0
```

Es git flow "de facto" pero con una sola rama deRelease en lugar de `develop`
permanente: `main` sigue siendo la línea de producción y la rama de release
absorbe el trabajo grande hasta que esté listo. Para este proyecto, con la
opción de dejar cada PR en `main` y usar feature flags para la activación, es
más simple y suficiente.

---

## 18. Referencias internas

- [ADR-0009: arquitectura hexagonal pragmática](../adr/0009-adopt-pragmatic-hexagonal-architecture.md)
- [ADR-0007: listeners transaccionales para efectos post-commit](../adr/0007-use-transactional-event-listener-for-post-commit.md)
- [ADR-0012: persistir los logros en vez de calcularlos en vuelo](../adr/0012-persist-achievements-instead-of-computing-them-on-demand.md)
- [ADR-0013: Flyway como fuente de verdad del esquema](../adr/0013-make-flyway-the-single-source-of-truth-for-the-database-schema.md)
- [Reglas de negocio de stats](../stats/business-rules.md)
- [Documentación del módulo workout](../workout/README.md)
