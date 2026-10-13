package kluniversity.com.expense.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kluniversity.com.expense.entity.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}