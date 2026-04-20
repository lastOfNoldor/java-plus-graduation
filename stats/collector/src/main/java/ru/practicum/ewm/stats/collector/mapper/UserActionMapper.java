package ru.practicum.ewm.stats.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionAvro map(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(mapAction(proto.getActionType()))
                .setTimestamp(Instant.ofEpochSecond(
                        proto.getTimestamp().getSeconds(),
                        proto.getTimestamp().getNanos()
                ))
                .build();
    }

    private ActionTypeAvro mapAction(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action: " + proto);
        };
    }
}
