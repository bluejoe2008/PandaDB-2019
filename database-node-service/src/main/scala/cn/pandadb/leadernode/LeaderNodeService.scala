package cn.pandadb.leadernode

import cn.pandadb.cluster.ClusterService
import cn.pandadb.configuration.Config
import cn.pandadb.datanode.{DataNodeDriver, SayHello}
import cn.pandadb.util.PandaReplyMsg
import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.netty.{HippoEndpointRef, HippoRpcEnv, HippoRpcEnvFactory}
import net.neoremind.kraps.rpc.{RpcAddress, RpcEnvClientConfig}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration


// do cluster data update
trait LeaderNodeService {
  def sayHello(clusterService: ClusterService): PandaReplyMsg.Value

  //  def createNode(labels: Array[String], properties: Map[String, Any]): PandaReplyMsg.Value

  //  def addNodeLabel(id: Long, label: String): Int
  //
  //  def getNodeById(id: Long): Int
  //
  //  def getNodesByProperty(label: String, propertiesMap: Map[String, Object]): Int
  //
  //  def getNodesByLabel(label: String): Int
  //
  //  def updateNodeProperty(id: Long, propertiesMap: Map[String, Any]): Int
  //
  //  def updateNodeLabel(id: Long, toDeleteLabel: String, newLabel: String): Int
  //
  //  def deleteNode(id: Long): Int
  //
  //  def removeProperty(id: Long, property: String): Int
  //
  //  def createNodeRelationship(id1: Long, id2: Long, relationship: String, direction: Direction): Int
  //
  //  def getNodeRelationships(id: Long): Int
  //
  //  def deleteNodeRelationship(id: Long, relationship: String, direction: Direction): Int
  //
  //  def getAllDBNodes(chunkSize: Int): Int
  //
  //  def getAllDBRelationships(chunkSize: Int): Int
}


class LeaderNodeServiceImpl() extends LeaderNodeService {
  val dataNodeDriver = new DataNodeDriver

  // leader node services
  override def sayHello(clusterService: ClusterService): PandaReplyMsg.Value = {
    // begin cluster transaction
    //TODO: begin leader node's transaction
    val res = sendSayHelloCommandToAllNodes(clusterService)

    //TODO: close leader node's transaction
    if (res == PandaReplyMsg.LEAD_NODE_SUCCESS) {
      PandaReplyMsg.LEAD_NODE_SUCCESS
    } else {
      PandaReplyMsg.LEAD_NODE_FAILED
    }
  }


  private def sendSayHelloCommandToAllNodes(clusterService: ClusterService): PandaReplyMsg.Value = {
    val leaderNode = clusterService.getLeaderNode()
    val dataNodes = clusterService.getDataNodes()
    val config = new Config()
    val clientConfig = RpcEnvClientConfig(new RpcConf(), "panda-client")
    val clientRpcEnv = HippoRpcEnvFactory.create(clientConfig)
    val allEndpointRefs = ArrayBuffer[HippoEndpointRef]()
    dataNodes.map(s => {
      val strs = s.split(":")
      val address = strs(0)
      val port = strs(1).toInt
      val ref = clientRpcEnv.setupEndpointRef(new RpcAddress(address, port), config.getDataNodeEndpointName())
      allEndpointRefs += ref
    })
    val refNumber = allEndpointRefs.size

    // send command to all data nodes
    var countReplyRef = 0
    allEndpointRefs.par.foreach(endpointRef => {
      val res = dataNodeDriver.sayHello("hello", endpointRef, Duration.Inf)
      if (res == PandaReplyMsg.SUCCESS) {
        countReplyRef += 1
      }
    })
    println(refNumber, countReplyRef)
    clientRpcEnv.shutdown()
    if (countReplyRef == refNumber) {
      PandaReplyMsg.LEAD_NODE_SUCCESS
    } else {
      PandaReplyMsg.LEAD_NODE_FAILED
    }
  }
}
