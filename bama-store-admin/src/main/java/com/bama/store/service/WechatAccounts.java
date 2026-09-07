package com.bama.store.service;
import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class WechatAccounts {
 private final JdbcTemplate jdbc; private final CustomerAuthService customers; private final AuthService staff; private final WechatFlows flows; private final SmsSender sms;
 private List<String> keys(WechatClient.Identity i) {
  var keys=new ArrayList<String>();keys.add("APP:"+WechatFlows.hash(i.appId()+":"+i.openId()));
  if(i.unionId()!=null && !i.unionId().isBlank())keys.add("UNION:"+WechatFlows.hash(i.unionId()));return keys;
 }
 private Long find(WechatClient.Identity i,String audience) {
  var ids=new HashSet<Long>();
  for(String key:keys(i)) ids.addAll(jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience=?",Long.class,key,audience));
  if(ids.size()>1)throw new BusinessException("微信身份关联冲突，请联系管理员处理");return ids.stream().findFirst().orElse(null);
 }
 private void link(WechatClient.Identity i,String audience,Long id) {
  for(String key:keys(i)) {
   var existing=jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience=?",Long.class,key,audience);
   if(existing.isEmpty())jdbc.update("INSERT INTO t_wechat_account(identity_key,audience,account_id) VALUES (?,?,?)",key,audience,id);
   else if(!existing.get(0).equals(id))throw new BusinessException("该微信已绑定其他账号");
  }
 }
 @Transactional
 public Map<String,Object> login(WechatClient.Identity identity,String audience) {
  Long id=find(identity,audience);
  if("CUSTOMER".equals(audience) && (id==null || !phoneVerified(id))) {
   if(id!=null) customers.requireActive(id);
   String ticket=flows.create("CUSTOMER_BIND",Map.of("app",identity.appId(),"openid",identity.openId(),"unionid",identity.unionId()==null?"":identity.unionId(),"audience",audience));
   return Map.of("bindRequired",true,"bindTicket",ticket,"audience",audience,"smsEnabled",sms.ready());
  }
  if("STAFF".equals(audience) && id==null) {
   String ticket=flows.create("BIND",Map.of("app",identity.appId(),"openid",identity.openId(),"unionid",identity.unionId()==null?"":identity.unionId(),"audience",audience));
   return Map.of("bindRequired",true,"bindTicket",ticket,"audience",audience);
  }
  Object account;
  if("CUSTOMER".equals(audience)) {var customer=customers.wechatLogin(id); id=((Number)customer.get("memberId")).longValue();account=customer;}
  else account=staff.loginById(id);
  link(identity,audience,id);return Map.of("bindRequired",false,"audience",audience,"account",account);
 }
 @Transactional
 public Map<String,Object> bind(WechatClient.Identity identity,Long id) {
  link(identity,"STAFF",id);
  return Map.of("bindRequired",false,"audience","STAFF","account",staff.loginById(id));
 }
 private boolean phoneVerified(Long id) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_phone_verified v JOIN t_member m ON m.id=v.member_id WHERE m.id=? AND m.phone=v.phone AND m.deleted=0",Integer.class,id)>0;
 }
 @Transactional
 public Map<String,Object> bindCustomer(String token,String phone) {
  jdbc.queryForList("SELECT id FROM t_sms_guard WHERE id=1 FOR UPDATE");
  var ticket=flows.consume(token,"CUSTOMER_BIND");
  var identity=new WechatClient.Identity(ticket.get("app"),ticket.get("openid"),ticket.get("unionid"));
  Long linked=find(identity,"CUSTOMER");
  var matching=jdbc.queryForList("SELECT id FROM t_member WHERE phone=? AND deleted=0 FOR UPDATE",Long.class,phone);
  if(matching.size()>1)throw new BusinessException("该手机号存在多个会员档案，请联系门店处理");
  Long target=matching.isEmpty()?null:matching.get(0);
  if(linked!=null) {
   customers.requireActive(linked);
   if(target!=null && !target.equals(linked))throw new BusinessException("微信档案和手机号会员档案不同，请联系门店核实合并，原有余额和订单会保留");
   if(phoneVerified(linked))throw new BusinessException("该微信已完成绑定，请重新微信登录；换绑请联系门店");
   var old=jdbc.queryForList("SELECT phone FROM t_member WHERE id=?",String.class,linked);
   if(!old.isEmpty() && old.get(0)!=null && !old.get(0).isBlank() && !phone.equals(old.get(0)))
    throw new BusinessException("手机号与原会员档案不一致，请联系门店核实");
   target=linked;
  }
  if(target!=null) {
   customers.requireActive(target);
   if(linked==null && jdbc.queryForObject("SELECT COUNT(*) FROM t_wechat_account WHERE audience='CUSTOMER' AND account_id=?",Integer.class,target)>0)
    throw new BusinessException("该会员已绑定其他微信，换绑请联系门店核实");
  } else target=customers.createWechatMember(phone);
  jdbc.update("UPDATE t_member SET phone=? WHERE id=? AND deleted=0",phone,target);
  jdbc.update("INSERT INTO t_wechat_phone_verified(member_id,phone,verified_at) VALUES(?,?,CURRENT_TIMESTAMP)",target,phone);
  link(identity,"CUSTOMER",target);
  return Map.of("bindRequired",false,"audience","CUSTOMER","account",customers.wechatLogin(target));
 }
}
