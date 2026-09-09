package com.bama.store.controller;

import com.bama.store.common.*;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class BannerController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final com.bama.store.service.BannerImages optimizer;
    private final Map<String,com.bama.store.service.BannerImages.Image> imageCache=Collections.synchronizedMap(new LinkedHashMap<>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,com.bama.store.service.BannerImages.Image> entry){return size()>32;}
    });
    public record Input(Long id, String title, String image, Integer sortOrder, Integer status, String target) {}
    private Map<String,Object> item(java.sql.ResultSet r) throws java.sql.SQLException {
        return Map.of("id",r.getLong("id"),"title",r.getString("title"),"sortOrder",r.getInt("sort_order"),"status",r.getInt("status"),"target",r.getString("target"),"imageUrl","/api/banner-images/"+r.getLong("id")+"?v="+r.getString("version"));
    }
    @GetMapping("/api/banners") @PreAuthorize("hasAuthority('store:manage')")
    public Result<?> list() {
        return Result.success(jdbc.query("SELECT id,title,sort_order,status,target,version FROM t_banner WHERE store_id=? ORDER BY sort_order,id",(r,n)->item(r),SecurityUtil.storeId()));
    }
    @GetMapping("/api/customer/banners")
    public Result<?> customer(@RequestParam Long storeId) {
        return Result.success(jdbc.query("SELECT b.id,b.title,b.sort_order,b.status,b.target,b.version FROM t_banner b JOIN t_store s ON s.id=b.store_id WHERE b.store_id=? AND b.status=1 AND s.status=1 AND s.deleted=0 ORDER BY b.sort_order,b.id",(r,n)->item(r),storeId));
    }
    @GetMapping("/api/banner-images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id,@RequestParam(required=false) String v,@RequestHeader(value="If-None-Match",required=false) String ifNoneMatch) {
        var versions=jdbc.queryForList("SELECT version FROM t_banner WHERE id=?",String.class,id);
        if(versions.isEmpty())return ResponseEntity.notFound().build();
        String version=versions.get(0),key=id+"-"+version+"-opt1",etag="\""+key+"\"";
        CacheControl cache=version.equals(v)?CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic():CacheControl.noCache();
        if(ifNoneMatch!=null && Arrays.stream(ifNoneMatch.split(",")).map(String::trim).anyMatch(t->"*".equals(t)||etag.equals(t.replaceFirst("^W/",""))))
            return ResponseEntity.status(304).cacheControl(cache).eTag(etag).build();
        var image=imageCache.get(key);
        if(image==null){
            var images=jdbc.queryForList("SELECT image_data FROM t_banner WHERE id=? AND version=?",String.class,id,version);
            if(images.isEmpty())return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
            image=optimizer.optimize(images.get(0));imageCache.put(key,image);
        }
        return ResponseEntity.ok().cacheControl(cache).eTag(etag).contentType(MediaType.parseMediaType(image.type())).body(image.bytes());
    }
    @PostMapping("/api/banners") @PreAuthorize("hasAuthority('store:manage')") @Transactional
    public Result<?> save(@RequestBody Input body) {
        if(body.title()==null || body.title().isBlank() || body.title().length()>60)throw new BusinessException("标题为1–60字");
        if(body.status()==null || body.status()!=0 && body.status()!=1 || body.sortOrder()==null || body.sortOrder()<0 || body.sortOrder()>999)throw new BusinessException("状态或排序不正确");
        if(!Set.of("none","rooms").contains(body.target()==null?"":body.target()))throw new BusinessException("跳转目标无效");
        long store=SecurityUtil.storeId();
        if(body.id()!=null) {
            var found=jdbc.queryForList("SELECT store_id FROM t_banner WHERE id=?",Long.class,body.id());
            if(found.isEmpty())throw new BusinessException("Banner不存在");
            SecurityUtil.ownStore(found.get(0));
        }
        String image=body.image()==null?null:validateImage(body.image());
        if(body.id()==null) {
            if(image==null)throw new BusinessException("请上传图片");
            // Lock the store to keep the per-store limit correct under concurrent saves.
            jdbc.queryForObject("SELECT id FROM t_store WHERE id=? FOR UPDATE",Long.class,store);
            if(jdbc.queryForObject("SELECT COUNT(*) FROM t_banner WHERE store_id=?",Integer.class,store)>=10)throw new BusinessException("每家分店最多10张Banner，请先删除不用的图片");
            jdbc.update("INSERT INTO t_banner(store_id,title,image_data,sort_order,status,target,version) VALUES(?,?,?,?,?,?,?)",store,body.title().trim(),image,body.sortOrder(),body.status(),body.target(),UUID.randomUUID().toString());
        } else {
            jdbc.update("UPDATE t_banner SET title=?,sort_order=?,status=?,target=?,version=? WHERE id=? AND store_id=?",body.title().trim(),body.sortOrder(),body.status(),body.target(),UUID.randomUUID().toString(),body.id(),store);
            if(image!=null)jdbc.update("UPDATE t_banner SET image_data=? WHERE id=? AND store_id=?",image,body.id(),store);
        }
        audit.record("保存首页Banner",body.id(),body.title());
        return Result.success();
    }
    @DeleteMapping("/api/banners/{id}") @PreAuthorize("hasAuthority('store:manage')") @Transactional
    public Result<?> delete(@PathVariable Long id) {
        var found=jdbc.queryForList("SELECT store_id FROM t_banner WHERE id=?",Long.class,id);
        if(found.isEmpty())throw new BusinessException("Banner不存在");
        SecurityUtil.ownStore(found.get(0));
        jdbc.update("DELETE FROM t_banner WHERE id=? AND store_id=?",id,SecurityUtil.storeId());
        audit.record("删除首页Banner",id,"");
        return Result.success();
    }
    private String validateImage(String data) {
        try {
            if(data.length()>2800000 || !data.matches("(?s)^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$"))throw new IOException();
            byte[] bytes=Base64.getDecoder().decode(data.substring(data.indexOf(',')+1));
            if(bytes.length>2*1024*1024)throw new IOException();
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                if(!readers.hasNext())throw new IOException();
                var reader=readers.next();
                try {
                    reader.setInput(stream);
                    int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<100 || h<50 || w>6000 || h>6000 || (long)w*h>16000000)throw new IOException();
                    var decoded=reader.read(0);
                    var out=new ByteArrayOutputStream();
                    String format=data.startsWith("data:image/png;")?"png":"jpeg";
                    if(!ImageIO.write(decoded,format,out))throw new IOException();
                    if(out.size()>2*1024*1024)throw new IOException();
                    return optimizer.dataUrl("data:image/"+format+";base64,"+Base64.getEncoder().encodeToString(out.toByteArray()));
                } finally { reader.dispose(); }
            }
        } catch(Exception e) { throw new BusinessException("请上传2MB以内的有效JPG/PNG图片，尺寸100×50至6000×6000，总像素不超过1600万"); }
    }
}
