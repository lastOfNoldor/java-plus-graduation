package ru.practicum.ewm.stats.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserRecommendationsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос рекомендаций для пользователя userId={} maxResults={}",
                    request.getUserId(), request.getMaxResults());

            recommendationService
                    .getRecommendationsForUser(request.getUserId(), request.getMaxResults())
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}: {}",
                    request.getUserId(), e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос похожих мероприятий eventId={} userId={} maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            recommendationService
                    .getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults())
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при получении похожих мероприятий для eventId={}: {}",
                    request.getEventId(), e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос рейтингов для мероприятий: {}", request.getEventIdList());

            recommendationService
                    .getInteractionsCount(request.getEventIdList())
                    .forEach(responseObserver::onNext);

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при получении рейтингов мероприятий: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
