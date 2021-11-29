package betting.comp.odds.onboarding;

import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.TracingMetadata;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaTracingService {

  public static final Logger LOGGER = Logger.getLogger(KafkaTracingService.class);

  @Outgoing("demo-kafka-tracing-string-out")
  public Multi<Message<String>> produceData() {
   return Multi.createFrom().ticks().every(Duration.ofSeconds(1)).map(tick -> Message.of("SmallRye Hello " + tick));
  }

  @Incoming("demo-kafka-tracing-uppercase-in")
  @Outgoing("demo-kafka-tracing-uppercase-out")
  public Message<String> toUpperCase(ConsumerRecord<String, String> message) {
    return Message.of(message.value().toUpperCase(Locale.ROOT)).addMetadata(TracingMetadata.withCurrent(
        Context.current()));
  }

  @Incoming("demo-kafka-tracing-sort-in")
  @Outgoing("demo-kafka-tracing-sort-out")
  public Message<String> sortString(ConsumerRecord<String, String> message) {
    var tempArray = message.value().toCharArray();
    Arrays.sort(tempArray);
    return Message.of(new String(tempArray)).addMetadata(TracingMetadata.withCurrent(
        Context.current()));
  }

  @Incoming("demo-kafka-tracing-display-in")
  public void displayMessage(ConsumerRecord<String, String> message) {
    LOGGER.infov("Message received {0}", message.value());
  }
}
