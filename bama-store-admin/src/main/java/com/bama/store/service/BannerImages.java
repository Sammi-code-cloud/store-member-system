package com.bama.store.service;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import org.springframework.stereotype.Service;

/** Bounded display dimensions; keep transparency and never enlarge source images. */
@Service
public class BannerImages {
 public record Image(String type,byte[] bytes) {}
 public Image optimize(String data) {
  byte[] original=Base64.getDecoder().decode(data.substring(data.indexOf(',')+1));
  String type=data.substring(5,data.indexOf(';'));
  try {
   BufferedImage source=ImageIO.read(new ByteArrayInputStream(original));
   if(source==null)throw new IOException("Invalid image");
   double scale=Math.min(1,Math.min(1500d/source.getWidth(),900d/source.getHeight()));
   int w=Math.max(1,(int)Math.round(source.getWidth()*scale)),h=Math.max(1,(int)Math.round(source.getHeight()*scale));
   boolean alpha=false;
   if(source.getColorModel().hasAlpha())outer:for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)!=255){alpha=true;break outer;}
   BufferedImage target=new BufferedImage(w,h,alpha?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
   Graphics2D g=target.createGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,w,h,null);}finally{g.dispose();}
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   if(alpha)ImageIO.write(target,"png",out);
   else {
    ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
    try(var output=ImageIO.createImageOutputStream(out)){
     writer.setOutput(output);var params=writer.getDefaultWriteParam();params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(.82f);writer.write(null,new IIOImage(target,null,null),params);
    }finally{writer.dispose();}
   }
   byte[] result=out.toByteArray();
   return result.length<original.length ? new Image(alpha?"image/png":"image/jpeg",result) : new Image(type,original);
  }catch(IOException e){return new Image(type,original);}
 }
 public String dataUrl(String data){Image image=optimize(data);return "data:"+image.type()+";base64,"+Base64.getEncoder().encodeToString(image.bytes());}
}
