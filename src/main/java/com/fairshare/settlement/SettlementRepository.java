package com.fairshare.settlement;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    Optional<Settlement> findByIdAndGroupId(Long id, Long groupId);
}
