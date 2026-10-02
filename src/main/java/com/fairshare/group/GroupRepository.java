package com.fairshare.group;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByJoinCode(String joinCode);
}
