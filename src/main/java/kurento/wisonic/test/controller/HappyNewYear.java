package kurento.wisonic.test.controller;

import com.jfinal.core.Controller;

/**
 * @author 杨金鑫
 */
public class HappyNewYear extends Controller{
    public void presenter() {
        render("presenter.html");
    }
    public void viewers() {
        render("viewers.html");
    }
}
