package com.bama.store;
import com.bama.store.service.*;
import com.bama.store.controller.BannerController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BannerImagesTest {
 String png(BufferedImage image)throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return "data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray());}
 @Test void largePhotoGetsSmallerAndResized()throws Exception {var source=new BufferedImage(1800,1000,BufferedImage.TYPE_INT_RGB);var r=new Random(8);for(int y=0;y<1000;y++)for(int x=0;x<1800;x++)source.setRGB(x,y,((x/8+r.nextInt(10))%256)<<16|((y/8+r.nextInt(10))%256)<<8|100);String data=png(source);var result=new BannerImages().optimize(data);var decoded=ImageIO.read(new ByteArrayInputStream(result.bytes()));assertThat(decoded.getWidth()).isEqualTo(1500);assertThat(decoded.getHeight()).isLessThanOrEqualTo(900);assertThat(result.bytes().length).isLessThan(Base64.getDecoder().decode(data.split(",")[1]).length/2);}
 @Test void smallTransparentImageStaysTransparentAndNeverEnlarges()throws Exception {var source=new BufferedImage(100,50,BufferedImage.TYPE_INT_ARGB);source.setRGB(20,20,0xffff0000);String data=png(source);var result=new BannerImages().optimize(data);var decoded=ImageIO.read(new ByteArrayInputStream(result.bytes()));assertThat(decoded.getWidth()).isEqualTo(100);assertThat(decoded.getRGB(0,0)>>>24).isZero();assertThat(result.type()).isEqualTo("image/png");}
 @Test void versionedImagesCacheAndConditionalGetHasNoBody()throws Exception {var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList("SELECT version FROM t_banner WHERE id=?",String.class,1L)).thenReturn(List.of("v1"));when(jdbc.queryForList("SELECT image_data FROM t_banner WHERE id=? AND version=?",String.class,1L,"v1")).thenReturn(List.of(png(new BufferedImage(100,50,BufferedImage.TYPE_INT_RGB))));var c=new BannerController(jdbc,mock(AuditService.class),new BannerImages());var first=c.image(1L,"v1",null);assertThat(first.getHeaders().getCacheControl()).contains("max-age=604800");var next=c.image(1L,"v1",first.getHeaders().getETag());assertThat(next.getStatusCode().value()).isEqualTo(304);assertThat(next.getBody()).isNull();assertThat(c.image(1L,"old",null).getHeaders().getCacheControl()).isEqualTo("no-cache");verify(jdbc,times(1)).queryForList("SELECT image_data FROM t_banner WHERE id=? AND version=?",String.class,1L,"v1");when(jdbc.queryForList("SELECT version FROM t_banner WHERE id=?",String.class,1L)).thenReturn(List.of());assertThat(c.image(1L,"v1",null).getStatusCode().value()).isEqualTo(404);}
}
