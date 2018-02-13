package kurento.wisonic.test.controller;

import com.jfinal.core.Controller;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 木数难数
 */
public class Broadcast extends Controller {
    public void index() {
        render("index.html");
    }
}
