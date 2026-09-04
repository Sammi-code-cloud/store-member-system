package com.bama.store.controller;

import com.bama.store.common.PageResult;
import com.bama.store.common.Result;
import com.bama.store.entity.Product;
import com.bama.store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 货品管理（录入）
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    public Result<PageResult<Product>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(productService.page(pageNum, pageSize, keyword)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /** 录入 / 编辑货品 */
    @PostMapping
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Long> save(@RequestBody Product product) {
        return Result.success(productService.save(product));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
