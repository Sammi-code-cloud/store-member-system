package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.entity.TeaRoom;
import com.bama.store.service.TeaRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 茶室管理
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class TeaRoomController {

    private final TeaRoomService teaRoomService;

    @GetMapping
    @PreAuthorize("hasAuthority('reservation:view')")
    public Result<List<TeaRoom>> list() {
        return Result.success(teaRoomService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Long> save(@RequestBody TeaRoom room) {
        return Result.success(teaRoomService.save(room));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        teaRoomService.delete(id);
        return Result.success();
    }
}
