# zookeeperAdmin
## Administration
### Configuration Parameters
#### Communication using the Netty framework
Netty is an NIO based client/server communication framework, it simplifies (over NIO being used directly) many of the features of network level communication for java applications. Additionally the Netty framework has built in support for encryption (SLL) and authentication (certificates). These are optional features and can be turned on or off individually.

In version 3.5+, a ZooKeeper server can use Netty instead of NIO (default option) by setting the environment variable **zookeeper.serverCnxnFactory** to **org.apache.zookeeper.server.NettyServerCnxnFactory**; for the client, set **zookeeper.clientCnxnSocket** to **org.apache.zookeeper.ClientCnxnSocketNetty**.
