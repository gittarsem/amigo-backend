package amigo_backend.dto.interset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateInterestsRequestDTO(

        @NotNull(message = "Interests cannot be null")
        @Size(
                min = 1,
                max = 20,
                message = "Select between 1 and 20 interests"
        )
        @Valid
        List<InterestDTO> interests

) {
}
