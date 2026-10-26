package amigo_backend.dto.interset;

import java.util.List;

public record InterestsResponseDTO(
        List<InterestDTO> interests
) {
}
