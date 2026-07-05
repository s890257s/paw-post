package tw.pers.allen.backend.model.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

// 後台列表專用的貼文「摘要」，刻意不含圖片欄位。
// 【教學點】API 應該只回傳「剛好夠用」的資料——
// 後台表格只顯示文字，若沿用含圖片的 PostResponseDto，
// 一頁 20 筆就要傳輸 20 張 Base64 圖片（動輒數十 MB），而且全部用不到。
@Data
@NoArgsConstructor
public class AdminPostSummaryDto {
    private Integer id;
    private String username;
    private String description;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Boolean isHidden;
}
