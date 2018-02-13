package kurento.wisonic.test.handle;

import com.jfinal.handler.Handler;
import com.jfinal.kit.StrKit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

/**
 * @author 木数难数
 */
public class WebSocketHandle extends Handler {

    private Pattern filterUrlRegxPattern;

    public WebSocketHandle(String filterUrlRegx) {
        if (StrKit.isBlank(filterUrlRegx)) {
            throw new IllegalArgumentException("The para filterUrlRegx can not be blank.");
        }
        filterUrlRegxPattern = Pattern.compile(filterUrlRegx);
    }

    @Override
    public void handle(String s, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, boolean[] booleans) {
        boolean flag = filterUrlRegxPattern.matcher(s).find();
        System.out.println(s);
        if (!flag) {
            next.handle(s, httpServletRequest, httpServletResponse, booleans);
        }
    }
}
