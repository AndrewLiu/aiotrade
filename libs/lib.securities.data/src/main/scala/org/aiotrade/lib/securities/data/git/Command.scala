package org.aiotrade.lib.securities.data.git

import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.text.MessageFormat
import java.util.logging.Logger
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.FetchResult
import org.eclipse.jgit.transport.TrackingRefUpdate


/**
 * @param git repository that the command was invoked within.
 */
import Command._
abstract class Command(protected val db: Repository) {
  
  /** RevWalk used during command line parsing, if it was required. */
  protected var argWalk: RevWalk = _

  protected def outputEncoding = if (db != null) db.getConfig.getString("i18n", null, "logOutputEncoding") else null
  
  protected def gitDir: String = if (db != null) db.getDirectory.getAbsolutePath else null

  /**
   * Perform the actions of this command.
   */
  @throws(classOf[Exception])
  protected def run
  
  protected def showFetchResult(r: FetchResult) {
    val reader = db.newObjectReader
    try {
      var uriShown = false
      val us = r.getTrackingRefUpdates.iterator
      while (us.hasNext) {
        val u = us.next
        if (u.getResult != RefUpdate.Result.NO_CHANGE) {
          val tpe = shortTypeOf(u.getResult)
          val longTpe = longTypeOf(reader, u)
          val src = abbreviateRef(u.getRemoteName, false)
          val dst = abbreviateRef(u.getLocalName, true)

          if (!uriShown) {
            log.info(MessageFormat.format(CLIText().fromURI, r.getURI))
            uriShown = true
          }

          log.info(String.format(" %c %-17s %-10s -> %s", tpe.asInstanceOf[AnyRef], longTpe, src, dst))
        }
      }
    } finally {
      reader.release
    }
    showRemoteMessages(r.getMessages)
  }
}

object Command {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def showRemoteMessages(aPkt: String) {
    var pkt = aPkt
    val sb = new StringBuilder
    while (0 < pkt.length) {
      val lf = pkt.indexOf('\n')
      val cr = pkt.indexOf('\r')
      val s = if (0 <= lf && 0 <= cr)
        math.min(lf, cr)
      else if (0 <= lf)
        lf
      else if (0 <= cr)
        cr
      else {
        sb.append(MessageFormat.format(CLIText().remoteMessage, pkt)).append('\n')
        log.info(sb.toString)
        return
      }

      if (pkt.charAt(s) == '\r') {
        sb.append(MessageFormat.format(CLIText().remoteMessage, pkt.substring(0, s)))
        sb.append('\r')
      } else {
        sb.append(MessageFormat.format(CLIText().remoteMessage, pkt.substring(0, s))).append('\n')
      }

      pkt = pkt.substring(s + 1)
    }
    log.info(sb.toString)
  }
  
  def abbreviateRef(dst: String, abbreviateRemote: Boolean): String = {
    if (dst.startsWith(Constants.R_HEADS))
      dst.substring(Constants.R_HEADS.length)
    else if (dst.startsWith(Constants.R_TAGS))
      dst.substring(Constants.R_TAGS.length)
    else if (abbreviateRemote && dst.startsWith(Constants.R_REMOTES))
      dst.substring(Constants.R_REMOTES.length)
    else dst
  }
  
  private def longTypeOf(reader: ObjectReader, u: TrackingRefUpdate): String = {
    u.getResult match {
      case RefUpdate.Result.LOCK_FAILURE => "[lock fail]"
      case RefUpdate.Result.IO_FAILURE => "[i/o error]"
      case RefUpdate.Result.REJECTED => "[rejected]"
      case _ if ObjectId.zeroId.equals(u.getNewObjectId) => "[deleted]"
      case RefUpdate.Result.NEW =>
        if (u.getRemoteName.startsWith(Constants.R_HEADS))
          "[new branch]"
        else if (u.getLocalName.startsWith(Constants.R_TAGS))
          "[new tag]"
        else "[new]"
      case RefUpdate.Result.FORCED =>
        val aOld = safeAbbreviate(reader, u.getOldObjectId)
        val aNew = safeAbbreviate(reader, u.getNewObjectId)
        aOld + "..." + aNew
      case RefUpdate.Result.FAST_FORWARD =>
        val aOld = safeAbbreviate(reader, u.getOldObjectId)
        val aNew = safeAbbreviate(reader, u.getNewObjectId)
        aOld + ".." + aNew
      case RefUpdate.Result.NO_CHANGE => "[up to date]"
      case r => "[" + r.name + "]"
    }
  }
  
  private def shortTypeOf(r: RefUpdate.Result): Char = {
    r match {
      case RefUpdate.Result.LOCK_FAILURE | RefUpdate.Result.IO_FAILURE | RefUpdate.Result.REJECTED => '!'
      case RefUpdate.Result.NEW => '*'
      case RefUpdate.Result.FORCED => '+'
      case RefUpdate.Result.FAST_FORWARD => ' '
      case RefUpdate.Result.NO_CHANGE => '='
      case _ => ' '
    }
  }
  
  private def safeAbbreviate(reader: ObjectReader, id: ObjectId): String = {
    try {
      reader.abbreviate(id).name
    } catch {
      case cannotAbbreviate: IOException => id.name
    }
  }
  
  @throws(classOf[MalformedURLException])
  private def configureHttpProxy {
    val s = System.getenv("http_proxy")
    if (s == null || s.equals(""))
      return

    val u = new URL(if (s.indexOf("://") == -1) "http://" + s else s)
    if (!"http".equals(u.getProtocol))
      throw new MalformedURLException(MessageFormat.format(CLIText().invalidHttpProxyOnlyHttpSupported, s))

    val proxyHost = u.getHost
    val proxyPort = u.getPort

    System.setProperty("http.proxyHost", proxyHost)
    if (proxyPort > 0)
      System.setProperty("http.proxyPort", String.valueOf(proxyPort))

    val userpass = u.getUserInfo
    if (userpass != null && userpass.contains(":")) {
      val c = userpass.indexOf(':')
      val user = userpass.substring(0, c)
      val pass = userpass.substring(c + 1)
      CachedAuthenticator.add(CachedAuthenticator.CachedAuthentication(proxyHost, proxyPort, user, pass))
    }
  }
  
}