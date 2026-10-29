package amigo_backend.dto.tasteprofile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitTasteAnswersRequestDTO(

        @NotEmpty(message = "Answers cannot be empty")
        @Valid
        List<TasteAnswerDTO> answers

) {
}