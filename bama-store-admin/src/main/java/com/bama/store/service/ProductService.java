package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.entity.Product;
import com.bama.store.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 货品管理（录入 / 编辑 / 上下架）
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public Page<Product> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getStoreId, com.bama.store.security.SecurityUtil.storeId());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Product::getName, keyword).or().like(Product::getBarcode, keyword));
        }
        wrapper.orderByDesc(Product::getId);
        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("货品不存在");
        }
        com.bama.store.security.SecurityUtil.ownStore(product.getStoreId());
        return product;
    }

    /** 录入或更新货品 */
    public Long save(Product product) {
        if (product.getId() != null) getById(product.getId());
        product.setStoreId(com.bama.store.security.SecurityUtil.storeId());
        // 条码唯一校验
        if (StringUtils.hasText(product.getBarcode())) {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                    .eq(Product::getBarcode, product.getBarcode());
            if (product.getId() != null) {
                wrapper.ne(Product::getId, product.getId());
            }
            if (productMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("该条码已存在");
            }
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product.getId();
    }

    public void updateStatus(Long id, Integer status) {
        getById(id);
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public void delete(Long id) {
        getById(id);
        productMapper.deleteById(id);
    }
}
