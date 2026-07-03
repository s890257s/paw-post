package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tw.pers.allen.backend.model.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    
    /**
     * 計算特定貼文的按讚總數
     *
     * @param postId 貼文 ID
     * @return 該貼文的按讚數量
     */
    int countByPostId(Integer postId);

    /**
     * 檢查特定會員是否已經對特定貼文按過讚
     *
     * @param memberId 會員 ID
     * @param postId   貼文 ID
     * @return 如果已經按過讚回傳 true，否則回傳 false
     */
    boolean existsByMemberIdAndPostId(Integer memberId, Integer postId);

    /**
     * 根據會員 ID 和貼文 ID 尋找按讚記錄
     *
     * @param memberId 會員 ID
     * @param postId   貼文 ID
     * @return 回傳符合條件的按讚記錄，若無則回傳 null
     */
    PostLike findByMemberIdAndPostId(Integer memberId, Integer postId);
}
