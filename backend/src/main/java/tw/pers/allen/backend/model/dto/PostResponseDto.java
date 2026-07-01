package tw.pers.allen.backend.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// 回傳給前端的貼文資料格式
@Data
@NoArgsConstructor
public class PostResponseDto {
    private Integer id;
    private Integer memberId;
    private String username;
    private String imageBase64;
    private String description;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Boolean isLiked;
}
