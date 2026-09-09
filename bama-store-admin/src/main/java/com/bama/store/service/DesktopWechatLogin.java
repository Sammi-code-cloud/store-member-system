package com.bama.store.service;

import com.bama.store.common.BusinessException;
import com.bama.store.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.*;

/** Single-server, short-lived browser login challenges. Restart invalidates outstanding codes. */
@Service @RequiredArgsConstructor
public class DesktopWechatLogin {
 private final WechatClient wechat;
 private final JdbcTemplate jdbc;
 private final AuthService auth;
 private final StoreAvailability stores;
 private final SecureRandom random=new SecureRandom();
 private final Map<String,Entry> entries=new HashMap<>();
 private final Map<String,Long> attempts=new HashMap<>();
 private static class Entry {
  String secret; long expires; Long staff;
  Entry(String secret,long expires){this.secret=secret;this.expires=expires;}
 }
 private String randomToken(){byte[] bytes=new byte[16];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public Map<String,Object> create(String address) {
  String ticket=randomToken(),secret=randomToken();
  synchronized(this){
   long now=System.currentTimeMillis();entries.values().removeIf(e->e.expires<=now);attempts.values().removeIf(t->t+15000<now);
   if(entries.size()>=1000 || attempts.size()>=1000 || attempts.containsKey(address))throw new BusinessException("请求过于频繁，请稍后再试");
   attempts.put(address,now);
  }
  String image=wechat.desktopLoginCode(ticket);
  synchronized(this){entries.put(ticket,new Entry(WechatFlows.hash(secret),System.currentTimeMillis()+120000));}
  return Map.of("ticket",ticket,"secret",secret,"image",image,"expiresIn",120);
 }
 private Entry require(String ticket){Entry e=entries.get(ticket);if(e==null || e.expires<=System.currentTimeMillis()){entries.remove(ticket);throw new BusinessException("登录码已过期，请在电脑上刷新");}return e;}
 private Entry browser(String ticket,String secret){Entry e=require(ticket);if(secret==null || !e.secret.equals(WechatFlows.hash(secret)))throw new BusinessException("登录请求无效");return e;}
 private LoginResponse account(Long id){LoginResponse result=auth.loginById(id);stores.requireActive(result.getStoreId());if(result.getPermissions().isEmpty())throw new BusinessException("该员工没有后台权限");return result;}
 public void confirm(String ticket,String code){
  synchronized(this){if(require(ticket).staff!=null)throw new BusinessException("登录码已确认，请勿重复操作");}
  var identity=wechat.exchange("MINI",code);var keys=new ArrayList<String>();keys.add("APP:"+WechatFlows.hash(identity.appId()+":"+identity.openId()));
  if(identity.unionId()!=null&&!identity.unionId().isBlank())keys.add("UNION:"+WechatFlows.hash(identity.unionId()));
  var ids=new HashSet<Long>();for(String key:keys)ids.addAll(jdbc.queryForList("SELECT account_id FROM t_wechat_account WHERE identity_key=? AND audience='STAFF'",Long.class,key));
  if(ids.size()!=1)throw new BusinessException("当前微信未绑定有效员工，请先联系管理员绑定");
  Long id=ids.iterator().next();account(id);
  synchronized(this){Entry e=require(ticket);if(e.staff!=null)throw new BusinessException("登录码已确认");e.staff=id;}
 }
 public synchronized Map<String,Object> poll(String ticket,String secret){
  Entry e=browser(ticket,secret);if(e.staff==null)return Map.of("status","WAITING");
  LoginResponse result=account(e.staff);entries.remove(ticket);return Map.of("status","CONFIRMED","account",result);
 }
 public synchronized void cancel(String ticket,String secret){browser(ticket,secret);entries.remove(ticket);}
}
