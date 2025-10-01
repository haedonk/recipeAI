package com.haekitchenapp.recipeapp.model.response.recipe;

public interface RecipeSimilarityView {
  Long   getId();
  String getTitle();
  String getSummary();
  Double getCosineDistance();   // matches alias 'cosine_distance'
  Double getSimilarity();       // matches alias 'similarity'


}
