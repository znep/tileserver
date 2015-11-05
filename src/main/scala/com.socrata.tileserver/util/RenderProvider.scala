package com.socrata.tileserver
package util

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import javax.servlet.http.HttpServletResponse.{SC_OK => ScOk}

import com.rojoma.simplearm.v2.ResourceScope
import com.socrata.http.client.{HttpClient, RequestBuilder, Response}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import org.velvia.MsgPack

import exceptions.FailedRenderException

import RenderProvider._

/** Calls out to the renderer service to render tiles.
  *
  * @constructor create a renderer
  * @param http the http client to use.
  * @param baseUrl the base url (host, port, etc) for the service.
  */
case class RenderProvider(http: HttpClient, baseUrl: RequestBuilder) {
  /** Render the provided tile using the provided request info.
    *
    * @param rawTile a Map that contains the features as WKB.
    * @param info the request info to use while rendering the tile.
    */
  def renderPng(rawTile: MapTile, info: RequestInfo): InputStream = {
    val style = info.style.get
    val tile: Map[String, Seq[String]] = rawTile.map { case (layer, wkbs) =>
      layer -> wkbs.map(Base64.encodeBase64String(_))
    }

    val content: Map[String, Any] = Map("tile" -> tile,
                                        "zoom" -> info.zoom,
                                        "style" -> style,
                                        "overscan" -> info.overscan.getOrElse(0))
    val packed: Array[Byte] = MsgPack.pack(content)

    val blob = info.rs.open(new ByteArrayInputStream(packed))

    val req = baseUrl.
      addPath("render").
      addHeader("X-Socrata-RequestID" -> info.requestId).
      blob(blob)

    logger.info(URLDecoder.decode(req.toString, UTF_8.name))

    val before = System.nanoTime()
    val resp = http.execute(req, info.rs)
    val after = System.nanoTime()
    val duration = (after - before)/1000000
    val message = s"Carto Renderer (${resp.resultCode}) took ${duration}ms."

    if (resp.resultCode == ScOk) {
      logger.info(message)
      resp.inputStream()
    } else {
      logger.warn(message)
      throw FailedRenderException(IOUtils.toString(resp.inputStream(), UTF_8))
    }
  }
}

object RenderProvider {
  type MapTile = Map[String, Seq[Array[Byte]]]
  private val logger: Logger = LoggerFactory.getLogger(getClass)
}
