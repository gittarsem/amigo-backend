package amigo_backend.dto.tasteprofile;

import jakarta.validation.constraints.NotBlank;

public record TasteAnswerDTO(

        @NotBlank(message = "Question ID is required")
        String questionId,

        @NotBlank(message = "Option ID is required")
        String optionId
) {
}