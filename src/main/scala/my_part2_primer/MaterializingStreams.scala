package my_part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {
  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  // Hard way
  private val matVal = Source(1 to 10)
    .viaMat(Flow[Int].filter(_ % 2 == 0))(Keep.right)
    .toMat(Sink.foreach[Int](println))(Keep.right).run()

  matVal.onComplete {
    case Success(message) => println(s"matVal success with message: $message") // Success with message: Done
    case Failure(e) => println(s"matVal failure: $e")
  }

  // Sugar path
  private val matVal2 = Source(1 to 10)
    .runWith(Sink.reduce[Int](_ + _))

  matVal2.onComplete {
    case Success(message) => println(s"matVal2 success with message: $message") // matVal2 success with message: 55
    case Failure(e) => println(s"matVal2 failure: $e")
  }

  /**
   * - return the last element out of a source (use Sink.last)
   * - compute the total word count out of a stream of sentences
   *   - map, fold, reduce
   */

  val last = Source(1 to 10)
    .runWith(Sink.last)

  val totalWordCount = Source(List(
    "one two three",
    "hello world",
    "sechs sieben"
  )).runFold(0)((numWords, sentence) => {
      numWords + sentence.split("\\s").length
    })
//    .runWith(Sink.head)
    .onComplete {
      case Success(numWords) => println(s"it was $numWords words in the stream") // it was 7 words in the stream
      case Failure(exc) => println(s"something went wrong that's why: ${exc.getMessage}")
    }
}
