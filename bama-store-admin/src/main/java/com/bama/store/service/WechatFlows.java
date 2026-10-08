package com.bama.store.service;
import com.bama.store.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
@Service @RequiredArgsConstructor
public class WechatFlows {
 private final JdbcTemplate jdbc; private final ObjectMapper json;
 public static String random() { byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
 public static String hash(String token) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public String create(String kind,Map<String,String> payload) {
  jdbc.update("DELETE FROM t_wechat_flow WHERE expires_at < ?",Timestamp.from(Instant.now()));
  String token=random();
  try{jdbc.update("INSERT INTO t_wechat_flow(token_hash,kind,payload,expires_at) VALUES (?,?,?,?)",hash(token),kind,json.writeValueAsString(payload),Timestamp.from(Instant.now().plusSeconds(300)));}
  catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
  return token;
 }
 @Transactional
 public Map<String,String> peek(String token,String kind) {
  if(token==null || token.length()!=43)throw new BusinessException("微信登录请求已过期，请重新发起");
  var rows=jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind=? AND expires_at>?",hash(token),kind,Timestamp.from(Instant.now()));
  if(rows.size()!=1)throw new BusinessException("微信登录请求已过期或已使用，请重新发起");
  try{return json.readValue(rows.get(0).get("payload").toString(),new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});}catch(Exception e){throw new IllegalStateException(e);}
 }
 // Password-based staff binding keeps its original single-use ticket behavior even if authentication fails.
 @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
 public Map<String,String> consumeStaffBinding(String token) {
  return consume(token,"BIND");
 }
 @Transactional
 public Map<String,String> consume(String token,String kind) {
  if(token==null || token.length()!=43)throw new BusinessException("微信登录请求已过期，请重新发起");
  var rows=jdbc.queryForList("SELECT payload FROM t_wechat_flow WHERE token_hash=? AND kind=? AND expires_at>? FOR UPDATE",hash(token),kind,Timestamp.from(Instant.now()));
  if(rows.size()!=1)throw new BusinessException("微信登录请求已过期或已使用，请重新发起");
  jdbc.update("DELETE FROM t_wechat_flow WHERE token_hash=?",hash(token));
  try{return json.readValue(rows.get(0).get("payload").toString(),new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});}catch(Exception e){throw new IllegalStateException(e);}
 }
}
