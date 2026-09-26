package com.tripai.backend.external.tour;

/** TourAPI 호출·응답 계약이 깨졌을 때 수집 작업이 원인을 남기고 중단되도록 하는 예외다. */
public class TourApiException extends RuntimeException {

    public TourApiException(String message) {
        super(message);
    }

    public TourApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
