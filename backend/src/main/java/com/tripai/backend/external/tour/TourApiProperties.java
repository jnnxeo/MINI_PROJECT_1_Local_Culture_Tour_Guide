package com.tripai.backend.external.tour;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** TourAPI 접속값. 실제 서비스 키는 backend/.env에서만 읽는다. */
@ConfigurationProperties(prefix = "tour-api")
public class TourApiProperties {

    private String baseUrl;
    private String serviceKey;
    private String mobileOs;
    private String mobileApp;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getMobileOs() {
        return mobileOs;
    }

    public void setMobileOs(String mobileOs) {
        this.mobileOs = mobileOs;
    }

    public String getMobileApp() {
        return mobileApp;
    }

    public void setMobileApp(String mobileApp) {
        this.mobileApp = mobileApp;
    }

    public boolean hasServiceKey() {
        return StringUtils.hasText(serviceKey) && !"CHANGE_ME".equals(serviceKey);
    }
}
