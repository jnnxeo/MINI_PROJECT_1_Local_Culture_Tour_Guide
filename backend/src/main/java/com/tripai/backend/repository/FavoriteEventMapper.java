package com.tripai.backend.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.favorite.FavoriteEventResponse;

@Mapper 
public interface FavoriteEventMapper {
    
    // 관심 행사 저장
    public int insertFavorite(
        @Param("userId") Long userId,
        @Param("eventContentId") String eventContentId
    );

    // 관심 행사 삭제
    public int deleteFavorite(
        @Param("userId") Long userId,
        @Param("eventContentId") String eventContentId
    );

    // 관심 행사 중복 확인
    public boolean existsFavorite(
        @Param("userId") Long userId,
        @Param("eventContentId") String eventContentId
    );

    // 관심 행사 목록 조희
    public List<FavoriteEventResponse> selectFavorite(@Param("userId") Long userId);

}
