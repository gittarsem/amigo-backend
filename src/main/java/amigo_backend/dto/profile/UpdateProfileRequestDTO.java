package amigo_backend.dto.profile;

import amigo_backend.entity.UserProfile;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequestDTO(

        @Size(
                max = 100,
                message = "First name must be at most 100 characters"
        )
        String firstName,

        @Size(
                max = 100,
                message = "Last name must be at most 100 characters"
        )
        String lastName,

        @Size(
                max = 100,
                message = "Username must be at most 100 characters"
        )
        String userName,

        @Size(
                max = 500,
                message = "Bio must be at most 500 characters"
        )
        String bio,

        LocalDate dateOfBirth,

        UserProfile.Gender gender,

        @Size(
                max = 100,
                message = "City must be at most 100 characters"
        )
        String city

) {
}
