package betting.comp.odds.onboarding;

import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;

public class KafkaStreamClientSupplier extends DefaultKafkaClientSupplier {

  @Override
  public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
    return new TracingKafkaProducer<>(super.getProducer(config));
  }

  @Override
  public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
    return new TracingKafkaConsumer<>(super.getConsumer(config));
  }
}
