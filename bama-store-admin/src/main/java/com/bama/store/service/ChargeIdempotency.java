package com.bama.store.service;

import com.bama.store.common.*;
import com.bama.store.dto.*;
import com.bama.store.vo.ChargeResultVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

/** 请求占位、扣款及结果保存必须使用同一数据库事务，进程或网络中断不能留下半笔扣款。 */
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class ChargeIdempotency {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ChargeResultVo begin(ChargeConfirmRequest req,List<ChargeItemDto> items,Long staffId,Long storeId,String memberName,String staffName){
        if(req.getBizNo()==null || !req.getBizNo().matches("[A-Za-z0-9_-]{1,64}"))throw new BusinessException("扣款必须提供有效业务号，重试请使用原业务号");
        var signature=new LinkedHashMap<String,Object>();
        signature.put("memberId",req.getMemberId());signature.put("staffId",staffId);signature.put("storeId",storeId);
        signature.put("manual",req.getAmount()!=null);signature.put("phone",Objects.toString(req.getExpectedPhone(),""));signature.put("remark",Objects.toString(req.getRemark(),""));
        signature.put("items",items.stream().map(i->List.of(i.getItemType(),i.getItemName(),i.getPrice().stripTrailingZeros().toPlainString(),i.getQuantity()==null?1:i.getQuantity())).toList());
        String hash;
        try{hash=WechatFlows.hash(json.writeValueAsString(signature));}catch(Exception e){throw new IllegalStateException(e);}
        // Unique-key upsert acquires a database row lock even across different application processes.
        jdbc.update("INSERT INTO t_charge_request(biz_no,request_hash) VALUES(?,?) ON DUPLICATE KEY UPDATE biz_no=biz_no",req.getBizNo(),hash);
        var row=jdbc.queryForMap("SELECT request_hash,result_json FROM t_charge_request WHERE biz_no=? FOR UPDATE",req.getBizNo());
        if(!hash.equals(row.get("request_hash")))throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        if(row.get("result_json")!=null){try{return json.readValue(row.get("result_json").toString(),ChargeResultVo.class);}catch(Exception e){throw new IllegalStateException(e);}}
        ChargeResultVo old=legacy(req,items,staffId,storeId,memberName,staffName);
        if(old!=null)complete(req.getBizNo(),old);
        return old;
    }
    public void complete(String bizNo,ChargeResultVo result){
        try{
            if(jdbc.update("UPDATE t_charge_request SET result_json=? WHERE biz_no=? AND result_json IS NULL",json.writeValueAsString(result),bizNo)!=1)throw new IllegalStateException("扣款回执保存失败");
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
    private ChargeResultVo legacy(ChargeConfirmRequest req,List<ChargeItemDto> items,Long staffId,Long storeId,String memberName,String staffName){
        // Include logically deleted rows so an old business number can never debit again.
        var rows=jdbc.queryForList("SELECT * FROM t_wallet_txn WHERE biz_no=? ORDER BY id FOR UPDATE",req.getBizNo());
        if(rows.isEmpty())return null;
        if(rows.size()!=1)throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        var txn=rows.get(0);
        if(!"CONSUME".equals(txn.get("type"))||!Objects.equals(((Number)txn.get("member_id")).longValue(),req.getMemberId())
                ||!Objects.equals(txn.get("staff_id"),staffId)||!Objects.equals(txn.get("store_id"),storeId))throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        var orders=jdbc.queryForList("SELECT * FROM t_consume_order WHERE order_no=?",txn.get("ref_order_no"));
        if(orders.size()!=1)throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        var order=orders.get(0);
        var saved=jdbc.queryForList("SELECT item_type,item_name,price,quantity FROM t_consume_item WHERE order_id=? ORDER BY id",order.get("id"));
        if(saved.size()!=items.size()||!Objects.toString(order.get("remark"),"").equals(Objects.toString(req.getRemark(),"")))throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        for(int n=0;n<items.size();n++){
            var expected=items.get(n);var actual=saved.get(n);
            if(!Objects.equals(actual.get("item_type"),expected.getItemType())||!Objects.equals(actual.get("item_name"),expected.getItemName())
                ||new BigDecimal(actual.get("price").toString()).compareTo(expected.getPrice())!=0
                ||((Number)actual.get("quantity")).intValue()!=(expected.getQuantity()==null?1:expected.getQuantity()))throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }
        ChargeResultVo result=new ChargeResultVo();result.setOrderNo(txn.get("ref_order_no").toString());result.setMemberId(req.getMemberId());result.setMemberName(memberName);
        result.setOriginAmount(new BigDecimal(order.get("origin_amount").toString()));result.setPayAmount(new BigDecimal(txn.get("amount").toString()));
        result.setBalanceAfter(new BigDecimal(txn.get("balance_after").toString()));result.setStaffName(staffName);
        Object time=txn.get("create_time");result.setTime(time instanceof java.sql.Timestamp t?t.toLocalDateTime():(java.time.LocalDateTime)time);
        return result;
    }
}
