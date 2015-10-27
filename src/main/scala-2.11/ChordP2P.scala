import akka.actor.{Props, ActorSystem}

/**
 * Created by gokul on 10/24/15.
 */
object ChordP2P extends App {
  override def main (args: Array[String]) {
    if(args.length != 2){
      println("Usage: run numNodes numRequests");
      System.exit(1); //Exit due to incorrect parameters
    }

    var numNodes:Int = args(0).toInt;
    var numRequests:Int = args(1).toInt;

    if (numNodes <= 0 || numRequests <= 0) {
      println("Nothing to do!");
      System.exit(2); //NumNodes or numRequests are invalid
    }

    val system = ActorSystem("ChordP2P");
    val chordAdmin = system.actorOf(Props(classOf[Admin], numNodes, numRequests), name = "admin");

    chordAdmin ! StartSystem;
  }

}
