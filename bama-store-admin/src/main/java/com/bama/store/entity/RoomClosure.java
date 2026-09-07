package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_room_closure")
public class RoomClosure extends BaseEntity {
    private Long roomId;
    private LocalDate closureDate;
    private String startTime;
    private String endTime;
    private String reason;
    private Long storeId;
}
