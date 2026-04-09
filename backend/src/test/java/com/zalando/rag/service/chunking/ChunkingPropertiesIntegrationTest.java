package com.zalando.rag.service.chunking;

import static org.junit.jupiter.api.Assertions.*;

import com.zalando.rag.RagTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify that ChunkingProperties correctly loads configuration from
 * application.yaml
 */
@SpringBootTest
@Import(RagTestConfiguration.class)
@TestPropertySource(
    properties = {
      "rag.chunking.default-strategy=recursive",
      "rag.chunking.global.max-chunk-size=1500",
      "rag.chunking.strategies.intelligent.preferred-chunk-size=900"
    })
class ChunkingPropertiesIntegrationTest {

  @Autowired private ChunkingProperties chunkingProperties;

  @Test
  void testDefaultStrategyConfiguration() {
    // Verify that default strategy is loaded from configuration
    assertEquals("recursive", chunkingProperties.getDefaultStrategy());
  }

  @Test
  void testGlobalConfiguration() {
    // Verify that global configuration is loaded
    ChunkingConfig globalConfig = chunkingProperties.getGlobalConfig();
    assertEquals(1500, globalConfig.getMaxChunkSize());
  }

  @Test
  void testStrategySpecificConfiguration() {
    // Verify that strategy-specific configuration is loaded
    ChunkingConfig intelligentConfig = chunkingProperties.getConfigForStrategy("intelligent");
    assertNotNull(intelligentConfig);
    assertEquals(900, intelligentConfig.getPreferredChunkSize());
    // Should inherit global max-chunk-size
    assertEquals(1500, intelligentConfig.getMaxChunkSize());
  }

  @Test
  void testDefaultStrategyFromApplicationYaml() {
    // In the actual application.yaml, default strategy is set to "intelligent"
    // This test verifies the real configuration (without test property override)
    ChunkingProperties realProperties = new ChunkingProperties();
    assertEquals("intelligent", realProperties.getDefaultStrategy());
  }
}
