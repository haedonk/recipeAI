package com.haekitchenapp.recipeapp.service.impl;

import com.haekitchenapp.recipeapp.entity.Cuisine;
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException;
import com.haekitchenapp.recipeapp.repository.CuisineRepository;
import com.haekitchenapp.recipeapp.service.CuisineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CuisineServiceImpl implements CuisineService {

    private final CuisineRepository cuisineRepository;

    @Override
    public Optional<Cuisine> findById(Integer id) {
        return cuisineRepository.findById(id);
    }

    @Override
    public Optional<Cuisine> findByName(String name) {
        return cuisineRepository.findByName(name);
    }

    @Override
    public List<Cuisine> findAll() {
        return cuisineRepository.findAll();
    }

    @Override
    public List<Cuisine> findByNameContaining(String name) {
        return cuisineRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Cuisine> findByRecipeId(Long recipeId) {
        return cuisineRepository.findByRecipeId(recipeId);
    }

    @Override
    @Transactional
    public Cuisine createCuisine(Cuisine cuisine) {
        log.info("Creating new cuisine: {}", cuisine.getName());
        return cuisineRepository.save(cuisine);
    }

    @Override
    @Transactional
    public Cuisine updateCuisine(Integer id, Cuisine cuisine) {
        log.info("Updating cuisine with id: {}", id);
        return cuisineRepository.findById(id)
                .map(existingCuisine -> {
                    existingCuisine.setName(cuisine.getName());
                    return cuisineRepository.save(existingCuisine);
                })
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteCuisine(Integer id) {
        log.info("Deleting cuisine with id: {}", id);
        if (!cuisineRepository.existsById(id)) {
            throw new CuisineNotFoundException("Cuisine not found with id: " + id);
        }
        cuisineRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return cuisineRepository.existsByName(name);
    }
}
