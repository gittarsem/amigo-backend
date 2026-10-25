package amigo_backend.service;


import amigo_backend.dto.profile.ProfileResponseDTO;
import amigo_backend.dto.profile.UpdateProfileRequestDTO;

public interface ProfileService {

    ProfileResponseDTO getProfile(String email);

    ProfileResponseDTO updateProfile(
            String email,
            UpdateProfileRequestDTO request
    );
}
