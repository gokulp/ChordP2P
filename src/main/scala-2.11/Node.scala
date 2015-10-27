import akka.actor.{Actor, ActorRef};
import akka.pattern.Patterns;
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global;
import scala.concurrent.Future;
import scala.concurrent.Await;
import scala.concurrent.Promise;
import akka.util.Timeout
import akka.pattern.{ ask, pipe };

import scala.concurrent.duration.Duration
;
/**
 * Created by gokul on 10/24/15.
 */
class Node (base:Int, id:Int) extends Actor {


  var admin:ActorRef = null;
  //Finger Table: ArrayList of vectors
  var fingerTable = new ArrayBuffer[FingerRow]();
  var successor:Long = 0L;
  var successorRef:ActorRef = null;
  var predecessor:Long = 0L;
  var predecessorRef:ActorRef= null;
  var identifier:Long = id;
  apply(base, identifier)

  def apply(base:Int,inId:Long): Unit ={
    //fingerTable.apply(inId, base)
    var tempID:Long = identifier;
    var mod = Math.pow(2, base).toLong;
    for(i <- 0 until base - 1){
      fingerTable += new FingerRow();
      tempID = (identifier + Math.pow(2, i).toLong)%mod;
      var end:Long = (identifier + Math.pow(2, i+1).toLong)%mod;
      fingerTable(i).apply(tempID, tempID, end, 0);
    }
    fingerTable += new FingerRow();
    tempID = (identifier + Math.pow(2, base - 1).toLong)%mod;
    fingerTable(base - 1).apply(tempID, tempID, identifier, 0);
    //Think hard on this
    //     cases: 1. First Node Joining Network
    //            2. Node Joining in existing network
    successor = identifier;
    fingerTable(0).successor = successor;
    successorRef = self;
    predecessor = identifier;
    predecessorRef = self;
  }

  //sucessor is finger[0].node


  def checkRange(start:Long, end:Long, inputID:Long): Boolean ={
    if (/*(start == end) ||*/ (start < end && start < inputID && inputID < end) ||
      (start > end && (inputID == 0 || (inputID > start && inputID > end) || (inputID < start && inputID < end)))) {
      return true
    } else {
      return false
    }
  }
  def checkRangeStartInclusive(start:Long, end:Long, inputID:Long): Boolean ={
    if (/*(start == end) || */(start < end && start <= inputID && inputID < end) ||
      (start > end && (inputID == 0 || (inputID >= start && inputID > end) || (inputID <= start && inputID < end)))) {
      return true
    } else {
      return false
    }
  }
  def checkRangeEndInclusive(start:Long, end:Long, inputID:Long): Boolean ={
    if (/*(start == end) || */(start < end && start < inputID && inputID <= end) ||
      (start > end && (inputID == 0 || (inputID > start && inputID >= end) || (inputID < start && inputID <= end)))) {
      return true
    } else {
      return false
    }
  }

  def checkRangeBothInclusive(start:Long, end:Long, inputID:Long): Boolean ={
    if (/*(start == end) || */(start < end && start <= inputID && inputID <= end) ||
      (start > end && (inputID == 0 || (inputID >= start && inputID >= end) || (inputID <= start && inputID <= end)))) {
      return true
    } else {
      return false
    }
  }

  //find_successor
  // n' = find_predecessor(id)
  // return(n'.successor)

  def find_successor(inputID:Long): ActorRef = {
    //println("In find_successor of identifier "+ identifier)
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));
    //var future: Future[ActorRef] = ask(self, FindPredecessor(inputID)).mapTo[ActorRef];
    //var result:ActorRef = Await.result(future, timeout.duration);
    var result:ActorRef = find_predecessor(inputID)
    //println("Node Retrieved")
    var node:Node = this;
    if (result != self){
      var future2: Future[Node] = ask(result, GetNodeObject).mapTo[Node];
      node = Await.result(future2, timeout.duration);
    }
    //println("Sending back node.successorRef: "+successorRef)
    return(node.successorRef)
  }

  //ask node n to find id's predecessor
  //  n.find_predecessor(id)
  //    n' = n
  //    while(id not in (n', n'.successor] )
  //      n' = n'.closest_preceding_finger(id)
  //    return(n')

  def find_predecessor(inputID:Long): ActorRef ={
    //println("In find_predecessor of identifier "+identifier)
    var node:ActorRef = null;
    var nprime = this
    var nprimeRef = self
    //println(nprime.identifier+ " "+nprime.successor+" "+inputID)
    if ( checkRangeEndInclusive(nprime.identifier, nprime.successor, inputID) ){
      return(self)
    } else {
      return(closest_preceding_finger(inputID))
    }
    return(self)
  }

  //return closest finger preceding id
  //  n.closest_preceding_finger(id)
  //    for i = m downto i
  //    if (finger[i].node E (n,id))
  //       return(finger[i].node)
  //    return(n);

  def closest_preceding_finger(inputID:Long): ActorRef = {
    //var flag = true
    //println("In closest_preceding_finger")
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));
    //var future:Future[Int] = ask(actor, inputID, timeout).toInt;
    for(i <- fingerTable.size - 1 to 0 by -1){
      println("value :"+i+" identifier:"+identifier+" inputID:"+inputID);
      var value = fingerTable(i).successor
      if (checkRange(identifier, inputID, value)) {
        //flag = false
        //println(start+" "+ end + " "+ inputID +"Reducing!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        var future: Future[ActorRef] = ask(fingerTable(i).node, FindPredecessor(inputID)).mapTo[ActorRef] //ask(fingerTable(i).node, inputID, timeout).toInt;
        var result:ActorRef = Await.result(future, timeout.duration)
        //pipe(future) to sender()
        return(result);
      }
    }
    //if (flag)
    return(self) // if no entry is found return self
  }

  //node n joins the network
  // n' is an arbitrary node in the network
  // n.join(n')
  //    if (n')
  //       init_finger_table(n')
  //       update_others();
  //       //make keys in (predecessor,n] from successor
  //    else //n is the only node in the network
  //       for i = 1 to m
  //          finger[i].node = n;
  //       predecessor = n;
  def join(nprimeRef:ActorRef) ={
    if (nprimeRef != null){
      initFingerTable(nprimeRef)
      update_others(nprimeRef, self)
    } else{
      for(i <- 0 until base ){
        fingerTable(i).node = self;
      }
      predecessor = identifier;
      predecessorRef = self;
    }
  }

  //initialize finger table of local node
  //n' is an arbitrary node already in the network
  //   n.init_finger_table(n')
  //      finger[1].node = n'.find_successor(finger[1].start)
  //      predecessor = successor.predecessor;
  //      successor.predecessor = n;
  //      for i = 1 to m-1
  //          if (finger[i+1].start E [n, finger[i].start);
  //             finger[i+1].node = finger[i].node;
  //          else
  //             finger[i+1].node = n'.find_successor(finger[i+1].start)

  def initFingerTable(nprimeRef:ActorRef): Unit ={
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));

    //println("Find for inputID : "+fingerTable(0).start)
    var future2: Future[ActorRef] = ask(nprimeRef, FindSuccessor(fingerTable(0).start)).mapTo[ActorRef];
    successorRef = Await.result(future2, timeout.duration);
    fingerTable(0).node = successorRef

    var future: Future[Node] = ask(successorRef, GetNodeObject).mapTo[Node];
    var successorObj:Node = Await.result(future, timeout.duration);
    predecessor = successorObj.predecessor
    predecessorRef = successorObj.predecessorRef

    var future3: Future[Int] = ask(successorRef, UpdateSuccessor(identifier, self)).mapTo[Int];
    var dummy = Await.result(future3, timeout.duration);
    successor = successorObj.identifier
    fingerTable(0).successor = successor
    
    for (i <- 0 until base - 1 ){
      if (checkRangeStartInclusive(identifier, fingerTable(i).successor, fingerTable(i+1).start)){
        fingerTable(i+1).node = fingerTable(i).node
      } else {
        var future3: Future[ActorRef] = ask(nprimeRef, FindSuccessor(fingerTable(i+1).start)).mapTo[ActorRef];
        fingerTable(i+1).node = Await.result(future3, timeout.duration);
      }
    }
  }

  //update all nodes whose finger tables should refer to n
  //n.update_others():
  //    for i = 1 to m
  //       //find last node p whose ith finger might be n
  //       p = find.predecessor(n - 2^(i-1))
  //       p.update_finger_table(n,i)
  def update_others(node:ActorRef, nodeRef:ActorRef) = {
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));
    for (i <- 0 until base){
      var index = identifier - Math.pow(2, base - i).toLong
      if (index < 0)
        index = Math.pow(2, base).toLong - Math.abs(index)%Math.pow(2, base).toLong;
      var future: Future[ActorRef] = ask(node, FindPredecessor(index)).mapTo[ActorRef];
      var p:ActorRef = Await.result(future, timeout.duration);

      var future2: Future[Int] = ask(p, UpdateFingerTable(identifier, self, i, nodeRef)).mapTo[Int];
      var p1:Int = Await.result(future2, timeout.duration);
    }
    //println("Going out")
  }

  //if s is ith finger of n, update n's finger table with s
  //n.update_finger_table(s,i):
  //   if(s E [n, finger[i].node) ):
  //     finger[i].node = s;
  //     p = predecessor;  // get first node preceding n
  //     p.update_finger_table(s,i)

  def update_finger_table(inputID:Long, nodeRef:ActorRef, i:Int, startRef:ActorRef):Unit ={
    //println("in Update finger table with identifier "+ identifier)
    implicit val timeout = new Timeout(Duration.create(100, "seconds"));
    if (checkRangeStartInclusive(identifier, fingerTable(i).successor, inputID)){
      fingerTable(i).node = nodeRef;
      if (predecessorRef == startRef)
        return
      //println(predecessorRef)
      var future2: Future[Int] = ask(predecessorRef, UpdateFingerTable(inputID, nodeRef, i, startRef)).mapTo[Int];
      var p:Int = Await.result(future2, timeout.duration);
    }
    //println("Going out")
  }

  override def receive = {
    case Join =>
      println("First Node being joined");
      admin = sender()
      join(null);
      sender ! 1
    case Join(nodeRef:ActorRef) =>
      println("Joining node with "+nodeRef)
      admin = sender()
      join(nodeRef)
      sender ! 1
    case UpdateSuccessor(inputID:Long, nodeRef:ActorRef) =>
      predecessor = inputID;
      predecessorRef = nodeRef;
      sender ! 1
      //sender ! this
    case GetNodeObject =>
      sender ! this
    case FindSuccessor(inputID:Long) =>
      //println("Recieved call for inputID"+inputID+" My identifier is "+identifier)
      var nprime:ActorRef = find_successor(inputID)
      //println("Returning to calling process!!!!!!!!!!!!!")
      sender ! nprime
    case FindPredecessor(inputID:Long) =>
      var nodeRef:ActorRef = find_predecessor(inputID)
      //println("Returning reference "+ nodeRef)
      sender ! nodeRef
    case ClosestPrecedingFinger(inputID:Long) =>
      closest_preceding_finger(inputID)
    case UpdateFingerTable(inputID:Long,nodeRef:ActorRef, i:Int, startRef:ActorRef) =>
      update_finger_table(inputID, nodeRef, i, startRef);
      sender ! 1
    case TestMessage(ident:Long, hopes:Long) =>
      implicit val timeout = new Timeout(Duration.create(100, "seconds"));
      if (sender != self) {
        if (ident == identifier) {
          println("Returning from "+self)
          admin ! hopes
          sender ! self
        } else {
          println("In node " + identifier)
          for (i <- base - 1 to 0 by -1) {
            if (checkRange(fingerTable(i).intervalStart, fingerTable(i).intervalEnd, ident)) {
              println("index " + i + " Called")
              var future: Future[ActorRef] = ask(fingerTable(i).node, TestMessage(ident, hopes + 1)).mapTo[ActorRef];
              var p:ActorRef = Await.result(future, timeout.duration);
              println("Returned from "+ self)
              sender ! p
              //fingerTable(i).node ! TestMessage(ident, hopes + 1)
            }
          }
        }
      }
    case _ =>
      println("Recieved unknown message!")
  }
}

