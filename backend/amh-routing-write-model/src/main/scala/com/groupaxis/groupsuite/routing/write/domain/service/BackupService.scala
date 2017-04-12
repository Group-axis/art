package com.groupaxis.groupsuite.routing.write.domain.service

import com.groupaxis.groupsuite.routing.write.domain.model.amh.backup.BackupRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.persistence.driver.DBDriver


trait BackupService extends BackupRepository with DBDriver {
//  import driver.backend.DatabaseDef

  implicit val ec: ExecutionContext
  implicit val db: Database

  def clean(): Future[Try[Boolean]] = initialize

  def backup(): Future[Try[Boolean]] = fillBackup

  def restore(): Future[Try[Boolean]] = restoreFromBackup
}

class BackupServiceImpl(exc: ExecutionContext, database : Database) extends BackupService {
  implicit val ec: ExecutionContext = exc
  implicit val db = database

}
