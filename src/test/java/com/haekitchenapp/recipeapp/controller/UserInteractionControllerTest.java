package com.haekitchenapp.recipeapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haekitchenapp.recipeapp.entity.RecipeLikes;
import com.haekitchenapp.recipeapp.entity.composite.RecipeLikesId;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeUserLikeDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.JwtTokenService;
import com.haekitchenapp.recipeapp.service.UserInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserInteractionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(Advice.class)
class UserInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserInteractionService userInteractionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void toggleRecipeLike_shouldRetrieveUserIdAndForwardToService() throws Exception {
        Long userId = 42L;
        Long recipeId = 100L;
        RecipeUserLikeDto requestDto = new RecipeUserLikeDto();
        requestDto.setRecipeId(recipeId);
        ApiResponse<Boolean> responseBody = ApiResponse.success("Toggle successful", true);

        when(jwtTokenService.getUserIdFromRequest(any(HttpServletRequest.class))).thenReturn(userId);
        when(userInteractionService.toggleRecipeLike(recipeId, userId)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/user/interaction/like-recipe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Toggle successful"))
                .andExpect(jsonPath("$.data").value(true));

        verify(jwtTokenService).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).toggleRecipeLike(recipeId, userId);
    }

    @Test
    void getRecipeLikesByUserId_shouldRetrieveUserIdAndForwardToService() throws Exception {
        Long userId = 55L;
        RecipeLikes like = new RecipeLikes();
        like.setId(new RecipeLikesId(userId, 200L));
        like.setLikedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        List<RecipeLikes> likes = List.of(like);
        ApiResponse<List<RecipeLikes>> responseBody = ApiResponse.success("Fetched likes", likes);

        when(jwtTokenService.getUserIdFromRequest(any(HttpServletRequest.class))).thenReturn(userId);
        when(userInteractionService.getRecipeLikesByUserId(userId)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/user/interaction/like-recipe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fetched likes"))
                .andExpect(jsonPath("$.data[0].id.userId").value(userId))
                .andExpect(jsonPath("$.data[0].id.recipeId").value(200L))
                .andExpect(jsonPath("$.data[0].likedAt").value("2024-01-01T12:00:00"));

        verify(jwtTokenService).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).getRecipeLikesByUserId(userId);
    }

    @Test
    void getRecipeLikesByUserIdForRecipes_shouldRetrieveUserIdAndForwardToService() throws Exception {
        Long userId = 77L;
        List<RecipeTitleDto> recipeTitles = List.of(new RecipeTitleDto(1L, "Recipe One"));
        ApiResponse<List<RecipeTitleDto>> responseBody = ApiResponse.success("Fetched recipes", recipeTitles);

        when(jwtTokenService.getUserIdFromRequest(any(HttpServletRequest.class))).thenReturn(userId);
        when(userInteractionService.getRecipeTitleDtosByUserId(userId)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/user/interaction/like-recipe/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fetched recipes"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("Recipe One"));

        verify(jwtTokenService).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).getRecipeTitleDtosByUserId(userId);
    }

    @Test
    void getRecipeLikesByRecipeId_shouldForwardToService() throws Exception {
        Long recipeId = 300L;
        RecipeLikes like = new RecipeLikes();
        like.setId(new RecipeLikesId(15L, recipeId));
        like.setLikedAt(LocalDateTime.of(2024, 2, 2, 8, 30));
        List<RecipeLikes> likes = List.of(like);
        ApiResponse<List<RecipeLikes>> responseBody = ApiResponse.success("Fetched recipe likes", likes);

        when(userInteractionService.getRecipeLikesByRecipeId(recipeId)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/user/interaction/like-recipe/recipe/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fetched recipe likes"))
                .andExpect(jsonPath("$.data[0].id.recipeId").value(recipeId))
                .andExpect(jsonPath("$.data[0].likedAt").value("2024-02-02T08:30:00"));

        verify(jwtTokenService, never()).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).getRecipeLikesByRecipeId(recipeId);
    }

    @Test
    void isRecipeLikedByUser_shouldRetrieveUserIdAndForwardToService() throws Exception {
        Long userId = 88L;
        Long recipeId = 400L;
        ApiResponse<Boolean> responseBody = ApiResponse.success("Checked like", true);

        when(jwtTokenService.getUserIdFromRequest(any(HttpServletRequest.class))).thenReturn(userId);
        when(userInteractionService.isRecipeLikedByUser(userId, recipeId)).thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/user/interaction/like-recipe/single/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Checked like"))
                .andExpect(jsonPath("$.data").value(true));

        verify(jwtTokenService).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).isRecipeLikedByUser(userId, recipeId);
    }

    @Test
    void toggleRecipeLike_whenServiceThrows_shouldReturnErrorFromAdvice() throws Exception {
        Long userId = 99L;
        Long recipeId = 500L;
        RecipeUserLikeDto requestDto = new RecipeUserLikeDto();
        requestDto.setRecipeId(recipeId);

        when(jwtTokenService.getUserIdFromRequest(any(HttpServletRequest.class))).thenReturn(userId);
        when(userInteractionService.toggleRecipeLike(recipeId, userId)).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(post("/api/user/interaction/like-recipe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred: Service failure"));

        verify(jwtTokenService).getUserIdFromRequest(any(HttpServletRequest.class));
        verify(userInteractionService).toggleRecipeLike(recipeId, userId);
    }
}
