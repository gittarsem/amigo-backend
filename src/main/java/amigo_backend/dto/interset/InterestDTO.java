package amigo_backend.dto.interset;

import amigo_backend.enums.InterestCategory;
import amigo_backend.enums.InterestTag;
import jakarta.validation.constraints.NotNull;

public record InterestDTO(

        @NotNull(message = "Interest category is required")
        InterestCategory category,

        @NotNull(message = "Interest tag is required")
        InterestTag tag
) {
}