package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.Post;

// 本專案用到的 findById、findAll 分頁等查詢
// JpaRepository 都已內建，因此不需要自己宣告任何方法。
public interface PostRepository extends JpaRepository<Post, Integer> {
}
