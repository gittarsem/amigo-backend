package amigo_backend.dto.tasteprofile;

import amigo_backend.enums.TasteDimension;

import java.util.Map;

public record TasteSignal(
        Map<TasteDimension, Double> dimensions,
        Map<String, Double> tags
) {
}
