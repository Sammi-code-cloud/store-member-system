package com.bama.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_audit_log")
public class AuditLog extends BaseEntity {
    private Long staffId;
    private Long storeId;
    private String actor;
    private String action;
    private String target;
    private String detail;
}
