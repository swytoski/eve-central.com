package com.evecentral.dataaccess

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import com.evecentral.dataaccess.GetHistStats.CapturedOrderStatistics
import com.evecentral.util.ActorNames
import com.evecentral.{Database, OrderStatistics}
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.common.cache.{Cache, CacheBuilder}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object GetHistStats {

  case class Request(marketType: MarketType, bid: Boolean, region: BaseRegion,
                     system: Option[SolarSystem] = None,
                     from: Option[DateTime] = None, to: Option[DateTime] = None)

  @JsonIgnoreProperties(value = Array("variance", "highToLow", "wavg", "generated"))
  case class CapturedOrderStatistics(median: Double,
                                     variance: Double,
                                     max: Double, avg: Double,
                                     stdDev: Double,
                                     highToLow: Boolean,
                                     min: Double, volume: Long,
                                     fivePercent: Double,
                                     wavg: Double,
                                     generated: DateTime) extends OrderStatistics {
    val at = ISODateTimeFormat.dateTimeNoMillis().print(generated)
  }

}

class GetHistStats extends Actor {

  val cache: Cache[GetHistStats.Request, Seq[GetHistStats.CapturedOrderStatistics]] = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .build()

  private val log = LoggerFactory.getLogger(getClass)

  val dbworker = context.actorOf(Props[GetHistStatsWorker].withRouter(new SmallestMailboxRouter(5)).withDispatcher("db-dispatcher"),
    ActorNames.gethiststats)

  implicit val timeout: akka.util.Timeout = 10.seconds
  implicit val execC: ExecutionContext = context.dispatcher

  def receive = {
    case req: GetHistStats.Request => {
      Option(cache.getIfPresent(req)) match {
        case None =>
          val origSender = sender
          (dbworker ? req).map {
            result =>
              result match {
                case os: Seq[CapturedOrderStatistics] =>
                  cache.put(req, os)
                  origSender ! result
                case t =>
                  log.error("Unknown response " + t)
              }
          }
        case Some(res) =>
          sender ! res
      }
    }
    case _ =>
      log.error("Unknown request type ")
  }
}

class GetHistStatsWorker extends Actor {

  def receive = {
    case GetHistStats.Request(mtype, bid, region, system, from, to) => {
      import net.noerd.prequel.SQLFormatterImplicits._
      val regionid = region.regionid

      val systemid = system match {
        case Some(s) => s.systemid
        case None => 0
      }
      val bidint = if (bid) 1 else 0
      val fromDate = from.getOrElse(new DateTime().minusDays(7))
      val toDate = to.getOrElse(new DateTime())

      val result = Database.coreDb.transaction {
        tx =>
          tx.select("SELECT average,median,volume,stddev,buyup,minimum,maximum,timeat FROM trends_type_region WHERE typeid = ? AND " +
            "systemid = ? AND region = ? AND bid = ? AND timeat >= ? AND timeat <= ? ORDER BY timeat",
            mtype.typeid, systemid, regionid, bidint, fromDate, toDate) {
            row =>
              val avg = row.nextDouble.get
              val median = row.nextDouble.get
              val volume = row.nextLong.get
              val stddev = row.nextDouble.get
              val buyup = row.nextDouble.get
              val minimum = row.nextDouble.get
              val maximum = row.nextDouble.get
              val timeat = new DateTime(row.nextDate.get)
              GetHistStats.CapturedOrderStatistics(median, 0, maximum, avg, stddev, bid, minimum, volume, buyup, 0, timeat)
          }

      }
      sender ! result

    }

  }

}
