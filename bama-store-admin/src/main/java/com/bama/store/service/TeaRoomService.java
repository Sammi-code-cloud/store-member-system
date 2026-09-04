package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bama.store.entity.TeaRoom;
import com.bama.store.mapper.TeaRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 茶室管理
 */
@Service
@RequiredArgsConstructor
public class TeaRoomService {

    private final TeaRoomMapper teaRoomMapper;

    public List<TeaRoom> list() {
        return teaRoomMapper.selectList(
                new LambdaQueryWrapper<TeaRoom>().orderByDesc(TeaRoom::getId));
    }

    public Long save(TeaRoom room) {
        if (room.getStatus() == null) {
            room.setStatus(1);
        }
        if (room.getId() == null) {
            teaRoomMapper.insert(room);
        } else {
            teaRoomMapper.updateById(room);
        }
        return room.getId();
    }

    public void delete(Long id) {
        teaRoomMapper.deleteById(id);
    }
}
