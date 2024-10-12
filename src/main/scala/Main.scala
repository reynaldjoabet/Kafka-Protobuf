package model

import scala.concurrent.duration._

import cats.effect.{IO, IOApp}
import fs2.kafka.*
import fs2.Stream

import com.google.protobuf.CodedInputStream
import model.addressbook.{AddressbookProto, Person}
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException

object Main extends IOApp.Simple {

  def run: IO[Unit] = {
    def processRecord(record: ConsumerRecord[String, Array[Byte]]): IO[Unit] = {
      val person = Person.parseFrom(CodedInputStream.newInstance(record.value))
      IO(println(s"Processing person: $person"))
    }

    val consumerSettings =
      ConsumerSettings[IO, String, Array[Byte]]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers("localhost:9092")
        .withGroupId("group")

    val producerSettings =
      ProducerSettings[IO, String, Array[Byte]].withBootstrapServers("localhost:9092")

    def createTopic: IO[Unit] = {
      val adminClientSettings: AdminClientSettings = AdminClientSettings(
        "localhost:9092"
      )

      KafkaAdminClient
        .resource[IO](adminClientSettings)
        .use {
          _.createTopic(new NewTopic("topic", 1, 1.toShort))
            .recoverWith { case _: TopicExistsException =>
              IO(println(s"Topic already exists"))
            }
        }
    }

    val protoPersonEvents = List(
      Person("Alice", 42, Some("alice@test.com")),
      Person("Bob", 55, Some("bob@test.com")),
      Person("Mallory", 77)
    )

    def producePersonProtoEvents: Stream[IO, Unit] =
      KafkaProducer
        .stream(producerSettings)
        .flatMap { producer =>
          Stream
            .emits(protoPersonEvents)
            .evalTap(p => IO(println(s"Producing person: $p")))
            .evalMap(p => producer.produceOne_("topic", "key", p.toByteArray).flatten.void)
            .metered(1.seconds)
        }

    def consumePersonEvents: Stream[IO, Unit] =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo("topic")
        .records
        .mapAsync(25) { committable =>
          processRecord(committable.record).as(committable.offset)
        }
        .through(commitBatchWithin(1, 1.seconds))

    val stream =
      Stream.eval(createTopic) ++ producePersonProtoEvents.concurrently(
        consumePersonEvents
      )

    stream.compile.drain
  }

}
