package kluniversity.com.expense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kluniversity.com.expense.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCreatedById(Long userId);
}