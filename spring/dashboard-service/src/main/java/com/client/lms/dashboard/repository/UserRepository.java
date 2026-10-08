package com.client.lms.dashboard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    List<User> findByKeycloakUserIdIn(Collection<String> keycloakUserIds);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentCode(String studentCode);

    long countByRoleAndStatus(Role role, UserStatus status);

    long countByStatus(UserStatus status);

    @Query("""
            select u from User u
            where u.status <> com.client.lms.dashboard.entity.UserStatus.DELETED
              and (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:q is null
                   or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.studentCode, '')) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("q") String q,
                      @Param("role") Role role,
                      @Param("status") UserStatus status,
                      Pageable pageable);
}
