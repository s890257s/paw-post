package tw.pers.allen.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Member member;

    @Lob
    @Column
    private byte[] imageData;

    @Column
    private String description;

    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
}
