package amigo_backend.entity;

import amigo_backend.enums.InterestCategory;
import amigo_backend.enums.InterestTag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_interests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_interest",
                        columnNames = {"user_id", "interest_tag"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "interest_tag",
            nullable = false,
            length = 50
    )
    private InterestTag tag;
}