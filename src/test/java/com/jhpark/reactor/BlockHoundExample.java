package com.jhpark.reactor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;

public class BlockHoundExample {
  @BeforeAll
  static void setUp() {
    BlockHound.install(builder ->
        builder.allowBlockingCallsInside("org.slf4j.impl.SimpleLogger", "write")); //허용할 apis
  }
}
