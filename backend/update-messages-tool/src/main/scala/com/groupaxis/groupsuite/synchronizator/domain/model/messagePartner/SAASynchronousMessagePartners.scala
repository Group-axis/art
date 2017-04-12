package com.groupaxis.groupsuite.synchronizator.domain.model.messagePartner

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.{MessagePartner, MessagePartnerES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait SAAMessagePartnerRepositoryT extends AMHRepository[MessagePartner, String] {

}

class SAAMessagePartnerRepository(val db: Database) extends SAAMessagePartnerRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartnerDAO._
  import driver.api._

  override def query(id: String): Future[Option[MessagePartner]] = {
    db.db.run(messagePartners.filter(_.name === id).result.headOption)
  }

  override def store(messagePartner: MessagePartner): Future[MessagePartner] = {
    db.db.run(messagePartners returning messagePartners += messagePartner)
  }

  override def all: Future[Seq[MessagePartner]] =
    db.db.run(messagePartners.result)

  override def storeAll(messagePartnersToStore: Seq[MessagePartner]): Future[Seq[MessagePartner]] =
    db.db.run(messagePartners returning messagePartners ++= messagePartnersToStore)
}

object SAAMessagePartnerRepository {
  def apply(db: Database) : SAAMessagePartnerRepository = {
    new SAAMessagePartnerRepository(db)
  }
}

trait SAAMessagePartnerESRepositoryT extends AMHESRepository[MessagePartnerES] {
  implicit override def indexName: String = "routing/messagePartners"
}

class SAAMessagePartnerESRepository(val client: ElasticClient) extends SAAMessagePartnerESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient: ElasticClient = client

  override def query(id: String): Future[Option[MessagePartnerES]] = Future {
    None
  }

  override def store(a: MessagePartnerES): Future[MessagePartnerES] = Future {
    a
  }

  override def all: Future[Seq[MessagePartnerES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[MessagePartnerES]): Future[Seq[MessagePartnerES]] = {
    val f = (messagePartner: MessagePartnerES) => index into indexName source messagePartner id messagePartner.name
    withBulk(f)(as)
  }

}

object SAAMessagePartnerESRepository {
  def apply(esClient: ElasticClient):SAAMessagePartnerESRepository = {
    new SAAMessagePartnerESRepository(esClient)
  }
}


sealed trait SAASynchronousMessagePartners {
  type SAAMessagePartners = Seq[MessagePartner]
  type SAAMessagePartnersES = Seq[MessagePartnerES]

}

object SAASynchronousMessagePartners extends SAASynchronousMessagePartners with AMHService[MessagePartner, String, MessagePartnerES] {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[MessagePartner, String], AMHRepository[MessagePartnerES, String]) => Future[Seq[MessagePartnerES]] = {
    updateIntoES(toES)
  }

  private def toES(messagePartners: SAAMessagePartners)(implicit ec: ExecutionContext): Future[SAAMessagePartnersES] = {

    Future {
      messagePartners.map(s => s.toES)
    }
  }
}