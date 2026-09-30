package com.weng.licensehub.user.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weng.licensehub.user.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
 @Query("""
            select userAccount
            from UserAccount userAccount
            where lower(userAccount.email) = lower(:email)
            """)
    Optional<UserAccount> findByEmailIgnoreCase(

        @Param("email") String email
    );

}
