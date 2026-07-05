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
//
// 【欄位命名對映】Spring Boot 預設的命名策略會自動把 camelCase 轉成 snake_case：
//   Post → post、createdAt → created_at、isHidden → is_hidden、
//   @ManyToOne 關聯欄位 member → member_id。
// 因此不需要逐欄寫 @Table(name=...) / @Column(name=...)，
// 只有名稱與慣例推導「不一致」時才需要顯式指定。
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Lob
    private byte[] imageData;

    // 圖片的 MIME type (例如 image/jpeg、image/webp)，與圖片內容一起保存
    private String imageContentType;

    private String description;

    private Boolean isHidden = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
