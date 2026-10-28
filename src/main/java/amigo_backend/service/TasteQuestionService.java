package amigo_backend.service;


import amigo_backend.dto.tasteprofile.TasteQuestionDTO;
import amigo_backend.enums.InterestTag;


import java.util.List;

public interface TasteQuestionService {

    List<TasteQuestionDTO> generateQuestions(
            List<InterestTag> interests
    );
}
