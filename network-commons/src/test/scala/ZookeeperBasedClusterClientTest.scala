import java.io.File

import cn.pandadb.network.{NodeAddress, ZKConstants, ZookeeperBasedClusterClient}
import cn.pandadb.server.ZKServiceRegistry
import org.junit.runners.MethodSorters
import org.junit.{Assert, FixMethodOrder, Test}
import org.neo4j.kernel.configuration.Config

/**
  * @Author: Airzihao
  * @Description:
  * @Date: Created at 10:32 2019/11/27
  * @Modified By:
  */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ZookeeperBasedClusterClientTest {

  val configFile = new File(this.getClass.getClassLoader.getResource("test_pnode0.conf").getPath)
  val zkConstants = ZKConstants

  val zkString = zkConstants.zkServerAddress

  val clusterClient = new ZookeeperBasedClusterClient(zkString)
  val register = new ZKServiceRegistry(zkString)

  // empty at first
  @Test
  def test1(): Unit = {
    Assert.assertEquals(true, clusterClient.getAllNodes().isEmpty)
  }

  // getAllNodes, will get test node
  @Test
  def test2(): Unit = {
    register.registerAsOrdinaryNode(NodeAddress.fromString(zkConstants.localNodeAddress))
    Thread.sleep(1000)
    Assert.assertEquals(false, clusterClient.getAllNodes().isEmpty)
    Assert.assertEquals(NodeAddress.fromString("10.0.88.11:1111"), clusterClient.getAllNodes().iterator.next())
  }

  // empty after test node unRegister itself
  @Test
  def test3(): Unit = {
    register.unRegisterOrdinaryNode(NodeAddress.fromString(zkConstants.localNodeAddress))
    Assert.assertEquals(true, clusterClient.getAllNodes().isEmpty)
  }

  // test leader
  @Test
  def test4(): Unit = {
    register.registerAsLeader(NodeAddress.fromString(zkConstants.localNodeAddress))
    Thread.sleep(1000)
    Assert.assertEquals(NodeAddress.fromString("10.0.88.11:1111"), clusterClient.getWriteMasterNode("").get)
  }

}
