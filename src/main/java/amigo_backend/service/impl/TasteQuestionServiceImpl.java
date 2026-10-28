package amigo_backend.service.impl;

import amigo_backend.dto.tasteprofile.TasteQuestionDTO;
import amigo_backend.dto.tasteprofile.TasteQuestionOptionDTO;
import amigo_backend.enums.InterestTag;
import amigo_backend.service.TasteQuestionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TasteQuestionServiceImpl implements TasteQuestionService {

    @Override
    public List<TasteQuestionDTO> generateQuestions(
            List<InterestTag> interests
    ) {
        List<TasteQuestionDTO> questions = new ArrayList<>();

        if (interests.contains(InterestTag.EDM)) {
            questions.add(new TasteQuestionDTO(
                    "EDM_ENVIRONMENT",
                    "What kind of EDM experience do you enjoy most?",
                    List.of(
                            option("CLUBS", "Club and DJ nights"),
                            option("FESTIVALS", "Music festivals"),
                            option("CONCERTS", "Live concerts"),
                            option("HOME", "Mostly listening at home")
                    )
            ));

            questions.add(new TasteQuestionDTO(
                    "MUSIC_ENERGY",
                    "What kind of atmosphere do you prefer at music events?",
                    List.of(
                            option("HIGH_ENERGY", "High-energy and crowded"),
                            option("BALANCED", "Energetic but not too crowded"),
                            option("RELAXED", "Relaxed and comfortable"),
                            option("INTIMATE", "Small and intimate")
                    )
            ));

            questions.add(new TasteQuestionDTO(
                    "MUSIC_COMPANION",
                    "Who would you ideally go to a music event with?",
                    List.of(
                            option("FRIENDS", "Close friends"),
                            option("GROUP", "A larger group"),
                            option("FEW_PEOPLE", "One or two people"),
                            option("ALONE", "I am comfortable going alone")
                    )
            ));

            questions.add(new TasteQuestionDTO(
                    "NIGHTLIFE_PREFERENCE",
                    "How do you feel about nightlife?",
                    List.of(
                            option("LOVE", "I love it"),
                            option("OCCASIONAL", "I enjoy it occasionally"),
                            option("QUIET", "I prefer quieter evenings"),
                            option("AVOID", "I usually avoid it")
                    )
            ));
        }

        if (interests.contains(InterestTag.MUSIC_FESTIVALS)) {
            questions.add(new TasteQuestionDTO(
                    "MUSIC_DISCOVERY",
                    "How do you usually discover new music or artists?",
                    List.of(
                            option("FRIENDS", "Friends"),
                            option("SOCIAL_MEDIA", "Instagram or YouTube"),
                            option("LIVE_EVENTS", "Live events"),
                            option("STREAMING", "Playlists and streaming"),
                            option("FAMILIAR", "I mostly stick to what I know")
                    )
            ));
        }

        if (interests.contains(InterestTag.CRICKET)) {
            questions.add(new TasteQuestionDTO(
                    "CRICKET_ACTIVITY",
                    "How do you enjoy cricket?",
                    List.of(
                            option("WATCHING", "Watching matches"),
                            option("PLAYING", "Playing cricket"),
                            option("LIVE", "Going to live matches"),
                            option("BOTH", "Watching and playing")
                    )
            ));

            questions.add(new TasteQuestionDTO(
                    "SPORT_EVENT_COMPANION",
                    "Would you attend a sports event with people you have not met before?",
                    List.of(
                            option("DEFINITELY", "Definitely"),
                            option("MAYBE", "Maybe"),
                            option("KNOWN_ONLY", "Only with someone I know"),
                            option("NO", "No")
                    )
            ));
        }

        if (interests.contains(InterestTag.AI)) {
            questions.add(new TasteQuestionDTO(
                    "AI_EVENT",
                    "What type of technology event interests you most?",
                    List.of(
                            option("CONFERENCE", "AI or tech conferences"),
                            option("HACKATHON", "Hackathons"),
                            option("STARTUP", "Startup events"),
                            option("MEETUP", "Tech meetups"),
                            option("WORKSHOP", "Workshops")
                    )
            ));

            questions.add(new TasteQuestionDTO(
                    "TECH_MOTIVATION",
                    "What do you enjoy most at technology events?",
                    List.of(
                            option("LEARNING", "Learning something new"),
                            option("BUILDING", "Building things"),
                            option("PEOPLE", "Meeting people"),
                            option("NETWORKING", "Networking"),
                            option("CAREER", "Career opportunities")
                    )
            ));
        }

        if (interests.contains(InterestTag.ANIME)
                || interests.contains(InterestTag.BOLLYWOOD_FILMS)
                || interests.contains(InterestTag.HOLLYWOOD_FILMS)) {

            questions.add(new TasteQuestionDTO(
                    "SCREENING_STYLE",
                    "How do you usually enjoy anime or movies?",
                    List.of(
                            option("ALONE", "Watching alone"),
                            option("FRIENDS", "Watching with close friends"),
                            option("GROUP", "Watching with a larger group"),
                            option("SCREENINGS", "Going to screenings or events")
                    )
            ));

            if (interests.contains(InterestTag.ANIME)) {
                questions.add(new TasteQuestionDTO(
                        "ANIME_MOTIVATION",
                        "What attracts you most to anime?",
                        List.of(
                                option("STORY", "Story"),
                                option("CHARACTERS", "Characters"),
                                option("ART", "Animation and art"),
                                option("ACTION", "Action"),
                                option("COMMUNITY", "Community and fandom")
                        )
                ));
            }
        }

        questions.add(new TasteQuestionDTO(
                "EVENT_PRIORITY",
                "When attending an event, what matters most to you?",
                List.of(
                        option("INTERESTS", "Having similar interests"),
                        option("ENERGY", "Having a similar energy level"),
                        option("TALK", "Having someone easy to talk to"),
                        option("ADVENTURE", "Going with someone adventurous"),
                        option("COMFORT", "Feeling comfortable and safe")
                )
        ));

        questions.add(new TasteQuestionDTO(
                "NEW_PERSON_COMFORT",
                "How open are you to attending an event with someone you have not met before?",
                List.of(
                        option("VERY_OPEN", "Very open"),
                        option("SOMEWHAT_OPEN", "Somewhat open"),
                        option("EVENT_DEPENDENT", "Depends on the event"),
                        option("PREFER_KNOWN", "I prefer people I already know")
                )
        ));

        questions.add(new TasteQuestionDTO(
                "EVENT_PERSONALITY",
                "Which describes you best at events?",
                List.of(
                        option("START_CONVERSATIONS", "I start conversations easily"),
                        option("RESPOND", "I talk once someone approaches me"),
                        option("STAY_GROUP", "I prefer staying with my group"),
                        option("EVENT_FOCUS", "I mostly enjoy the event itself"),
                        option("SMALL_GROUP", "I prefer smaller groups")
                )
        ));

        return questions;
    }

    private TasteQuestionOptionDTO option(
            String id,
            String label
    ) {
        return new TasteQuestionOptionDTO(id, label);
    }
}
