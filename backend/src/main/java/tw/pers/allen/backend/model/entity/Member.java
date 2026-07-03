package tw.pers.allen.backend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// JPA Entity 不使用 @Data，原因詳見 Post.java 的說明
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String username;

    @Column
    private String password;

    @Column
    private String role;

    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
}
