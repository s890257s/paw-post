package tw.pers.allen.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
    
    // 使用 @EntityGraph 預載 member，解決列表查詢時的 N+1 問題
    @EntityGraph(attributePaths = {"member"})
    Page<Post> findAll(Pageable pageable);
}
