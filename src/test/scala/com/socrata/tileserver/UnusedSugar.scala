package com.socrata.tileserver

import scala.language.implicitConversions

import org.mockito.Mockito.mock

import com.socrata.http.client.Response
import com.socrata.http.server.{HttpRequest, HttpResponse}

trait UnusedSugar {
  trait UnusedValue
  object Unused extends UnusedValue
  implicit def unusedToInt(u: UnusedValue): Int = 0
  implicit def unusedToString(u: UnusedValue): String = "unused"
  implicit def unusedToHttpRequest(u: UnusedValue): HttpRequest =
    mocks.StaticRequest()
  implicit def unusedToQuadTile(u: UnusedValue): util.QuadTile =
    util.QuadTile(0, 0, 0)
  implicit def unusedToCoordinateMapper(u: UnusedValue): util.CoordinateMapper =
    new util.CoordinateMapper(0) {
      override def tilePx(lon: Double, lat:Double): (Int, Int) =
        (lon.toInt, lat.toInt)
    }
  implicit def unusedToRespToHttpResponse(u: UnusedValue): Response => HttpResponse =
    r => mock(classOf[HttpResponse])
}
