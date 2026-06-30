package com.mycosmetic.application.port.out;

import com.mycosmetic.domain.user.User;

import java.util.Optional;

/**
 * User 영속성 outbound 포트.
 * 구현체: {@code adapter.out.persistence.UserPersistenceAdapter}
 */
public interface UserRepository {

    User save(User user);

    void delete(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
