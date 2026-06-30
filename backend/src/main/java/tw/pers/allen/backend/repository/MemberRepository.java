package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
}
