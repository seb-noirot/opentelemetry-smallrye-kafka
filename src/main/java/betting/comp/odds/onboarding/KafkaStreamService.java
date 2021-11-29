package betting.comp.odds.onboarding;

import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaStreamService {

  public static final Logger LOGGER = Logger.getLogger(KafkaStreamService.class);

  @Outgoing("demo-kafka-stream-string-out")
  public Multi<Message<String>> produceData() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
        .map(tick -> Message.of("Stream Hello " + tick));
  }

  @Produces
  public Topology buildTopology() {

    var builder = new StreamsBuilder();
    builder.stream(
            "stream-string-out",
            Consumed.with(Serdes.String(), Serdes.String()))
        .mapValues(object -> object.toUpperCase(Locale.ROOT))
        .to("stream-uppercase-out",
            Produced.with(Serdes.String(), Serdes.String()));

    builder.stream(
            "stream-uppercase-out",
            Consumed.with(Serdes.String(), Serdes.String()))
        .mapValues(object -> {
          var tempArray = object.toCharArray();
          Arrays.sort(tempArray);
          return new String(tempArray);
        })
        .to("stream-sort-out",
            Produced.with(Serdes.String(), Serdes.String()));

    builder.stream(
            "stream-sort-out",
            Consumed.with(Serdes.String(), Serdes.String()))
        .process(() -> new Processor<String, String>() {
          @Override
          public void init(ProcessorContext processorContext) {

          }

          @Override
          public void process(String s, String s2) {
            LOGGER.infov("Message received from stream {0}", s2);
          }

          @Override
          public void close() {

          }
        });
    return builder.build();
  }

  @Produces
  public KafkaClientSupplier kafkaClientSupplier() {
    return new KafkaStreamClientSupplier();
  }

}
