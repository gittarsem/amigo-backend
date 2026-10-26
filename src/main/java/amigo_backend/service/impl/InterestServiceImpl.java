package amigo_backend.service.impl;


import amigo_backend.dto.interset.InterestDTO;
import amigo_backend.dto.interset.InterestsResponseDTO;
import amigo_backend.dto.interset.UpdateInterestsRequestDTO;
import amigo_backend.entity.User;
import amigo_backend.entity.UserInterest;
import amigo_backend.enums.InterestTag;

import amigo_backend.repository.UserInterestRepository;
import amigo_backend.repository.UserRepository;

import amigo_backend.service.InterestService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;

    @Override
    @Transactional(readOnly = true)
    public InterestsResponseDTO getInterests(String email) {

        User user = findUserByEmail(email);

        List<InterestDTO> interests = userInterestRepository
                .findByUser(user)
                .stream()
                .map(userInterest -> new InterestDTO(
                        userInterest.getCategory(),
                        userInterest.getTag()
                ))
                .toList();

        return new InterestsResponseDTO(interests);
    }

    @Override
    @Transactional
    public InterestsResponseDTO updateInterests(
            String email,
            UpdateInterestsRequestDTO request
    ) {

        User user = findUserByEmail(email);

        validateInterests(request);

        userInterestRepository.deleteByUser(user);

        List<UserInterest> userInterests = request.interests()
                .stream()
                .map(interest -> UserInterest.builder()
                        .user(user)
                        .category(interest.category())
                        .tag(interest.tag())
                        .build()
                )
                .toList();

        userInterestRepository.saveAll(userInterests);

        return getInterests(email);
    }

    private void validateInterests(
            UpdateInterestsRequestDTO request
    ) {

        for (InterestDTO interest : request.interests()) {

            if (interest.tag() == null) {
                throw new IllegalArgumentException(
                        "Interest tag cannot be null"
                );
            }

            if (interest.category() == null) {
                throw new IllegalArgumentException(
                        "Interest category cannot be null"
                );
            }

            if (interest.tag().getCategory()
                    != interest.category()) {

                throw new IllegalArgumentException(
                        "Interest category does not match interest tag: "
                                + interest.tag()
                );
            }
        }
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }
}
