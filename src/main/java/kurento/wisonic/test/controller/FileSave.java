package kurento.wisonic.test.controller;

import com.jfinal.core.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.List;

/**
 * @author 木数难数
 */
public class FileSave extends Controller {

    public void index() throws Exception {
        HttpServletResponse response = getResponse();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");

        HttpServletRequest request = getRequest();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            System.out.println(key + " : " + value);
        }
        System.out.println("Method : " + request.getMethod());

        InputStream in = request.getInputStream();

        String path = "C:/Users/Administrator/Desktop/mp4.mp4";
        OutputStream out = new FileOutputStream(path);

        int c = 0;
        while ((c = in.read()) != -1) {
            out.write(c);
            out.flush();
        }
        in.close();
        out.close();

        renderNull();
    }

}
