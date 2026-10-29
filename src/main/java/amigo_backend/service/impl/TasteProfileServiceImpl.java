    package amigo_backend.service.impl;

    import amigo_backend.dto.tasteprofile.*;
    import amigo_backend.entity.TasteProfile;
    import amigo_backend.entity.User;
    import amigo_backend.entity.UserInterest;
    import amigo_backend.enums.InterestTag;
    import amigo_backend.enums.TasteDimension;
    import amigo_backend.repository.TasteProfileRepository;
    import amigo_backend.repository.UserInterestRepository;
    import amigo_backend.repository.UserRepository;
    import amigo_backend.service.TasteProfileService;
    import amigo_backend.service.TasteQuestionService;

    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.ArrayList;
    import java.util.EnumMap;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.List;
    import java.util.Map;
    import java.util.Set;

    @Service
    @RequiredArgsConstructor
    public class TasteProfileServiceImpl implements TasteProfileService {

        private static final double EXPLICIT_EVIDENCE_WEIGHT = 3.0;
        private static final double QUESTION_EVIDENCE_WEIGHT = 1.0;

        private final UserRepository userRepository;
        private final UserInterestRepository userInterestRepository;
        private final TasteProfileRepository tasteProfileRepository;
        private final TasteQuestionService tasteQuestionService;
        private final TasteSignalResolver tasteSignalResolver;

        @Override
        @Transactional(readOnly = true)
        public TasteProfileResponseDTO getTasteProfile(String email) {

            User user = findUserByEmail(email);

            TasteProfile profile = tasteProfileRepository
                    .findByUserId(user.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Taste profile has not been generated yet"
                            )
                    );

            return mapToResponse(user, profile);
        }

        @Override
        @Transactional(readOnly = true)
        public List<TasteQuestionDTO> getInitialQuestions(String email) {

            User user = findUserByEmail(email);

            List<UserInterest> userInterests =
                    userInterestRepository.findByUser(user);

            if (userInterests.isEmpty()) {
                throw new IllegalArgumentException(
                        "Select at least one interest before getting taste questions"
                );
            }

            List<InterestTag> interests = userInterests
                    .stream()
                    .map(UserInterest::getTag)
                    .toList();

            List<TasteQuestionDTO> questions =
                    tasteQuestionService.generateQuestions(interests);

            if (questions.isEmpty()) {
                throw new IllegalStateException(
                        "No taste questions could be generated for the selected interests"
                );
            }

            return questions;
        }

        @Override
        @Transactional
        public TasteProfileResponseDTO generateTasteProfile(
                String email,
                SubmitTasteAnswersRequestDTO request
        ) {

            if (request == null) {
                throw new IllegalArgumentException(
                        "Taste profile request cannot be null"
                );
            }

            User user = findUserByEmail(email);

            List<UserInterest> userInterests =
                    userInterestRepository.findByUser(user);

            if (userInterests.isEmpty()) {
                throw new IllegalArgumentException(
                        "Select at least one interest before generating taste profile"
                );
            }

            List<InterestTag> interests = userInterests
                    .stream()
                    .map(UserInterest::getTag)
                    .toList();

            List<TasteQuestionDTO> questions =
                    tasteQuestionService.generateQuestions(interests);

            if (questions.isEmpty()) {
                throw new IllegalStateException(
                        "No taste questions could be generated for the selected interests"
                );
            }

            validateAnswers(
                    request.answers(),
                    questions
            );

            Map<TasteDimension, List<Evidence>> evidence =
                    collectDimensionEvidence(
                            userInterests,
                            request.answers()
                    );

            Map<TasteDimension, Double> dimensionScores =
                    calculateDimensionScores(evidence);

            Map<TasteDimension, Double> confidenceScores =
                    calculateConfidence(evidence);

            Map<String, Double> tagScores =
                    createTagScores(userInterests);

            TasteProfile profile =
                    tasteProfileRepository
                            .findByUserId(user.getId())
                            .orElseGet(() ->
                                    TasteProfile.builder()
                                            .user(user)
                                            .version(1)
                                            .initialized(false)
                                            .build()
                            );

            int nextVersion = profile.isInitialized()
                    ? profile.getVersion() + 1
                    : 1;

            profile.setVersion(nextVersion);
            profile.setDimensionScores(dimensionScores);
            profile.setDimensionConfidence(confidenceScores);
            profile.setTagScores(tagScores);
            profile.setInitialized(true);

            TasteProfile savedProfile =
                    tasteProfileRepository.save(profile);

            return mapToResponse(
                    user,
                    savedProfile
            );
        }

        private Map<TasteDimension, List<Evidence>> collectDimensionEvidence(
                List<UserInterest> interests,
                List<TasteAnswerDTO> answers
        ) {

            Map<TasteDimension, List<Evidence>> evidence =
                    new EnumMap<>(TasteDimension.class);

            for (TasteDimension dimension : TasteDimension.values()) {
                evidence.put(
                        dimension,
                        new ArrayList<>()
                );
            }

            addExplicitInterestEvidence(
                    evidence,
                    interests
            );

            addQuestionnaireEvidence(
                    evidence,
                    answers
            );

            return evidence;
        }

        private void addExplicitInterestEvidence(
                Map<TasteDimension, List<Evidence>> evidence,
                List<UserInterest> interests
        ) {

            for (UserInterest interest : interests) {

                TasteDimension dimension =
                        resolvePrimaryDimension(
                                interest.getTag()
                        );

                if (dimension == null) {
                    continue;
                }

                double strength =
                        resolveExplicitInterestStrength(
                                interest.getTag()
                        );

                evidence
                        .get(dimension)
                        .add(
                                new Evidence(
                                        strength,
                                        EXPLICIT_EVIDENCE_WEIGHT
                                )
                        );
            }
        }

        private void addQuestionnaireEvidence(
                Map<TasteDimension, List<Evidence>> evidence,
                List<TasteAnswerDTO> answers
        ) {

            for (TasteAnswerDTO answer : answers) {

                TasteSignal signal =
                        tasteSignalResolver.resolve(
                                answer.questionId(),
                                answer.optionId()
                        );

                if (signal == null) {
                    throw new IllegalStateException(
                            "No taste signal could be resolved for question: "
                                    + answer.questionId()
                    );
                }

                if (signal.dimensions() == null) {
                    continue;
                }

                for (Map.Entry<TasteDimension, Double> entry :
                        signal.dimensions().entrySet()) {

                    if (entry.getKey() == null
                            || entry.getValue() == null) {
                        continue;
                    }

                    double normalizedSignal =
                            normalizeSignal(
                                    entry.getValue()
                            );

                    evidence
                            .get(entry.getKey())
                            .add(
                                    new Evidence(
                                            normalizedSignal,
                                            QUESTION_EVIDENCE_WEIGHT
                                    )
                            );
                }
            }
        }

        private Map<TasteDimension, Double> calculateDimensionScores(
                Map<TasteDimension, List<Evidence>> evidence
        ) {

            Map<TasteDimension, Double> scores =
                    new EnumMap<>(TasteDimension.class);

            for (TasteDimension dimension :
                    TasteDimension.values()) {

                List<Evidence> dimensionEvidence =
                        evidence.getOrDefault(
                                dimension,
                                List.of()
                        );

                if (dimensionEvidence.isEmpty()) {
                    scores.put(
                            dimension,
                            0.50
                    );
                    continue;
                }

                double weightedSum = 0.0;
                double totalWeight = 0.0;

                for (Evidence item : dimensionEvidence) {

                    weightedSum +=
                            item.value() * item.weight();

                    totalWeight +=
                            item.weight();
                }

                double score =
                        totalWeight == 0.0
                                ? 0.50
                                : weightedSum / totalWeight;

                scores.put(
                        dimension,
                        roundScore(
                                clamp(score)
                        )
                );
            }

            return scores;
        }

        private Map<TasteDimension, Double> calculateConfidence(
                Map<TasteDimension, List<Evidence>> evidence
        ) {

            Map<TasteDimension, Double> confidence =
                    new EnumMap<>(TasteDimension.class);

            for (TasteDimension dimension :
                    TasteDimension.values()) {

                List<Evidence> dimensionEvidence =
                        evidence.getOrDefault(
                                dimension,
                                List.of()
                        );

                if (dimensionEvidence.isEmpty()) {
                    confidence.put(
                            dimension,
                            0.0
                    );
                    continue;
                }

                double totalWeight =
                        dimensionEvidence
                                .stream()
                                .mapToDouble(Evidence::weight)
                                .sum();

                double confidenceScore =
                        1.0 - Math.exp(
                                -totalWeight / 4.0
                        );

                confidence.put(
                        dimension,
                        roundScore(
                                clamp(confidenceScore)
                        )
                );
            }

            return confidence;
        }

        private Map<String, Double> createTagScores(
                List<UserInterest> interests
        ) {

            Map<String, Double> scores =
                    new HashMap<>();

            for (UserInterest interest : interests) {

                if (interest.getTag() == null) {
                    continue;
                }

                String tag =
                        interest
                                .getTag()
                                .getValue();

                if (tag == null || tag.isBlank()) {
                    continue;
                }

                scores.put(
                        tag,
                        1.0
                );
            }

            return scores;
        }

        private TasteDimension resolvePrimaryDimension(
                InterestTag tag
        ) {

            if (tag == null) {
                return null;
            }

            return switch (tag) {

                case EDM,
                     INDIE_MUSIC,
                     ROCK,
                     HIP_HOP,
                     SUFI,
                     BOLLYWOOD_MUSIC,
                     MUSIC_FESTIVALS ->
                        TasteDimension.MUSIC;

                case CRICKET,
                     FOOTBALL,
                     KABADDI ->
                        TasteDimension.SPORTS;

                case BOLLYWOOD_FILMS,
                     HOLLYWOOD_FILMS,
                     ANIME ->
                        TasteDimension.MOVIES;

                case STAND_UP_COMEDY,
                     IMPROV ->
                        TasteDimension.COMEDY;

                case THEATRE,
                     MUSICALS ->
                        TasteDimension.THEATRE;

                case PHOTOGRAPHY,
                     POTTERY ->
                        TasteDimension.ARTS;

                case FOOD_FESTIVALS ->
                        TasteDimension.FOOD;

                case CODING,
                     TECH,
                     STARTUPS,
                     AI ->
                        TasteDimension.TECHNOLOGY;
            };
        }

        private double resolveExplicitInterestStrength(
                InterestTag tag
        ) {

            if (tag == null) {
                return 0.0;
            }

            return switch (tag) {

                case EDM,
                     MUSIC_FESTIVALS,
                     CRICKET,
                     AI,
                     CODING,
                     TECH,
                     STARTUPS,
                     BOLLYWOOD_FILMS,
                     HOLLYWOOD_FILMS,
                     STAND_UP_COMEDY,
                     IMPROV,
                     THEATRE,
                     MUSICALS,
                     FOOD_FESTIVALS ->
                        0.85;

                case ANIME,
                     INDIE_MUSIC,
                     ROCK,
                     HIP_HOP,
                     SUFI,
                     BOLLYWOOD_MUSIC,
                     FOOTBALL,
                     KABADDI,
                     PHOTOGRAPHY,
                     POTTERY ->
                        0.80;
            };
        }

        private double normalizeSignal(double signal) {

            if (Double.isNaN(signal)
                    || Double.isInfinite(signal)) {

                throw new IllegalArgumentException(
                        "Taste signal must be a finite number"
                );
            }

            return clamp(
                    0.50 + (signal / 2.0)
            );
        }

        private void validateAnswers(
                List<TasteAnswerDTO> answers,
                List<TasteQuestionDTO> questions
        ) {

            if (answers == null || answers.isEmpty()) {
                throw new IllegalArgumentException(
                        "Taste answers cannot be empty"
                );
            }

            Map<String, TasteQuestionDTO> questionMap =
                    new HashMap<>();

            for (TasteQuestionDTO question : questions) {

                if (question == null
                        || question.id() == null
                        || question.id().isBlank()) {

                    throw new IllegalStateException(
                            "Taste question contains an invalid ID"
                    );
                }

                questionMap.put(
                        question.id(),
                        question
                );
            }

            Set<String> answeredQuestions =
                    new HashSet<>();

            for (TasteAnswerDTO answer : answers) {

                if (answer == null) {
                    throw new IllegalArgumentException(
                            "Taste answer cannot be null"
                    );
                }

                if (answer.questionId() == null
                        || answer.questionId().isBlank()) {

                    throw new IllegalArgumentException(
                            "Question ID is required"
                    );
                }

                if (answer.optionId() == null
                        || answer.optionId().isBlank()) {

                    throw new IllegalArgumentException(
                            "Option ID is required for question: "
                                    + answer.questionId()
                    );
                }

                if (!answeredQuestions.add(
                        answer.questionId()
                )) {

                    throw new IllegalArgumentException(
                            "Question answered more than once: "
                                    + answer.questionId()
                    );
                }

                TasteQuestionDTO question =
                        questionMap.get(
                                answer.questionId()
                        );

                if (question == null) {

                    throw new IllegalArgumentException(
                            "Invalid taste question: "
                                    + answer.questionId()
                    );
                }

                if (question.options() == null
                        || question.options().isEmpty()) {

                    throw new IllegalStateException(
                            "Taste question has no valid options: "
                                    + question.id()
                    );
                }

                boolean validOption =
                        question.options()
                                .stream()
                                .filter(option -> option != null)
                                .anyMatch(option ->
                                        answer.optionId()
                                                .equals(
                                                        option.id()
                                                )
                                );

                if (!validOption) {

                    throw new IllegalArgumentException(
                            "Invalid option '"
                                    + answer.optionId()
                                    + "' for question '"
                                    + answer.questionId()
                                    + "'"
                    );
                }
            }

            if (!answeredQuestions.equals(
                    questionMap.keySet()
            )) {

                Set<String> missingQuestions =
                        new HashSet<>(
                                questionMap.keySet()
                        );

                missingQuestions.removeAll(
                        answeredQuestions
                );

                throw new IllegalArgumentException(
                        "Missing answers for questions: "
                                + missingQuestions
                );
            }
        }

        private TasteProfileResponseDTO mapToResponse(
                User user,
                TasteProfile profile
        ) {

            Map<TasteDimension, Double> dimensions =
                    profile.getDimensionScores() == null
                            ? new EnumMap<>(TasteDimension.class)
                            : profile.getDimensionScores();

            Map<TasteDimension, Double> confidence =
                    profile.getDimensionConfidence() == null
                            ? new EnumMap<>(TasteDimension.class)
                            : profile.getDimensionConfidence();

            Map<String, Double> tags =
                    profile.getTagScores() == null
                            ? new HashMap<>()
                            : profile.getTagScores();

            return new TasteProfileResponseDTO(
                    user.getId(),
                    profile.getVersion(),
                    profile.isInitialized(),
                    roundDimensionScores(dimensions),
                    roundDimensionScores(confidence),
                    roundTagScores(tags)
            );
        }

        private Map<TasteDimension, Double> roundDimensionScores(
                Map<TasteDimension, Double> scores
        ) {

            Map<TasteDimension, Double> result =
                    new EnumMap<>(TasteDimension.class);

            for (TasteDimension dimension :
                    TasteDimension.values()) {

                double score =
                        scores.getOrDefault(
                                dimension,
                                0.50
                        );

                result.put(
                        dimension,
                        roundScore(
                                clamp(score)
                        )
                );
            }

            return result;
        }

        private Map<String, Double> roundTagScores(
                Map<String, Double> scores
        ) {

            Map<String, Double> result =
                    new HashMap<>();

            scores.forEach(
                    (tag, score) -> {

                        if (tag == null
                                || tag.isBlank()
                                || score == null) {
                            return;
                        }

                        result.put(
                                tag,
                                roundScore(
                                        clamp(score)
                                )
                        );
                    }
            );

            return result;
        }

        private double clamp(double value) {

            if (Double.isNaN(value)
                    || Double.isInfinite(value)) {

                return 0.50;
            }

            return Math.min(
                    1.0,
                    Math.max(
                            0.0,
                            value
                    )
            );
        }

        private double roundScore(double value) {

            return Math.round(
                    value * 100.0
            ) / 100.0;
        }

        private User findUserByEmail(String email) {

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "Authenticated user email is required"
                );
            }

            return userRepository
                    .findByEmail(email)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "User not found"
                            )
                    );
        }

        private record Evidence(
                double value,
                double weight
        ) {
        }
    }