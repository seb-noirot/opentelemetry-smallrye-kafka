package betting.comp.odds.onboarding;

import static io.smallrye.reactive.messaging.kafka.KafkaConnector.TRACER;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.smallrye.reactive.messaging.kafka.tracing.HeaderExtractAdapter;
import io.smallrye.reactive.messaging.kafka.tracing.HeaderInjectAdapter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;

public class TracingKafkaProducer<K, V> implements Producer<K, V> {

  private Producer<K, V> producer;

  public TracingKafkaProducer(Producer<K, V> producer) {
    this.producer = producer;
  }

  @Override
  public void initTransactions() {
    producer.initTransactions();
  }

  @Override
  public void beginTransaction() throws ProducerFencedException {
    producer.beginTransaction();
  }

  @Override
  public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
      String consumerGroupId)
      throws ProducerFencedException {
    producer.sendOffsetsToTransaction(offsets, consumerGroupId);
  }

  @Override
  public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
      ConsumerGroupMetadata groupMetadata) throws ProducerFencedException {
    producer.sendOffsetsToTransaction(offsets, groupMetadata);
  }

  @Override
  public void commitTransaction() throws ProducerFencedException {
    producer.commitTransaction();
  }

  @Override
  public void abortTransaction() throws ProducerFencedException {
    producer.abortTransaction();
  }

  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
    return send(record, null);
  }


  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
    createOutgoingTrace(record);
    return producer.send(record, callback);
  }

  private void createOutgoingTrace(ProducerRecord<K, V> message) {

    TracingMetadata tracingMetadata =TracingMetadata.empty();

    if (message.headers() != null) {
      // Read tracing headers
      Context context = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
          .extract(Context.current(), message.headers(), HeaderExtractAdapter.GETTER);
      tracingMetadata = TracingMetadata.withCurrent(context);
    }

      final SpanBuilder spanBuilder = TRACER.spanBuilder(message.topic() + " send")
          .setSpanKind(SpanKind.PRODUCER);

      if (tracingMetadata.getCurrentContext() != null) {
        // Handle possible parent span
        final Context parentSpanContext = tracingMetadata.getCurrentContext();
        if (parentSpanContext != null) {
          spanBuilder.setParent(parentSpanContext);
        } else {
          spanBuilder.setNoParent();
        }
      } else {
        spanBuilder.setNoParent();
      }

      final Span span = spanBuilder.startSpan();
      Scope scope = span.makeCurrent();

      // Set Span attributes
      if (message.partition() != null) {
        span.setAttribute(SemanticAttributes.MESSAGING_KAFKA_PARTITION, message.partition());
      }
      span.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "kafka");
      span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION, message.topic());
      span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic");

      // Set span onto headers
      GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
          .inject(Context.current(), message.headers(), HeaderInjectAdapter.SETTER);

      span.end();
      scope.close();
  }

  @Override
  public void flush() {
    producer.flush();
  }

  @Override
  public List<PartitionInfo> partitionsFor(String topic) {
    return producer.partitionsFor(topic);
  }

  @Override
  public Map<MetricName, ? extends Metric> metrics() {
    return producer.metrics();
  }

  @Override
  public void close() {
    producer.close();
  }

  @Override
  public void close(Duration duration) {
    producer.close(duration);
  }

  @Override
  public void close(long timeout, TimeUnit timeUnit) {
    producer.close(timeout, timeUnit);
  }

}
