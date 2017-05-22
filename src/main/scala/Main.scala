//import akka.actor.ActorSystem
//import akka.actor.Props
//import akka.actor.ActorRef
//import akka.actor.Actor
//import akka.actor.ActorLogging
//import akka.actor.Terminated
//import akka.stream.ActorMaterializer
//
//object Main {
//
//  def main(args: Array[String]): Unit = {
//    implicit val system = ActorSystem("Sys")
//    implicit val materializer = ActorMaterializer
//    val supervisor = system.actorOf(Props[Supervisor], "supervisor")
//    system.actorOf(Props(classOf[Terminator], supervisor), "terminator")
//  }
//
//  class Terminator(ref: ActorRef) extends Actor with ActorLogging {
//    context watch ref
//    def receive = {
//      case Terminated(_) =>
//        log.info("{} has terminated, shutting down system", ref.path)
//        context.system.terminate()
//    }
//  }
//
//}
//class Supervisor extends Actor {
//
//  override def preStart(): Unit = {
//    // create the greeter actor
//    val greeter = context.actorOf(Props[ImageConsumer], "image_consumer")
//    // tell it to perform the greeting
//    greeter ! ImageConsumer
//  }
//
//  def receive = {
//    // when the greeter is done, stop this actor and with it the application
//    case ImageConsumer => context.stop(self)
//  }
//}
