import java.util.ArrayList

import akka.actor.ActorRef

import scala.collection.mutable.ArrayBuffer

/**
 * Created by gokul on 10/25/15.
 */

class FingerRow {
  var start: Long = 0L  // identifier of the node
  var intervalStart: Long = 0L       // identifier of the interval start
  var intervalEnd: Long = 0L         // identifier of the interval end
  var successor: Long = 0L   // successor of the identifier
  var node:ActorRef = null;  // node reference to node corresponding to successor

  def apply(inIdentifier: Long, inStart: Long, inEnd: Long, inSuccessor: Long) {
    start = inIdentifier;
    intervalStart = inStart;
    intervalEnd = inEnd;
    successor = inSuccessor;

  }

  def assignSuccessor(inSuccessor: Long) {
    successor = inSuccessor;
  }
}


