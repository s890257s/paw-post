package tw.pers.allen.backend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 【為什麼不用 @Data？】JPA Entity 使用 @Data 有兩個經典地雷：
// 1. toString() 會印出所有欄位 —— 這裡的 imageData 是整張圖片的 byte[]，一進 log 就是災難。
// 2. equals()/hashCode() 會用到 Lazy 關聯欄位，可能觸發意外查詢或 LazyInitializationException。
// 因此 Entity 只用 @Getter / @Setter。
@Getter
@Setter
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

    // 圖片的 MIME type (例如 image/jpeg、image/webp)，與圖片內容一起保存
    @Column(name = "image_content_type")
    private String imageContentType;

    @Column
    private String description;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
}
