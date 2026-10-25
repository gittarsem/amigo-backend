package amigo_backend.service.impl;


import amigo_backend.dto.profile.ProfileResponseDTO;
import amigo_backend.dto.profile.UpdateProfileRequestDTO;
import amigo_backend.entity.User;
import amigo_backend.entity.UserProfile;

import amigo_backend.repository.UserProfileRepository;
import amigo_backend.repository.UserRepository;

import amigo_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public ProfileResponseDTO getProfile(String email) {

        User user = findUserByEmail(email);

        UserProfile profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> createEmptyProfile(user));

        return mapToResponse(user, profile);
    }

    @Transactional
    public ProfileResponseDTO updateProfile(
            String email,
            UpdateProfileRequestDTO request
    ) {

        User user = findUserByEmail(email);

        if (request.userName() != null &&
                !request.userName().isBlank() &&
                !request.userName()
                        .trim()
                        .equalsIgnoreCase(user.getUserName())) {

            if (userRepository.existsByUserName(
                    request.userName().trim()
            )) {
                throw new IllegalArgumentException(
                        "Username is already taken"
                );
            }
        }

        if (request.firstName() != null &&
                !request.firstName().isBlank()) {

            user.setFirstName(
                    request.firstName().trim()
            );
        }

        if (request.lastName() != null) {

            user.setLastName(
                    request.lastName().trim()
            );
        }

        if (request.userName() != null &&
                !request.userName().isBlank()) {

            user.setUserName(
                    request.userName().trim()
            );
        }

        UserProfile profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> createEmptyProfile(user));

        if (request.bio() != null) {

            profile.setBio(
                    request.bio().trim()
            );
        }

        if (request.dateOfBirth() != null) {

            profile.setDateOfBirth(
                    request.dateOfBirth()
            );
        }

        if (request.gender() != null) {

            profile.setGender(
                    request.gender()
            );
        }

        if (request.city() != null) {

            profile.setCity(
                    request.city().trim()
            );
        }

        userRepository.save(user);
        userProfileRepository.save(profile);

        return mapToResponse(user, profile);
    }

    private UserProfile createEmptyProfile(User user) {

        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();

        return userProfileRepository.save(profile);
    }

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    private ProfileResponseDTO mapToResponse(
            User user,
            UserProfile profile
    ) {

        return new ProfileResponseDTO(
                profile.getId(),
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                profile.getBio(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getCity(),
                profile.getProfileImageUrl(),
                user.isEmailVerified(),
                user.isFaceVerified(),
                user.isLivenessVerified(),
                user.getRole()
        );
    }
}
