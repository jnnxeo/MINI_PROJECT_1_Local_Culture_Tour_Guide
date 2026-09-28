package com.tripai.backend.external.tour;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 수집 전용 실행 명령의 페이지 범위. 기본값은 API 호출 한도를 고려한 한 페이지다. */
@ConfigurationProperties(prefix = "tour-api.sync")
public class TourRestaurantSyncProperties {

    private int pageNo = 1;
    private int numOfRows = 100;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getNumOfRows() {
        return numOfRows;
    }

    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
}
