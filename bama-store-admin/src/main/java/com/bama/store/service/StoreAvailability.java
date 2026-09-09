package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.entity.Store;
import com.bama.store.mapper.StoreMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreAvailability {
    private final StoreMapper stores;

    public Store requireActive(Long id) {
        return validate(id == null ? null : stores.selectById(id));
    }

    /** Hold the store row until the business transaction commits, serializing with closure. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Store lockActive(Long id) {
        return validate(id == null ? null : stores.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, id).last("FOR UPDATE")));
    }

    private Store validate(Store store) {
        if (store == null || !Integer.valueOf(1).equals(store.getStatus()))
            throw new BusinessException("门店已暂停营业或已删除，请选择营业中的门店");
        return store;
    }
}
