import akka.actor.ActorRef

/**
 * Created by gokul on 10/24/15.
 */
case object StartSystem;
case object GetNodeObject;
case object Join;
case class UpdateSuccessor(inputID:Long, nodeRef:ActorRef);
case class Join(nodeRef: ActorRef);
case class UpdateFingerTable(inputID:Long, nodeRef:ActorRef, i:Int, startRef:ActorRef);
case class FindSuccessor(inputID:Long);
case class FindPredecessor(inputID:Long)
case class ClosestPrecedingFinger(inputID:Long)
case class TestMessage(ident: Long, hops: Long)
case class Hops(hops:Long)