package kluniversity.com.expense.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kluniversity.com.expense.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}