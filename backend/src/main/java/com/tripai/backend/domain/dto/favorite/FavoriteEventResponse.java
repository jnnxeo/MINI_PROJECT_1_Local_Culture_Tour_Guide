package com.tripai.backend.domain.dto.favorite;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class FavoriteEventResponse {
    
    private String eventContentId;
    private String eventName;
    private String eventType;
    private String districtName;
    private LocalDate eventStartDate;
    private String imageUrl;
}
