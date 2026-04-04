package ru.practicum.requst_service.dto.param;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestParamDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long eventId;
}