## Getting Started with the ZooKeeper API
### Setting the ZooKeeper CLASSPATH
We need to set up the appropriate classpath to run and compile ZooKeeper Java code. ZooKeeper uses a number of third-party libraries in addition to the ZooKeeper JAR file. To make typing a little easier and to make the text a little more readable, we will use an environment variable `CLASSPATH` with all the required libraries. The script *zkEnv.sh* in the *bin* directory of the ZooKeeper distribution sets this environment variable for us. We need to source it using the following:
```
ZOOBINDIR="<path_to_distro>/bin"
. "$ZOOBINDIR"/zkEnv.sh
```
Once we run this script, the `CLASSPATH` variable will be set correctly. We will use it to compile and run our Java programs.

### Creating a ZooKeeper Session
The constructor that creates a ZooKeeper handle usually looks like this:
```
ZooKeeper(
     String connectString,
     int sessionTimeout,
     Watcher watcher)
```
where:

- `connectString`

  Contains the hostnames and ports of the ZooKeeper servers. We listed those servers when we used zkCli to connect to the ZooKeeper service.

- `sessionTimeout`

  The time in milliseconds that ZooKeeper waits without hearing from the client before declaring the session dead. For now, we will just use a value of 15000, for 15 seconds.

- `watcher`

  An object we need to create that will receive session events. Because `Watcher` is an interface, we will need to implement a class and then instantiate it to pass an instance to the ZooKeeper constructor. Clients use the `Watcher` interface to monitor the health of the session with ZooKeeper. Events will be generated when a connection is established or lost to a ZooKeeper sever. They can also be used to monitor changes to ZooKeeper data. Finally, if a session with ZooKeeper expires, an event is delivered through the `Watcher` interface to notify the client application.

#### Implementing a Watcher
To receive notifications from ZooKeeper, we need to implement watchers. Let's look at a bit more closely at the `Watcher` interface. It has the following declaration:
```
public interface Watcher {
	void process(WatchedEvent event);
}
```
Not much of an interface, right? We'll be using it heavily, but for now we will simply print the event. So, let's start our example implementation of the `Master`:
```
import java.io.IOException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class Master implements Watcher {
    ZooKeeper zk;
    String hostPort;

    Master(String hostPort) {
        this.hostPort = hostPort; (1)
    }

    void startZK() throws IOException {
        zk = new ZooKeeper(hostPort, 15000, this); (2)
    }

    public void process(WatchedEvent e) {
        System.out.println(e); (3)
    }

    public static void main(String args[])
        throws Exception {
        Master m = new Master(args[0]);

        m.startZK();

        // wait for a bit
        Thread.sleep(60000); (4)
    }
}
```

(3) This simple example does not have complex event handling. Instead, we will simply print out the event that we receive.

(4) Once we have connected to ZooKeeper, there will be a background thread that will maintain the ZooKeeper session. This thread is a daemon thread, which means that the program may exit even if the thread is still active. Here we sleep for a bit so that we can see some events come in before the program exits.

We can compile this simple example like so:
```
$ javac  -cp $CLASSPATH Master.java
```
Once we have compiled *Master.java*, we run it and see the following:
```
$ java  -cp $CLASSPATH Master 127.0.0.1:2181
... - INFO  [...] - Client environment:zookeeper.version=3.4.5-1392090, ... (1)
...
... - INFO  [...] - Initiating client connection,
connectString=127.0.0.1:2181 ... (2)
... - INFO  [...] - Opening socket connection to server
localhost/127.0.0.1:2181. ...
... - INFO  [...] - Socket connection established to localhost/127.0.0.1:2181,
initiating session
... - INFO  [...] - Session establishment complete on server
localhost/127.0.0.1:2181, ... (3)
WatchedEvent state:SyncConnected type:None path:null (4)
```
The ZooKeeper client API produces various log messages to help the user understand what is happening. The logging is rather verbose and can be disabled using configuration files, but these messages are invaluable during development and even more invaluable if something unexpected happens after deployment.

(1) The first few log messages describe the ZooKeeper client implementation and environment.

(2) These log messages will be generated whenever a client initiates a connection to a ZooKeeper server. This may be the initial connection or subsequent reconnections.

(3) This message shows information about the connection after it has been established. It shows the host and port that the client connected to and the actual session timeout that was negotiated. If the requested session timeout is too short to be detected by the server or too long, the server will adjust the session timeout.

(4) This line did not come from the ZooKeeper library; it is the `WatchEvent` object that we print in our implementation of `Watcher.process(WatchedEvent e)`.

### Getting Mastership
#### Getting Mastership Asynchronously
In ZooKeepe, all synchronous calls have corresponding asynchronous calls. This allows us to issue many calls at a time from a single thread and may also simplify our implementation. Let’s revisit the mastership example, this time using asynchronous calls.

Here is the asynchronous version of `create`:
```
void create(String path,
    byte[] data,
    List<ACL> acl,
    CreateMode createMode,
    AsyncCallback.StringCallback cb, (1)
    Object ctx) (2)
```
This version of `create` looks exactly like the synchronous version except for two additional parameters:

(1) An object containing the function that serves as the callback

(2) A user-specified context (an object that will be passed through to the callback when it is invoked)

This call will return immediately, usually before the create request has been sent to the server. The callback object often takes data, which we can pass through the context argument. When the result of the `create` request is received from the server, the context argument will be given to the application through the callback object.

The callback object implements `StringCallback` with one method:
```
void processResult(int rc, String path, Object ctx, String name)
```
The parameters of `processResult` have the following meanings:
- `rc`

  Returns the result of the call, which will be `OK` or a code corresponding to a `KeeperException`

- `path`

  The path that we passed to the `create`

- `ctx`

  Whatever context we passed to the `create`

- `name`

  The name of the znode that was created


So, let's start writing our master functionality. Here, we create a `masterCreateCallback` object that will receive the results of the `create` call:

```
    static boolean isLeader;

    static StringCallback masterCreateCallback = new StringCallback() {
        void processResult(int rc, String path, Object ctx, String name) {
            switch(Code.get(rc)) { (1)
            case CONNECTIONLOSS: (2)
                checkMaster();
                return;
            case OK: (3)
                isLeader = true;
                break;
            default: (4)
                isLeader = false;
            }
            System.out.println("I'm " + (isLeader ? "" : "not ") +
                               "the leader");
        }
    };

    void runForMaster() {
        zk.create("/master", serverId.getBytes(), OPEN_ACL_UNSAFE,
                  CreateMode.EPHEMERAL, masterCreateCallback, null); (5)
    }
```
(1) We get the result of the `create` call in `rc` and convert it to a `Code` enum to switch on. `rc` corresponds to a `KeeperException` if `rc` is not zero.

(2) If the `create` fails due to a connection loss, we will get the `CONNECTIONLOSS` result code rather than the `ConnectionLossException`. When we get a connection loss, we need to check on the state of the system and figure out what we need to do to recover. We do this in the `checkMaster` method, which we will implement next.

(3) Woo-hoo! We are the leader. For now, we will just set `isLeader` to `true`.

(4) If any other problem happened, we did not become the leader.

(5) We kick things off in `runForMaster` when we pass the `masterCreateCallback` object to the `create` method. We pass `null` as the context object because there isn’t any information that we need to pass from `runForMaster` to the `masterCreateCallback.processResult` method.

## Running ZooKeeper
### Configuring a ZooKeeper Server
Each ZooKeeper server takes options from a configuration file named *zoo.cfg* when it starts. Servers that play similar roles and have the same basic setup can share a file. The *myid* file in the *data* directory distinguishes servers from each other. Each *data* directory must be unique to the server anyway, so it is a convenient place to put the distinguishing file. The server ID contained in the *myid* file serves as an index into the configuration file, so that a particular ZooKeeper server can know how it should configure itself.

### Reconfiguration
Reconfiguration allows us to change not only the members of the ensemble, but also their network parameters. Because the configuration can change, ZooKeeper needs to move the reconfigurable parameters out of the static configuration file and into a configuration file that will be updated automatically. The **dynamicConfigFile** parameter links the two files together.

Let's take an example configuration file that we have been using before dynamic configuration:
```
tickTime=2000
initLimit=10
syncLimit=5
dataDir=./data
dataLogDir=./txnlog
clientPort=2182
server.1=127.0.0.1:2222:2223
server.2=127.0.0.1:3333:3334
server.3=127.0.0.1:4444:4445
```
and changes it to a configuration file that supports dynamic configuration:
```
tickTime=2000
initLimit=10
syncLimit=5
dataDir=./data
dataLogDir=./txnlog
dynamicConfigFile=./dyn.cfg
```
Notice that we have even removed the **ckientPort** parameter from the configuration file. The *dyn.cfg* file is now going to be made up of just the server entries. We are adding a bit of information, though. Now each entry will have the form:
```
server.id=host:n:n[:role];[client_address:]client_port
```
Just as in the normal configuration file, the hostname and ports used for quorum and leader election messages are listed for each server. The **role** must be either *participant* or **observer**. If **role** is omitted, **participant** is the default. We also specify the **client_port** (the server port to which clients will connect) and optionally the address of a specific interface on that server. Because we removed **clientPort** from the static config file, we need to add it here.

So now our *dyn.cfg* looks like this:
```
server.1=127.0.0.1:2222:2223:participant;2181
server.2=127.0.0.1:3333:3334:participant;2182
server.3=127.0.0.1:4444:4445:participant;2183
```
