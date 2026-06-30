package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
}
