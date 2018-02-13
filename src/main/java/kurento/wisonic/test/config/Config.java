package kurento.wisonic.test.config;

import com.jfinal.config.*;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.template.Engine;
import kurento.wisonic.test.controller.*;
import kurento.wisonic.test.handle.WebSocketHandle;
import kurento.wisonic.test.model.Kurento;
import kurento.wisonic.test.model.Viewers;

/**
 * @author 木数难数
 */
public class Config extends JFinalConfig{

    @Override
    public void configConstant(Constants constants) {
        constants.setDevMode(true);
    }

    @Override
    public void configRoute(Routes routes) {
        routes.add("/", Hello.class);
        routes.add("/hello-world", HelloWorld.class);
        routes.add("/one2many", One2Many.class);
        routes.add("/one2one", One2One.class);
        routes.add("/recorder", Recorder.class);
        routes.add("/group", Group.class);
        routes.add("/screen-sharing", ScreenSharing.class);
        routes.add("/mixing", Mixing.class);
        routes.add("/broadcast", Broadcast.class);
        routes.add("/file-save", FileSave.class);
        routes.add("/broadcast-http", BroadcastHttp.class);
        routes.add("/broadcast-js", BroadcastJs.class);
        routes.add("/happy-new-year", HappyNewYear.class);
        routes.add("/ios", Ios.class);
    }

    @Override
    public void configEngine(Engine engine) {

    }

    @Override
    public void configPlugin(Plugins plugins) {
        DruidPlugin dp = new DruidPlugin("jdbc:mysql://localhost:20001/kurento", "root", "123456", "com.mysql.jdbc.Driver");
        plugins.add(dp);
        ActiveRecordPlugin arp = new ActiveRecordPlugin(dp);
        plugins.add(arp);
        arp.addMapping("kurento", Kurento.class);
        arp.addMapping("viewers", Viewers.class);
    }

    @Override
    public void configInterceptor(Interceptors interceptors) {

    }

    @Override
    public void configHandler(Handlers handlers) {
        handlers.add(new WebSocketHandle("websocket$"));
    }
}
