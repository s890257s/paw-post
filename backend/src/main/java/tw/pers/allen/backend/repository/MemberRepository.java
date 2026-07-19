package tw.pers.allen.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.pers.allen.backend.model.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    // 方法名稱衍生查詢——Spring Data JPA 解析方法名自動產生 SQL,不需要寫任何實作
    Member findByUsername(String username);
}
