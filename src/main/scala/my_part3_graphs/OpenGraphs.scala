package my_part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}

object OpenGraphs extends App {
  implicit val system = ActorSystem("OpenGraphs")
  implicit val meterializer = ActorMaterializer()

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 50)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concatenator = builder.add(Concat[Int](inputPorts = 2))

      firstSource ~> concatenator
      secondSource ~> concatenator

      SourceShape(concatenator.out)
    }
  )

  //  sourceGraph.runWith(Sink.foreach(println)).onComplete {
  //    case Success(done) =>
  //      println(done)
  //      system.terminate()
  //    case Failure(e) => e.printStackTrace()
  //  }

  val complexSink = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sink1 = Sink.foreach[Int](_ => println("Sink-1"))
      val sink2 = Sink.foreach[Int](_ => println("Sink-2"))

      val broadcast = builder.add(Broadcast[Int](outputPorts = 2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  //  Source(1 to 5).runWith(complexSink)

  /**
   * Complex Flow should combine 2 flows
   * - one adds 1 to a number
   * - second - multiplies this number by 10
   */
  val complexFlow = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val incrementer = builder.add(Flow[Int].map(_ + 1))
      val multiplier = builder.add(Flow[Int].map(_ * 10))

      incrementer ~> multiplier

      FlowShape(in = incrementer.in, out = multiplier.out)
    }
  )

  Source(1 to 10) via complexFlow runWith Sink.foreach(println)
}
