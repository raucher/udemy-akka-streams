package my_part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Rock", "the", "JVM"))
  val printerSink = Sink.foreach(println)
  val counterSink = Sink.fold[Int, String](0)((count, _) => count + 1)

  /**
   * Sink that:
   * - prints out all strings which are lowercase
   * - counts all strings that are shorter than N chars
   */
  val compositeSink = Sink.fromGraph(
    GraphDSL.create(counterSink, printerSink)((counterSinkMatValue, printerSinkMatValue) => (counterSinkMatValue, printerSinkMatValue)) {
      implicit builder =>
        (counterSinkShape, printerSinkShape) =>
          import GraphDSL.Implicits._

          val n = 5

          val broadcast = builder.add(Broadcast[String](outputPorts = 2))

          broadcast.out(0).filter(word => word equals word.toLowerCase) ~> printerSinkShape
          broadcast.out(1).filter(_.length < n) ~> counterSinkShape

          SinkShape(in = broadcast.in)
    }
  )

  //  val wordCountFuture = wordSource
  //    .toMat(compositeSink)(Keep.right) // Choose which materialized value to take
  //    .run
  val (wordCountFuture, printerFuture) = wordSource.runWith(compositeSink) // logically the same, keep Sink's mat. value

  wordCountFuture.onComplete {
    case Success(wordCount) => println(s"wordCount = $wordCount")
  }

  /**
   * Exercise: return new Flow, that materializes Future[Int] of num values passt through original Flow
   */
  def enhanceFlow[A, B](originalFlow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val foldSink = Sink.fold[Int, B](0)((count, _) => count + 1)

    Flow.fromGraph(GraphDSL.create(foldSink) { implicit builder =>
      foldSinkShape =>
        import GraphDSL.Implicits._

        val originalFlowShape = builder.add(originalFlow)
        val broadcast = builder.add(Broadcast[B](2))

        originalFlowShape ~> broadcast ~> foldSinkShape // implicitly connected via broadcast.out(0)

        FlowShape[A, B](in = originalFlowShape.in, out = broadcast.out(1))
    }
    )
  }

  val exFlow = enhanceFlow(Flow.fromFunction[String, Int](word => word.length))

  val exFuture: Future[Int] = wordSource
    .viaMat(exFlow)(Keep.right)
    .toMat(Sink.ignore)(Keep.left)
    .run

  exFuture.onComplete {
    case Success(count) => println(s"enhanced flow mat val: $count")
  }
}