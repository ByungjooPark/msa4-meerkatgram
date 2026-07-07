package com.msa4meerkatgram.domain.user.repositories;

import com.msa4meerkatgram.domain.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
