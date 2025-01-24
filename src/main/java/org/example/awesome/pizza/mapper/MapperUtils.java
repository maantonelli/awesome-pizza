package org.example.awesome.pizza.mapper;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@UtilityClass
public class MapperUtils {
  OffsetDateTime toOffsetDateTime(final Instant input) {
    return OffsetDateTime.ofInstant(input, ZoneId.systemDefault());
  }
}
