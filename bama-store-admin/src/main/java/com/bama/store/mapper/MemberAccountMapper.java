package com.bama.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bama.store.entity.MemberAccount;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface MemberAccountMapper extends BaseMapper<MemberAccount> {

    /** 统计全部会员卡余额总额 */
    @Select("SELECT IFNULL(SUM(balance),0) FROM t_member_account WHERE deleted = 0")
    BigDecimal sumBalance();
}
