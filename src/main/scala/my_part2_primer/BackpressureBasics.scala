package my_part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object BackpressureBasics extends App {
  implicit val system: ActorSystem = ActorSystem("BackpressureBasics")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val fastSource = Source(1 to 100)

  val simpleFlow = Flow[Int].map { x =>
    println(s"Flow: $x")
    x
  }

  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  //  fastSource.via(simpleFlow).runWith(slowSink) // Operator fussion (sequentially in 1 actor), not actually a backpressure

  //  fastSource.async // run next operation in different actor
  //    .via(simpleFlow).async // run next operation in different actor
  //    .runWith(slowSink) // this is a backpressure

  // Buffering
//  val bufferedFlow =
//    simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
//
//  fastSource.async
//    .via(bufferedFlow).async
//    .runWith(slowSink)

  // Throttling
  Source(1 to 100).throttle(elements = 5, per = 1 second)
    .runWith(Sink.foreach(println))
}
