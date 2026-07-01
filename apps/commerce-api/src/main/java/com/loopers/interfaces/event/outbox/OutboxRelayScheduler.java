package com.loopers.interfaces.event.outbox;

import com.loopers.domain.outbox.OutboxModel;
import com.loopers.domain.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<OutboxModel> pending = outboxRepository.findPendingBatch(BATCH_SIZE);
        for (OutboxModel outbox : pending) {
            try {
                kafkaTemplate.send(outbox.getTopic(), outbox.getPartitionKey(), outbox.getPayload()).get();
                outbox.markPublished();
                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("[OutboxRelay] 발행 실패: outboxId={}, topic={}", outbox.getId(), outbox.getTopic(), e);
            }
        }
    }
}
