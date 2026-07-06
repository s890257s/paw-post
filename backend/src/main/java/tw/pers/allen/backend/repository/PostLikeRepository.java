package tw.pers.allen.backend.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.pers.allen.backend.model.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

    /**
     * Projection 介面，用來接收 GROUP BY 回傳的結果
     * 
     */
    interface PostLikeCount {
        Integer getPostId();

        Integer getLikeCount();
    }

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

    /**
     * 批次查詢多篇貼文的按讚總數
     *
     * @param postIds 貼文 ID 列表
     * @return 貼文 ID 與按讚數量的映射列表
     */
    @Query("SELECT l.post.id AS postId, COUNT(l) AS likeCount FROM PostLike l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<PostLikeCount> countByPostIdIn(List<Integer> postIds);

    /**
     * 批次檢查特定會員在指定貼文列表中按過哪些讚
     *
     * @param memberId 會員 ID
     * @param postIds  貼文 ID 列表
     * @return 該會員按過讚的貼文 ID 集合
     */
    @Query("SELECT l.post.id FROM PostLike l WHERE l.member.id = :memberId AND l.post.id IN :postIds")
    Set<Integer> findLikedPostIdsByMemberAndPosts(@Param("memberId") Integer memberId,
            @Param("postIds") List<Integer> postIds);
}
