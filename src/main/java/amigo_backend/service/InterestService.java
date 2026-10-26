package amigo_backend.service;


import amigo_backend.dto.interset.InterestsResponseDTO;
import amigo_backend.dto.interset.UpdateInterestsRequestDTO;

public interface InterestService {

    InterestsResponseDTO getInterests(String email);

    InterestsResponseDTO updateInterests(
            String email,
            UpdateInterestsRequestDTO request
    );
}
