package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
}
