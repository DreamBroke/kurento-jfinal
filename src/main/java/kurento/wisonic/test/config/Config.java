package kurento.wisonic.test.config;

import com.jfinal.config.*;
import com.jfinal.template.Engine;
import kurento.wisonic.test.controller.*;

/**
 * @author 木数难数
 */
public class Config extends JFinalConfig{

    @Override
    public void configConstant(Constants constants) {

    }

    @Override
    public void configRoute(Routes routes) {
        routes.add("/hello-world", HelloWorld.class);
        routes.add("/one2many", One2Many.class);
        routes.add("/one2one", One2One.class);
        routes.add("/recorder", Recorder.class);
        routes.add("/group", Group.class);
        routes.add("/screen-sharing", ScreenSharing.class);
    }

    @Override
    public void configEngine(Engine engine) {

    }

    @Override
    public void configPlugin(Plugins plugins) {

    }

    @Override
    public void configInterceptor(Interceptors interceptors) {

    }

    @Override
    public void configHandler(Handlers handlers) {

    }
}
