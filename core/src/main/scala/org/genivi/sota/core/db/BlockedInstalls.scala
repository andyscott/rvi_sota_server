/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.db

import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace._
import org.genivi.sota.core.data.BlockedInstall
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object BlockedInstalls {

  import org.genivi.sota.db.SlickExtensions._
  import Device._

  implicit val deviceIdColumnType =
    MappedColumnType.base[DeviceId, String](
      { case DeviceId(value) => value.toString },
      DeviceId(_)
    )

  // scalastyle:off
  class BlockedInstallTable(tag: Tag) extends Table[BlockedInstall](tag, "BlockedInstall") {
    def id = column[Id]("uuid")
    def blockedAt = column[Instant]("blocked_at")

    def * = (id, blockedAt).shaped <>
      ((BlockedInstall.apply _).tupled, BlockedInstall.unapply)

    def pk = primaryKey("id", id)
  }
  // scalastyle:on
  val all = TableQuery[BlockedInstallTable]

  def get(id: Device.Id)
         (implicit ec: ExecutionContext): DBIO[Seq[BlockedInstall]] = {
    all.filter(d => d.id === id).result
  }

  def delete(id: Device.Id)
            (implicit ec: ExecutionContext): DBIO[Int] = {
    all
      .filter(d => d.id === id)
      .delete
  }

  def persist(id: Device.Id)
             (implicit ec: ExecutionContext): DBIO[Int] = {
    all.insertOrUpdate(
      BlockedInstall.from(id)
    )
  }

  def isBlockedInstall(id: Device.Id)
                      (implicit ec: ExecutionContext): DBIO[Boolean] = {
    all
      .filter(d => d.id === id)
      .exists
      .result
  }

}
