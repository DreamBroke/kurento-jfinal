package kurento.wisonic.test.controller;

import com.jfinal.core.Controller;

/**
 * @author 木数难数
 */
public class Hello extends Controller {
    public void index() {
        render("/index.jsp");
    }
}
