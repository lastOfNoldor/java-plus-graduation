package ru.practicum.interaction_api.dto.event;

import lombok.Builder;
import lombok.Data;
import ru.practicum.interaction_api.enums.EventState;

@Data
@Builder
public class EventInternalDto {
    private Long id;
    private Long initiatorId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}
