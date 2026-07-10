package com.sophia.insightflow.analytics.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public class ClasspathAnalyticsDataRepository implements AnalyticsDataRepository {

  private final ObjectMapper objectMapper;
  private AnalyticsDataset cachedDataset;

  public ClasspathAnalyticsDataRepository(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public synchronized AnalyticsDataset getDataset() {
    if (cachedDataset == null) {
      cachedDataset = loadDataset();
    }
    return cachedDataset;
  }

  private AnalyticsDataset loadDataset() {
    try (InputStream stream = new ClassPathResource("analytics-facts.json").getInputStream()) {
      return objectMapper.readValue(stream, AnalyticsDataset.class);
    } catch (IOException exception) {
      throw new UncheckedIOException("Unable to read analytics facts", exception);
    }
  }
}
