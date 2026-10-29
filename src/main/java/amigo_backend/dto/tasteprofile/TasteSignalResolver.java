package amigo_backend.dto.tasteprofile;

import amigo_backend.enums.TasteDimension;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TasteSignalResolver {

    public TasteSignal resolve(
            String questionId,
            String optionId
    ) {
        return switch (questionId) {

            case "EDM_ENVIRONMENT" ->
                    resolveEdmEnvironment(optionId);

            case "MUSIC_ENERGY" ->
                    resolveMusicEnergy(optionId);

            case "MUSIC_COMPANION" ->
                    resolveMusicCompanion(optionId);

            case "NIGHTLIFE_PREFERENCE" ->
                    resolveNightlife(optionId);

            case "MUSIC_DISCOVERY" ->
                    resolveMusicDiscovery(optionId);

            case "CRICKET_ACTIVITY" ->
                    resolveCricketActivity(optionId);

            case "SPORT_EVENT_COMPANION" ->
                    resolveSportCompanion(optionId);

            case "AI_EVENT" ->
                    resolveAiEvent(optionId);

            case "TECH_MOTIVATION" ->
                    resolveTechMotivation(optionId);

            case "SCREENING_STYLE" ->
                    resolveScreeningStyle(optionId);

            case "ANIME_MOTIVATION" ->
                    resolveAnimeMotivation(optionId);

            case "EVENT_PRIORITY" ->
                    resolveEventPriority(optionId);

            case "NEW_PERSON_COMFORT" ->
                    resolveNewPersonComfort(optionId);

            case "EVENT_PERSONALITY" ->
                    resolveEventPersonality(optionId);

            default ->
                    new TasteSignal(Map.of(), Map.of());
        };
    }

    private TasteSignal resolveEdmEnvironment(String optionId) {
        return switch (optionId) {

            case "CLUBS" -> signal(
                    Map.of(
                            TasteDimension.MUSIC, 0.20,
                            TasteDimension.NIGHTLIFE, 0.35,
                            TasteDimension.PARTY, 0.30
                    )
            );

            case "FESTIVALS" -> signal(
                    Map.of(
                            TasteDimension.MUSIC, 0.25,
                            TasteDimension.PARTY, 0.20,
                            TasteDimension.SOCIAL, 0.20,
                            TasteDimension.LIVE_EVENT, 0.30
                    )
            );

            case "CONCERTS" -> signal(
                    Map.of(
                            TasteDimension.MUSIC, 0.30,
                            TasteDimension.LIVE_EVENT, 0.30,
                            TasteDimension.SOCIAL, 0.10
                    )
            );

            case "HOME" -> signal(
                    Map.of(
                            TasteDimension.MUSIC, 0.25,
                            TasteDimension.NIGHTLIFE, -0.15,
                            TasteDimension.PARTY, -0.15
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveMusicEnergy(String optionId) {
        return switch (optionId) {

            case "HIGH_ENERGY" -> signal(
                    Map.of(
                            TasteDimension.PARTY, 0.30,
                            TasteDimension.SOCIAL, 0.20,
                            TasteDimension.NIGHTLIFE, 0.20
                    )
            );

            case "BALANCED" -> signal(
                    Map.of(
                            TasteDimension.PARTY, 0.10,
                            TasteDimension.SOCIAL, 0.10
                    )
            );

            case "RELAXED" -> signal(
                    Map.of(
                            TasteDimension.PARTY, -0.10,
                            TasteDimension.SOCIAL, 0.05
                    )
            );

            case "INTIMATE" -> signal(
                    Map.of(
                            TasteDimension.PARTY, -0.15,
                            TasteDimension.SOCIAL, -0.05
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveMusicCompanion(String optionId) {
        return switch (optionId) {

            case "FRIENDS" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.15)
            );

            case "GROUP" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.25,
                            TasteDimension.GROUP_PREFERENCE, 0.30
                    )
            );

            case "FEW_PEOPLE" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.10,
                            TasteDimension.GROUP_PREFERENCE, 0.10
                    )
            );

            case "ALONE" -> signal(
                    Map.of(
                            TasteDimension.SOLO_COMFORT, 0.30,
                            TasteDimension.GROUP_PREFERENCE, -0.20
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveNightlife(String optionId) {
        return switch (optionId) {

            case "LOVE" -> signal(
                    Map.of(
                            TasteDimension.NIGHTLIFE, 0.40,
                            TasteDimension.PARTY, 0.30,
                            TasteDimension.SOCIAL, 0.20
                    )
            );

            case "OCCASIONAL" -> signal(
                    Map.of(
                            TasteDimension.NIGHTLIFE, 0.15,
                            TasteDimension.PARTY, 0.10
                    )
            );

            case "QUIET" -> signal(
                    Map.of(
                            TasteDimension.NIGHTLIFE, -0.20,
                            TasteDimension.PARTY, -0.15
                    )
            );

            case "AVOID" -> signal(
                    Map.of(
                            TasteDimension.NIGHTLIFE, -0.40,
                            TasteDimension.PARTY, -0.30
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveMusicDiscovery(String optionId) {
        return switch (optionId) {

            case "FRIENDS" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.20,
                            TasteDimension.EXPLORATION, 0.15
                    )
            );

            case "SOCIAL_MEDIA" -> signal(
                    Map.of(TasteDimension.EXPLORATION, 0.20)
            );

            case "LIVE_EVENTS" -> signal(
                    Map.of(
                            TasteDimension.EXPLORATION, 0.25,
                            TasteDimension.LIVE_EVENT, 0.20
                    )
            );

            case "STREAMING" -> signal(
                    Map.of(TasteDimension.EXPLORATION, 0.15)
            );

            case "FAMILIAR" -> signal(
                    Map.of(TasteDimension.EXPLORATION, -0.15)
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveCricketActivity(String optionId) {
        return switch (optionId) {

            case "WATCHING" -> signal(
                    Map.of(TasteDimension.SPORTS, 0.25)
            );

            case "PLAYING" -> signal(
                    Map.of(
                            TasteDimension.SPORTS, 0.25,
                            TasteDimension.OUTDOOR, 0.20
                    )
            );

            case "LIVE" -> signal(
                    Map.of(
                            TasteDimension.SPORTS, 0.25,
                            TasteDimension.LIVE_EVENT, 0.30,
                            TasteDimension.SOCIAL, 0.15
                    )
            );

            case "BOTH" -> signal(
                    Map.of(
                            TasteDimension.SPORTS, 0.30,
                            TasteDimension.OUTDOOR, 0.15,
                            TasteDimension.LIVE_EVENT, 0.15
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveSportCompanion(String optionId) {
        return switch (optionId) {

            case "DEFINITELY" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.25,
                            TasteDimension.EXPLORATION, 0.15
                    )
            );

            case "MAYBE" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.10,
                            TasteDimension.EXPLORATION, 0.05
                    )
            );

            case "KNOWN_ONLY" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, -0.05,
                            TasteDimension.EXPLORATION, -0.10
                    )
            );

            case "NO" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, -0.20,
                            TasteDimension.EXPLORATION, -0.20
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveAiEvent(String optionId) {
        return switch (optionId) {

            case "CONFERENCE" -> signal(
                    Map.of(
                            TasteDimension.TECHNOLOGY, 0.25,
                            TasteDimension.ACADEMIC, 0.15
                    )
            );

            case "HACKATHON" -> signal(
                    Map.of(
                            TasteDimension.TECHNOLOGY, 0.25,
                            TasteDimension.CREATIVE, 0.15,
                            TasteDimension.COMPETITIVE, 0.15
                    )
            );

            case "STARTUP" -> signal(
                    Map.of(
                            TasteDimension.TECHNOLOGY, 0.25,
                            TasteDimension.SOCIAL, 0.15
                    )
            );

            case "MEETUP" -> signal(
                    Map.of(
                            TasteDimension.TECHNOLOGY, 0.20,
                            TasteDimension.SOCIAL, 0.20
                    )
            );

            case "WORKSHOP" -> signal(
                    Map.of(
                            TasteDimension.TECHNOLOGY, 0.20,
                            TasteDimension.CREATIVE, 0.10
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveTechMotivation(String optionId) {
        return switch (optionId) {

            case "LEARNING" -> signal(
                    Map.of(TasteDimension.ACADEMIC, 0.25)
            );

            case "BUILDING" -> signal(
                    Map.of(
                            TasteDimension.CREATIVE, 0.25,
                            TasteDimension.TECHNOLOGY, 0.15
                    )
            );

            case "PEOPLE" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.25)
            );

            case "NETWORKING" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.30)
            );

            case "CAREER" -> signal(
                    Map.of(
                            TasteDimension.ACADEMIC, 0.15,
                            TasteDimension.SOCIAL, 0.15
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveScreeningStyle(String optionId) {
        return switch (optionId) {

            case "ALONE" -> signal(
                    Map.of(
                            TasteDimension.SOLO_COMFORT, 0.25,
                            TasteDimension.SOCIAL, -0.10
                    )
            );

            case "FRIENDS" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.15)
            );

            case "GROUP" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.25,
                            TasteDimension.GROUP_PREFERENCE, 0.25
                    )
            );

            case "SCREENINGS" -> signal(
                    Map.of(
                            TasteDimension.LIVE_EVENT, 0.25,
                            TasteDimension.SOCIAL, 0.15
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveAnimeMotivation(String optionId) {
        return switch (optionId) {

            case "STORY" -> signal(
                    Map.of(TasteDimension.MOVIES, 0.25)
            );

            case "CHARACTERS" -> signal(
                    Map.of(TasteDimension.MOVIES, 0.20)
            );

            case "ART" -> signal(
                    Map.of(
                            TasteDimension.MOVIES, 0.15,
                            TasteDimension.ARTS, 0.20
                    )
            );

            case "ACTION" -> signal(
                    Map.of(
                            TasteDimension.MOVIES, 0.20,
                            TasteDimension.PARTY, 0.05
                    )
            );

            case "COMMUNITY" -> signal(
                    Map.of(
                            TasteDimension.MOVIES, 0.20,
                            TasteDimension.SOCIAL, 0.30
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveEventPriority(String optionId) {
        return switch (optionId) {

            case "INTERESTS" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.05)
            );

            case "ENERGY" -> signal(
                    Map.of(TasteDimension.PARTY, 0.10)
            );

            case "TALK" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.20)
            );

            case "ADVENTURE" -> signal(
                    Map.of(TasteDimension.EXPLORATION, 0.25)
            );

            case "COMFORT" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.10)
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveNewPersonComfort(String optionId) {
        return switch (optionId) {

            case "VERY_OPEN" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.30,
                            TasteDimension.EXPLORATION, 0.20
                    )
            );

            case "SOMEWHAT_OPEN" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.15,
                            TasteDimension.EXPLORATION, 0.10
                    )
            );

            case "EVENT_DEPENDENT" -> signal(
                    Map.of(TasteDimension.EXPLORATION, 0.05)
            );

            case "PREFER_KNOWN" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, -0.15,
                            TasteDimension.EXPLORATION, -0.15
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal resolveEventPersonality(String optionId) {
        return switch (optionId) {

            case "START_CONVERSATIONS" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.30)
            );

            case "RESPOND" -> signal(
                    Map.of(TasteDimension.SOCIAL, 0.15)
            );

            case "STAY_GROUP" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.05,
                            TasteDimension.GROUP_PREFERENCE, 0.20
                    )
            );

            case "EVENT_FOCUS" -> signal(
                    Map.of(TasteDimension.SOCIAL, -0.05)
            );

            case "SMALL_GROUP" -> signal(
                    Map.of(
                            TasteDimension.SOCIAL, 0.10,
                            TasteDimension.GROUP_PREFERENCE, 0.25
                    )
            );

            default -> emptySignal();
        };
    }

    private TasteSignal signal(
            Map<TasteDimension, Double> dimensions
    ) {
        return new TasteSignal(dimensions, Map.of());
    }

    private TasteSignal emptySignal() {
        return new TasteSignal(Map.of(), Map.of());
    }
}