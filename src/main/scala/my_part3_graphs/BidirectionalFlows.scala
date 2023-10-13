package my_part3_graphs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = ActorMaterializer()

  def encrypt(n: Int)(data: String) =
    data.map(char => (char + n).toChar)

  val dataToEncrypt = "Hello World!"
  val encrypted = encrypt(3)(dataToEncrypt)
  println(s"encrypted string '$dataToEncrypt' = '$encrypted'")

  val decrypted = encrypt(-3)(encrypted)
  println(s"decrypted '$encrypted' = '$decrypted'")

  // ACTUAL BIDIRECTIONAL GRAPH COMES HERE

}
