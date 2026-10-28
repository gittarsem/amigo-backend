package amigo_backend.dto.tasteprofile;


import java.util.List;

public record TasteQuestionDTO(
        String id,
        String question,
        List<TasteQuestionOptionDTO> options
) {}
