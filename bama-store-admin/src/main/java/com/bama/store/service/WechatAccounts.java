package com.bama.store.service;
import com.bama.store.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class WechatAccounts {
 private final JdbcTemplate jdbc; private final CustomerAuthService customers; private final AuthService staff; private final WechatFlows flows;
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
}
