package amigo_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestTag {

    BOLLYWOOD_MUSIC(
            InterestCategory.MUSIC,
            "bollywood music"
    ),
    INDIE_MUSIC(
            InterestCategory.MUSIC,
            "indie music"
    ),
    EDM(
            InterestCategory.MUSIC,
            "edm"
    ),
    ROCK(
            InterestCategory.MUSIC,
            "rock"
    ),
    HIP_HOP(
            InterestCategory.MUSIC,
            "hip hop"
    ),
    SUFI(
            InterestCategory.MUSIC,
            "sufi"
    ),
    MUSIC_FESTIVALS(
            InterestCategory.MUSIC,
            "music festivals"
    ),

    STAND_UP_COMEDY(
            InterestCategory.COMEDY,
            "stand-up comedy"
    ),
    IMPROV(
            InterestCategory.COMEDY,
            "improv"
    ),

    CRICKET(
            InterestCategory.SPORTS,
            "cricket"
    ),
    FOOTBALL(
            InterestCategory.SPORTS,
            "football"
    ),
    KABADDI(
            InterestCategory.SPORTS,
            "kabaddi"
    ),

    BOLLYWOOD_FILMS(
            InterestCategory.MOVIES,
            "bollywood films"
    ),
    HOLLYWOOD_FILMS(
            InterestCategory.MOVIES,
            "hollywood films"
    ),
    ANIME(
            InterestCategory.MOVIES,
            "anime"
    ),

    THEATRE(
            InterestCategory.THEATRE,
            "theatre"
    ),
    MUSICALS(
            InterestCategory.THEATRE,
            "musicals"
    ),

    PHOTOGRAPHY(
            InterestCategory.PHOTOGRAPHY,
            "photography"
    ),

    POTTERY(
            InterestCategory.ARTS_AND_CRAFTS,
            "pottery"
    ),

    FOOD_FESTIVALS(
            InterestCategory.FOOD,
            "food festivals"
    ),

    CODING(
            InterestCategory.TECHNOLOGY,
            "coding"
    ),
    TECH(
            InterestCategory.TECHNOLOGY,
            "tech"
    ),
    STARTUPS(
            InterestCategory.TECHNOLOGY,
            "startups"
    ),
    AI(
            InterestCategory.TECHNOLOGY,
            "ai"
    );

    private final InterestCategory category;
    private final String value;
}
