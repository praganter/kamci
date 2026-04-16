package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtEventAsyncPublisher {

    private final DebtEventProducer debtEventProducer;

    @Async("taskExecutor")
    public void publishDebtAddedEvent(
            final DebtDocument debt, final CategoryDocument category, final String creatorId,
            final String locale) {
        final List<String> memberIds = category.getMembers().stream()
                .map(MemberEmbedded::getUserId)
                .filter(uid -> !uid.equals(creatorId))
                .toList();

        if (memberIds.isEmpty()) {
            return;
        }

        debtEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.DEBT_ADDED_ROUTING_KEY,
                DebtAddedEvent.builder()
                        .debtId(debt.getId())
                        .ownerId(debt.getOwnerId())
                        .debtName(debt.getName())
                        .amount(debt.getAmount())
                        .memberUserIds(memberIds)
                        .locale(locale)
                        .build()
        );
        log.info("Debt added event produced for {} member(s)", memberIds.size());
    }
}
