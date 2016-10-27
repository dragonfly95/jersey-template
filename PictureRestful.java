package app.rest.ecare.common;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import net.coobird.thumbnailator.Thumbnails;
import onnuri.db.SqlConfigManager;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibatis.sqlmap.client.SqlMapClient;


@Path("/picture")
public class PictureRestful {

  Properties prop = new Properties();

  String realDir = "";
  String tmpDir = "";

  // db ibatis
  SqlMapClient sqlMap = SqlConfigManager.getSqlMapInstance();


  /**
   * 파일 업로드하기
   */
  @POST
  @Path("/upload")
  @Produces(MediaType.TEXT_PLAIN)
  public HashMap upload(@Context HttpServletRequest request) throws Exception {

    File child = new File(request.getRealPath("/"),"/WEB-INF/config/setting.xml");
    prop.loadFromXML(new FileInputStream (child));

    realDir = (String) prop.get("USER_PHOTO_ABSOLUTEPATH");
    tmpDir  = (String) prop.get("tmpDir");

    boolean isMultipart = FileUpload.isMultipartContent(request);

    if(isMultipart) {

      HashMap map = new HashMap();
      
      DiskFileUpload upload = new DiskFileUpload();
      upload.setSizeMax(10 * 1024 * 1024);            // 10m 용량제한

      List items = upload.parseRequest(request);
      Iterator it = items.iterator();
      while(it.hasNext()) {
        FileItem fileItem = (FileItem) it.next();
        if(fileItem.isFormField()) {
//          System.out.println(fileItem.getFieldName() + "=" + fileItem.getString("utf-8") );
          map.put(fileItem.getFieldName(), fileItem.getString("utf-8"));

        } else {

          if(fileItem.getSize() > 0) {

            String fieldName  = fileItem.getFieldName();
            String fileName   = fileItem.getName();
            String contentType = fileItem.getContentType();
            boolean isInMemory = fileItem.isInMemory();
            long sizeInBytes  = fileItem.getSize();
            String suffix       = ".gif";

 System.out.println("파일 [fieldName] : "+ fieldName +"<br/>");
 System. out.println("파일 [fileName] : "+ fileName +"<br/>");
 System. out.println("파일 [contentType] : "+ contentType +"<br/>");
 System. out.println("파일 [isInMemory] : "+ isInMemory +"<br/>");
 System. out.println("파일 [sizeInBytes] : "+ sizeInBytes +"<br/>");
 System. out.println("파일 [suffix] : "+ suffix +"<br/>");

              int width = 250;
              int height = 200;

            // 사진파일만 저장
              if(".gif".equals(suffix) || ".png".equals(suffix) || ".jpg".equals(suffix) || ".jpeg".equals(suffix)) {

//              1. 파일업로드하여 저장 - 사진만 업로드 할 수 있도록 jquery로 제한
//                 servlet에서도 확장자 봐서 사진만 저장하도록 조건 걸기
//              2. 사진파일 용량 줄이기 thumbnail jar 이용
//              3. 파일이름 변경하여 다른 이름으로 저장
//              4. 1번에 저장한 원본파일 삭제

                
              String folder = makeYearFolder();  // 년도별 폴더만들기 
              
              File savedFile = new File(folder, fileName);   // 첨부파일을 저장합니다.
              fileItem.write(savedFile);                    // 저장 했음.

              UUID uid = UUID.randomUUID();
              String strname = uid.toString().replace("-", "").toUpperCase();

              Thumbnails.of(savedFile).size(width, height).toFile(folder + "/" + strname + suffix);
              
              savedFile.delete();    // 4. 1번에 저장한 원본파일 삭제
              
              sqlMap.update("picture_upload", map);

              }  // 사진파일만 저장
          }
        }
      }  // end while


    }   // isMultipart

    HashMap returnMap = new HashMap();
    returnMap.put("result", "ok");

    return returnMap;
  }




  /**
   * 사진파일 삭제하기 
   */
  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashMap delete(String data,@Context HttpServletRequest request) throws Exception {

    String json = data;

    String alertMsg = "";
    String picture_id = null;
    if(!"".equals(json)) {

      JsonParser parser = new JsonParser();
      JsonObject obj = parser.parse(json).getAsJsonObject();
      picture_id = obj.get("picture_id").getAsString();
    }

    File child = new File(request.getRealPath("/"),"/WEB-INF/config/setting.xml");
    prop.loadFromXML(new FileInputStream (child));

    realDir = (String) prop.get("USER_PHOTO_ABSOLUTEPATH");

    File newFile = new File(realDir + "/"+ picture_id + ".gif");
    if(newFile.exists()) {
      newFile.delete();
      alertMsg = "ok";
      System.out.println("파일삭제...........................................");
    }

    HashMap resultMap = new HashMap();
    resultMap.put("alertMsg", alertMsg);

    return resultMap;
  }

  
  /**
   * 년도 폴더 만들기 
   */
  private String makeYearFolder() {
    
    String year = "";
    String folder = "";

    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
    year = sdf.format(cal.getTime());

    File f = new File(realDir, year);

    if(!f.isDirectory()) {
      f.mkdir();  // 폴더생성
    }
    return f.getAbsolutePath().toString();
  }


  
}
