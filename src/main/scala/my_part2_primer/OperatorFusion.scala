package my_part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

  implicit val system: ActorSystem = ActorSystem("OperatorFusion")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source = Source(1 to 1000)
  val flow1 = Flow[Int].map(_ * 10)
  val flow2 = Flow[Int].map(_ + 1)
  val sink = Sink.foreach[Int](println)

  source
    .via(flow1)
    .via(flow2)
  //    .runWith(sink)

  val complexFlow1 = Flow[Int].map {
    Thread.sleep(1000)
    _ * 10
  }

  val complexFlow2 = Flow[Int].map {
    Thread.sleep(1000)
    _ + 1
  }

  source
    // communication done via async actor messages
    .via(complexFlow1).async // next ops will be run on another actor and thread,
    .via(complexFlow2).async // sink will run on another actor and thread
    .runWith(sink)


  //  system.terminate()
}
