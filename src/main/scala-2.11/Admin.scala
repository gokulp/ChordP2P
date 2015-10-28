import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.util.Timeout

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import akka.pattern.{ ask, pipe };
/**
 * Created by gokul on 10/24/15.
 */
class Admin(numNodes:Int, numRequests:Int) extends Actor{
  var nodes = numNodes;
  var requests = numRequests;
  var base:Int = 32 - Integer.numberOfLeadingZeros(numNodes);
  var nodeRef = new ArrayBuffer[ActorRef]()
  var nodeBuf = new ArrayBuffer[Node]()
  def createTopology() ={
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));
    nodeRef += context.actorOf(Props(classOf[Node], base, 0), name = "node"+0)
    println("Node Created with "+nodeRef(0))
    var future: Future[Int] = ask(nodeRef(0), Join).mapTo[Int];
    var p:Int = Await.result(future, timeout.duration);
    println("Initial node activated with code "+p)
    //fingerTable += new FingerRow();

    for (i <- 1 until numNodes){
      nodeRef += context.actorOf(Props(classOf[Node], base, i), name = "node"+i)
      var future2: Future[Int] = ask(nodeRef(i), Join(nodeRef(i-1))).mapTo[Int];
      var p:Int = Await.result(future2, timeout.duration);
      //println("Node "+i+" Joined with code "+p)
    }
    println("Topology complete!")
  }
  override def receive = {
    case Hops(hops:Long) =>
      println("Number hops for "+sender+ " were "+hops )
    case StartSystem =>
      createTopology()
      println("base is "+base + " max nodes allowed is "+ Math.pow(2, base).toLong)
      nodeRef(7) ! TestMessage(5, 1)
      //context.system.shutdown();
      //System.exit(0)
/*
    case _ =>
      println("Recieved unknown message!")
*/
  }
}
