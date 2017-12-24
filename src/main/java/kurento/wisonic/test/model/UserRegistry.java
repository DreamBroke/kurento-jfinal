/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package kurento.wisonic.test.model;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map of users registered in the system. This class has a concurrent hash map to store users, using
 * its name as key in the map.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.3.1
 */
public class UserRegistry {

  private ConcurrentHashMap<String, UserSessionOne2One> usersByName = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, UserSessionOne2One> usersBySessionId = new ConcurrentHashMap<>();

  public void register(UserSessionOne2One user) {
    usersByName.put(user.getName(), user);
    usersBySessionId.put(user.getSession().getId(), user);
  }

  public UserSessionOne2One getByName(String name) {
    return usersByName.get(name);
  }

  public UserSessionOne2One getBySession(Session session) {
    return usersBySessionId.get(session.getId());
  }

  public boolean exists(String name) {
    return usersByName.keySet().contains(name);
  }

  public UserSessionOne2One removeBySession(Session session) {
    final UserSessionOne2One user = getBySession(session);
    if (user != null) {
      usersByName.remove(user.getName());
      usersBySessionId.remove(session.getId());
    }
    return user;
  }

}
