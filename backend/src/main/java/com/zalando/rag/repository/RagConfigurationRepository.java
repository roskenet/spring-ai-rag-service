package com.zalando.rag.repository;

import com.zalando.rag.entity.RagConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RagConfigurationRepository extends JpaRepository<RagConfiguration, Long> {

  Optional<RagConfiguration> findByConfigKey(String configKey);

  Optional<RagConfiguration> findByConfigKeyAndIsActiveTrue(String configKey);

  @Query(
      "SELECT r FROM RagConfiguration r WHERE r.isActive = true ORDER BY r.updatedAt DESC LIMIT 1")
  Optional<RagConfiguration> findActiveConfiguration();

  @Query("SELECT r FROM RagConfiguration r WHERE r.configKey = 'default' AND r.isActive = true")
  Optional<RagConfiguration> findDefaultConfiguration();
}
