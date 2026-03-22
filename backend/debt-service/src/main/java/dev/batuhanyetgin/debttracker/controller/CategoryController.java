package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.AddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.debttracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("category/")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category and membership management")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create a new category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Category limit exceeded"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("save")
    ResponseEntity<SaveCategoryResponse> save(@RequestBody @Valid final CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.save(request));
    }

    @Operation(summary = "Update a category name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "403", description = "Not the category owner"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("update")
    ResponseEntity<UpdateCategoryResponse> update(@RequestBody @Valid final UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(request));
    }

    @Operation(summary = "Delete a category and all its debts")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category and associated debts deleted"),
            @ApiResponse(responseCode = "403", description = "Not the category owner"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("delete/{id}")
    ResponseEntity<Void> delete(@PathVariable final String id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a member to a category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member added"),
            @ApiResponse(responseCode = "403", description = "Not the category owner"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Member already exists"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("{id}/member")
    ResponseEntity<Void> addMember(
            @PathVariable final String id,
            @RequestBody @Valid final AddMemberRequest request) {
        categoryService.addMember(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Add a member to multiple categories at once")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member added to all specified categories"),
            @ApiResponse(responseCode = "403", description = "Not the owner of one or more categories"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("members/bulk")
    ResponseEntity<Void> bulkAddMember(@RequestBody @Valid final BulkAddMemberRequest request) {
        categoryService.bulkAddMember(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove a member from a category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "403", description = "Not the category owner"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("{id}/member/{userId}")
    ResponseEntity<Void> removeMember(
            @PathVariable final String id,
            @PathVariable final String userId) {
        categoryService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all categories owned by or shared with the authenticated user")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    @GetMapping("getAll")
    ResponseEntity<List<GetAllCategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}