package tw.pers.allen.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeCountDto {
    private Integer postId;
    private Long likeCount;
}
