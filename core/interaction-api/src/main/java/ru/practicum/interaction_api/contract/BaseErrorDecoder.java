package ru.practicum.interaction_api.contract;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.practicum.interaction_api.exception.ConflictException;
import ru.practicum.interaction_api.exception.ForbiddenException;
import ru.practicum.interaction_api.exception.NotFoundException;
import ru.practicum.interaction_api.exception.ValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseErrorDecoder implements ErrorDecoder {

    protected final ErrorDecoder defaultDecoder = new Default();

    protected String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try {
            return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        return switch (response.status()) {
            case 400 -> new ValidationException(body);
            case 404 -> new NotFoundException(body);
            case 409 -> new ConflictException(body);
            case 403 -> new ForbiddenException(body);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
