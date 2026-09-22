package com.legalmetrology.auth.repository;

import com.legalmetrology.auth.entity.Role;
import com.legalmetrology.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
