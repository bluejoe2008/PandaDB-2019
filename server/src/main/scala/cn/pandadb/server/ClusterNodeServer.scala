package cn.pandadb.server

import cn.pandadb.cluster.ClusterService
import cn.pandadb.configuration.Config
import cn.pandadb.leadernode.LeaderNodeDriver
import cn.pandadb.server.Store.DataStore
import cn.pandadb.server.modules.LifecycleServerModule
import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.netty.HippoRpcEnvFactory
import net.neoremind.kraps.rpc.{RpcAddress, RpcEnvClientConfig}
import org.apache.curator.shaded.com.google.common.net.HostAndPort

class ClusterNodeServer(config: Config, clusterService: ClusterService, dataStore: DataStore)
      extends LifecycleServerModule{
  val logger = config.getLogger(this.getClass)
  val nodeHostAndPort = HostAndPort.fromString(config.getNodeAddress())

  override def start(): Unit = {
    logger.info(this.getClass + ": start")

    if (clusterService.hasLeaderNode()) {
      val leaderNode = clusterService.getLeaderNodeHostAndPort()
      syncDataFromCluster(leaderNode)
    }
    clusterService.registerAsFreshNode()
    while (!clusterService.hasLeaderNode()) {
      participateInLeaderElection()
    }
  }

  def syncDataFromCluster(leaderNode: HostAndPort): Unit = {
    logger.info("syncDataFromCluster")
      val dbDir = dataStore.databaseDirectory

      val rpcServerName = config.getRpcServerName()
      val LeaderNodeEndpointName = config.getLeaderNodeEndpointName()
      val clientConfig = RpcEnvClientConfig(new RpcConf(), rpcServerName)
      val clientRpcEnv = HippoRpcEnvFactory.create(clientConfig)
      val leaderNodeEndpointRef = clientRpcEnv.setupEndpointRef(
        new RpcAddress(leaderNode.getHostText, leaderNode.getPort), LeaderNodeEndpointName)
      val leaderNodeDriver = new LeaderNodeDriver
//      leaderNodeDriver.pullDbFileFromDataNode()
      logger.info(s"syncDataFromCluster: pull data <fromVersion: ${dataStore.getDataVersion()}> to dir <$dbDir>")
      logger.info("update local data version")
      dataStore.setDataVersion(0)
  }

  def participateInLeaderElection(): Unit = {
    logger.info(this.getClass + "participateInLeaderElection: " )
    val localDataVersion = dataStore.getDataVersion()
    val clusterDataVersion = clusterService.getDataVersion().toLong
    if (localDataVersion >= clusterDataVersion) {
      // try register leader
      // add leader lister handler function

    }
  }
}
