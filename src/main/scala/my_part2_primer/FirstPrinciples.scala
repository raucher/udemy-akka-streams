package my_part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object FirstPrinciples extends App {

  // Used by materializer
  implicit val system = ActorSystem("FirstPrinciples")
  // Used by graph.run
  implicit val materializer = ActorMaterializer()

  // Stream parts: Source -> [Operation] -> Sink
  val source = Source(1 to 10)
  val sink = Sink.foreach[Int](println)
  val graph = source.to(sink)

  // executes the graph, uses implicit materializer
  //  graph.run

  // Flow
  val flow = Flow[Int].map(_ * 2)
  val sourceWithFlow = source.via(flow)
  val sinkWithFlow = flow.to(sink)

  // all are equivalent
  sourceWithFlow.to(sink).run()
  source.to(sinkWithFlow).run()
  source via flow to sink run

  // Sources
  val emptySource = Source.empty[Int]
  val singleSource = Source.single(357)
  val finiteSource = Source(List(1, 2, 3))
  val infiniteSource = Source(Stream.from(1))
  val fromFutureSource = Source.fromFuture(Future(759))

  // Sinks
  val ignoreSink = Sink.ignore
  val foreachSink = Sink.foreach[Int](println)
  val headSink = Sink.head[Int]
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  // Flows - are usually mapped to collection functions
  val takeFlow = Flow[Int].take(5)
  val filterFlow = Flow[Int].filter(_ % 2 == 0)
  // except .flatMap, we can't resolve streams from stream, but should have sub-streams in such a situation
  // Basically: Source -> Flow -> Flow -> ... -> Sink

  // Shortcuts
  // source via flow via flow to sink
  Source(1 to 10).filter(_ % 2 == 0).map(_ * 3).runForeach(println)

  /**
   * Exercise:
   *  Keep first 2 name with length greater then 5 chars
   */
  Source(List("bob", "ozzy", "bo", "giulia", "helen", "wolfgang"))
    .filter(_.length > 5)
    .take(2)
    .map(_.capitalize)
    .runForeach(println) // Giulia, Wolfgang


  // Terminate the system
  system.terminate().onComplete {
    case Success(_) => println("Bye!")
    case Failure(exception) =>
  }
}
