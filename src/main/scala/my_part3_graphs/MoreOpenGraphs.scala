package my_part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}

import scala.language.postfixOps

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  val max3StaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val range = 1 to 10

  val source1 = Source(range)
  val source2 = Source(range.map(x => 5))
  val source3 = Source(range.reverse)

  val sink = Sink.foreach[Int](println)

  val runnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape
      source2 ~> max3Shape.in(1) // we can specify explicitly
      source3 ~> max3Shape // or just leave input numbering implicit

      max3Shape ~> sink

      ClosedShape
    })

  //  runnableGraph.run()


  case class Transaction(id: Int, sender: String, amount: Int)

  val txnSource = Source(List(
    Transaction(1, "John", 9000),
    Transaction(2, "John", 300),
    Transaction(3, "John", 10000),
    Transaction(4, "John", 42),
    Transaction(5, "John", 100000),
  ))

  val transactionFilterGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousFilter = builder.add(Flow[Transaction].filter(_.amount >= 10000).map(_.id))

    broadcast.out(1) ~> suspiciousFilter

    new FanOutShape2(broadcast.in, broadcast.out(0), suspiciousFilter.out)
  }


  val allTxnSink = Sink.foreach[Transaction](println)
  val suspiciousTxnSink = Sink.foreach[Int](println)

  val runnableSuspiciousGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val transactionFilterShape = builder.add(transactionFilterGraph)

      txnSource ~> transactionFilterShape.in

      transactionFilterShape.out0 ~> allTxnSink
      transactionFilterShape.out1 ~> suspiciousTxnSink

      ClosedShape
    }
  )

  runnableSuspiciousGraph.run()
}
