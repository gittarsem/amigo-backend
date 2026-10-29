package amigo_backend.service;

import amigo_backend.dto.tasteprofile.SubmitTasteAnswersRequestDTO;
import amigo_backend.dto.tasteprofile.TasteProfileResponseDTO;
import amigo_backend.dto.tasteprofile.TasteQuestionDTO;

import java.util.List;

public interface TasteProfileService {

    TasteProfileResponseDTO getTasteProfile(String email);

    List<TasteQuestionDTO> getInitialQuestions(String email);

    TasteProfileResponseDTO generateTasteProfile(
            String email,
            SubmitTasteAnswersRequestDTO request
    );
}