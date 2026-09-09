package com.bama.store.controller;
import com.bama.store.common.Result;
import com.bama.store.service.MemberEnrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletResponse;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class MemberEnrollmentController {
 private final MemberEnrollment enrollment;
 public record Create(String name,String phone,String remark){}
 public record Bind(String ticket,String phone,String code){}
 @PostMapping("/members") @PreAuthorize("hasAuthority('member:manage')")
 public Result<?> create(@RequestBody Create body){return Result.success(enrollment.create(body.name(),body.phone(),body.remark()));}
 @PostMapping("/members/{id}/wechat-code") @PreAuthorize("hasAuthority('member:manage')")
 public Result<?> code(@PathVariable Long id,HttpServletResponse response){response.setHeader("Cache-Control","no-store");return Result.success(enrollment.code(id));}
 @PostMapping("/wechat/customer/member-bind")
 public Result<?> bind(@RequestBody Bind body,HttpServletResponse response){response.setHeader("Cache-Control","no-store");return Result.success(enrollment.bind(body.ticket(),body.phone(),body.code()));}
}
