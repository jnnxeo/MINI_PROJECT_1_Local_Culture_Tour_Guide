package com.tripai.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripai.backend.service.FavoriteEventService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;



@RestController 
@RequestMapping("/api/favorites/events")
@RequiredArgsConstructor 
public class FavoriteEventController {

    private final FavoriteEventService favoriteEventService;

   @PostMapping("/{eventContentId}")
   public ResponseEntity<Void> addFavorite(@PathVariable String eventContentId) {
      
        Long userId = 1L; // TODO: 로그인 구현 후 인증 사용자로 변경예정
        
        favoriteEventService.addFavorite(userId, eventContentId);

       
       return ResponseEntity.ok().build(); // TODO: 팀 응답 포맷(ApiResponse) 확정되면 교체
   }
   

   @DeleteMapping ("/{eventContentId}")
   public ResponseEntity<Void> deleteFavorite(@PathVariable String eventContentId) {
      
        Long userId = 1L; // TODO: 로그인 구현 후 인증 사용자로 변경예정
        
        favoriteEventService.deleteFavorite(userId, eventContentId);

       
       return ResponseEntity.noContent().build(); // TODO: 팀 응답 포맷·상태코드(204 vs 200) 확정 후 교체
   }
    
}
