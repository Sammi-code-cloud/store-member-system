package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.service.BusinessDictionary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-dictionary")
@PreAuthorize("hasAuthority('store:all')")
@RequiredArgsConstructor
public class BusinessDictionaryController {
    private final BusinessDictionary dictionary;

    @GetMapping
    public Result<State> get() {
        return Result.success(new State(BusinessDictionary.KEY, dictionary.enabled()));
    }

    @PutMapping
    public Result<State> update(@Valid @RequestBody Update request) {
        dictionary.update(request.enabled());
        return Result.success(new State(BusinessDictionary.KEY, dictionary.enabled()));
    }

    public record Update(@NotNull Boolean enabled) {}
    public record State(String key, boolean enabled) {}
}
