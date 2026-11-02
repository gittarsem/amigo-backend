package amigo_backend.event.dto;

import amigo_backend.enums.InterestCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Used for both create (POST) and full update (PUT). */
public record EventRequestDTO(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotNull(message = "Category is required")
        InterestCategory category,

        @Size(max = 15, message = "At most 15 tags are allowed")
        List<String> tags,

        @NotBlank(message = "Venue name is required")
        @Size(max = 200, message = "Venue name must be at most 200 characters")
        String venueName,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,

        @NotNull(message = "Start time is required")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        LocalDateTime endTime,

        @PositiveOrZero(message = "Budget cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Budget must have at most 8 digits and 2 decimals")
        BigDecimal budget,

        @Size(max = 150, message = "Organizer name must be at most 150 characters")
        String organizerName,

        @Size(max = 1000, message = "Official URL must be at most 1000 characters")
        String officialUrl,

        @Size(max = 1000, message = "Ticket URL must be at most 1000 characters")
        String ticketUrl,

        @Size(max = 1000, message = "Poster URL must be at most 1000 characters")
        String posterUrl,

        @Size(max = 2000, message = "Supporting information must be at most 2000 characters")
        String supportingInformation

) {
}