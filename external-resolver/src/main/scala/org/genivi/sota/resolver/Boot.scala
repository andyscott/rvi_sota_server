/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.filters.FilterDirectives
import org.genivi.sota.resolver.packages.PackageDirectives
import org.genivi.sota.resolver.resolve.ResolveDirectives
import org.genivi.sota.resolver.vehicles.VehicleDirectives
import org.genivi.sota.resolver.components.ComponentDirectives
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.driver.MySQLDriver.api._

/**
 * Base API routing class.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class Routing
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
 {
   import Directives._
   import org.genivi.sota.datatype.NamespaceDirective._

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        new VehicleDirectives(defaultNamespaceExtractor).route ~
        new PackageDirectives(defaultNamespaceExtractor).route ~
        new FilterDirectives(defaultNamespaceExtractor).route ~
        new ResolveDirectives(defaultNamespaceExtractor).route ~
        new ComponentDirectives(defaultNamespaceExtractor).route
      }
    }
  }
}

object Boot extends App {

  implicit val system       = ActorSystem("sota-external-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec         = system.dispatcher
  implicit val log          = Logging(system, "boot")
  implicit val db           = Database.forConfig("database")
  val config = system.settings.config

  log.info(org.genivi.sota.resolver.BuildInfo.toString)

  // Database migrations
  if (config.getBoolean("database.migrate")) {
    val url = config.getString("database.url")
    val user = config.getString("database.properties.user")
    val password = config.getString("database.properties.password")

    import org.flywaydb.core.Flyway
    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    flyway.migrate()
  }

  val route         = new Routing
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://${host}:${port}/")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.terminate() )
  }
}
