package com.zalando.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(RagTestConfiguration.class)
class ZeosRagApplicationTests {

  @Test
  void contextLoads() {}
}
