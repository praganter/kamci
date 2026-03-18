package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.AddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.debttracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("category/")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("save")
    ResponseEntity<SaveCategoryResponse> save(@RequestBody @Valid final CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.save(request));
    }

    @PostMapping("update")
    ResponseEntity<UpdateCategoryResponse> update(@RequestBody @Valid final UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(request));
    }

    @DeleteMapping("delete/{id}")
    ResponseEntity<Void> delete(@PathVariable final String id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{id}/member")
    ResponseEntity<Void> addMember(
            @PathVariable final String id,
            @RequestBody @Valid final AddMemberRequest request) {
        categoryService.addMember(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("members/bulk")
    ResponseEntity<Void> bulkAddMember(@RequestBody @Valid final BulkAddMemberRequest request) {
        categoryService.bulkAddMember(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}/member/{userId}")
    ResponseEntity<Void> removeMember(
            @PathVariable final String id,
            @PathVariable final String userId) {
        categoryService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("getAll")
    ResponseEntity<List<GetAllCategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}