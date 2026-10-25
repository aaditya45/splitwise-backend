package com.splitwise.event;

import com.splitwise.service.NotificationClient;
import com.splitwise.service.NotificationClient.SendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseNotificationListener {

    private static final String TYPE_EXPENSE_ADDED = "EXPENSE_ADDED";
    private static final int MAX_TITLE_LENGTH = 100;

    private final NotificationClient notificationClient;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        try {
            List<SendRequest> requests = event.recipients().stream()
                    // The creator already knows about it. Remove this filter to notify them too.
                    .filter(member -> !Objects.equals(member.userId(), event.actorUserId()))
                    .map(member -> toRequest(event, member))
                    .toList();
            System.out.println("-------------> Sending notifications for expense " + event.expenseId() + " to " + requests.size() + " recipients");
            notificationClient.sendBulk(requests);
        } catch (Exception e) {
            log.error("Could not send notifications for expense {}", event.expenseId(), e);
        }
    }

    private SendRequest toRequest(ExpenseCreatedEvent event, ExpenseCreatedEvent.Recipient member) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("actorName", event.actorName());
        params.put("groupName", event.groupName());
        params.put("amount", event.amount().toPlainString());
        if (event.description() != null && !event.description().isBlank()) {
            params.put("expenseTitle", abbreviate(event.description().trim()));
        }

        return new SendRequest(
                member.email(),          // your JWT "sub" is the email, so email is the notification user key
                TYPE_EXPENSE_ADDED,
                null,                    // default channels for this type: in-app + push
                params,
                null,                    // title from the service's template
                null,                    // body from the service's template
                "GROUP",                 // deep link: <FRONTEND_URL>/groups/<groupId>
                String.valueOf(event.groupId()),
                "expense-" + event.expenseId() + "-added-" + member.userId());
    }

    private static String abbreviate(String text) {
        return text.length() <= MAX_TITLE_LENGTH ? text : text.substring(0, MAX_TITLE_LENGTH - 1) + "…";
    }
}