package my_part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Balance
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 100)
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val sink = Sink.foreach[(Int, Int)](println)

  // Step 1 - create a graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Step 2 - add auxiliary components
      val broadcast = builder.add(Broadcast[Int](2)) // UniformFanOutShape
      val zip = builder.add(Zip[Int, Int]) // FanInShape2

      // Step 3 - construct the graph
      input ~> broadcast // input "feeds" broadcast

      // broadcast output to incrementer and multiplier components
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> sink // zip "feeds" sink

      ClosedShape
    }
  )
  graph.run() // materialized value is NotUsed

  val twoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](outputPorts = 2))
      //      val sink1 = builder.add(Sink.foreach[Int](x => println(s"Sink-1: $x")))
      //      val sink2 = builder.add(Sink.foreach[Int](x => println(s"Sink-2: $x")))
      val sink1 = Sink.foreach[Int](x => println(s"Sink-1: $x"))
      val sink2 = Sink.foreach[Int](x => println(s"Sink-2: $x"))

      //      input ~> broadcast
      //      broadcast.out(0) ~> sink1
      //      broadcast.out(1) ~> sink2

      input ~> broadcast ~> sink1 // implicit port numbering
      broadcast ~> sink2 // here "implicit" means automatic allocation

      ClosedShape
    }
  )
  twoSinksGraph.run()

  /**
   * 2 Sources -> Fast and Slow, feed Merge -> feed Balance -> feed 2 Sinks
   */
  val balancedGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // 2 Sources
      val fastSource = Source(1 to 100 by 2)
      val slowSource = Source(2 to 100 by 2).map(x => {
        Thread.sleep(250)
        x
      })

      // 2 Sinks
      val sink1 = Sink.foreach[Int](x => println(s"[BalancedGraph] Sink-1: $x"))
      val sink2 = Sink.foreach[Int](x => println(s"[BalancedGraph] Sink-2: $x"))

      // Merge
      val merge = builder.add(Merge[Int](inputPorts = 2))
      fastSource ~> merge
      slowSource ~> merge

      // Balance
      val balance = builder.add(Balance.create[Int](outputCount = 2))
      merge ~> balance ~> sink1
      balance ~> sink2

      ClosedShape
    }
  )
  //  balancedGraph.run()

  /**
   * 2 Sources -> Fast and Slow, feed Merge -> feed Balance -> feed 2 Sinks
   */
  val balancedThrottledGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // 2 Sources
      val fastSource = input.take(30).throttle(5, 1 second)
      val slowSource = input.take(30).throttle(3, 1 second)

      // 2 Sinks
      val sink1 = Sink.foreach[Int](x => println(s"[BalancedThrottledGraph] Sink-1: $x"))
      val sink2 = Sink.foreach[Int](x => println(s"[BalancedThrottledGraph] Sink-2: $x"))

      // Merge
      val merge = builder.add(Merge[Int](inputPorts = 2))

      // Balance
      val balance = builder.add(Balance.create[Int](outputCount = 2))

      // Construct the graph
      fastSource ~> merge
      slowSource ~> merge

      merge ~> balance

      balance ~> sink1
      balance ~> sink2

      ClosedShape
    }
  )

  balancedThrottledGraph.run()
}
