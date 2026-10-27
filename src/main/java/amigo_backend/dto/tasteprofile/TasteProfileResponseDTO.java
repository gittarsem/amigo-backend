package amigo_backend.dto.tasteprofile;

import amigo_backend.enums.TasteDimension;

import java.util.Map;

public record TasteProfileResponseDTO(
        Long userId,
        int version,
        boolean initialized,
        Map<TasteDimension, Double> dimensions,
        Map<TasteDimension, Double> confidence,
        Map<String, Double> tags
) {
}