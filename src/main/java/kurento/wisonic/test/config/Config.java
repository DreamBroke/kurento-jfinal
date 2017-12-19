package kurento.wisonic.test.config;

import com.jfinal.config.*;
import com.jfinal.template.Engine;
import kurento.wisonic.test.controller.HelloWorld;

/**
 * @author 木数难数
 */
public class Config extends JFinalConfig{

    @Override
    public void configConstant(Constants constants) {

    }

    @Override
    public void configRoute(Routes routes) {
        routes.add("/hello_world", HelloWorld.class, "");
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
