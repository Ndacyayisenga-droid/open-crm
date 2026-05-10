package com.openelements.crm.user;

import com.openelements.spring.base.security.user.UserEntity;
import com.openelements.spring.base.security.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * CRM-side extension of the lib {@link UserRepository} that adds a query
 * excluding the well-known SYSTEM-USER row, used by the admin user list.
 */
public interface AdminUserRepository extends UserRepository {

    Page<UserEntity> findBySubNot(String sub, Pageable pageable);
}
