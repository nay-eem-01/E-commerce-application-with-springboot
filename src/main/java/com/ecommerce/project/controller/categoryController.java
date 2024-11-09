package com.ecommerce.project.controller;


import com.ecommerce.project.DTO.CategoryDTO;
import com.ecommerce.project.DTO.CategoryResponse;
import com.ecommerce.project.config.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.project.service.CategoryService;


@RestController
@RequestMapping("/api")
public class categoryController {

    private final CategoryService categoryService;


    @Autowired
    public categoryController(CategoryService categoryService) {
        this.categoryService = categoryService;

    }


    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getCategories(
            @RequestParam(name = "pageNumber",defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy",defaultValue = AppConstants.SORT_CATEGORIES_BY)String sortBy,
            @RequestParam(name = "sortOrder",defaultValue = AppConstants.SORT_ORDER)String sortOrder ) {

        CategoryResponse categories = categoryService.getAllCategories(pageNumber,pageSize,sortBy,sortOrder);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @PostMapping("/admin/addCategory")
    public ResponseEntity<CategoryDTO> addNewCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.addNewCategory(categoryDTO);
        return new ResponseEntity<>(savedCategoryDTO , HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/deleteCategory/{id}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long id) {
      CategoryDTO deleteCategoryDTO = categoryService.deleteCategory(id);
        return new ResponseEntity<>(deleteCategoryDTO, HttpStatus.OK);

    }

    @PutMapping("admin/updateCategory/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(@RequestBody CategoryDTO categoryDTO, @PathVariable Long id) {
       CategoryDTO updatedCategoryDTO =  categoryService.updateCategory(categoryDTO, id);
        return new ResponseEntity<>(updatedCategoryDTO, HttpStatus.OK);

    }

}
