package org.aiotrade.lib.securities.data.git

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.text.MessageFormat
import java.util.ArrayList
import java.util.Collections
import java.util.logging.Logger
import org.eclipse.jgit.dircache.DirCacheCheckout
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.RefComparator
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.FetchResult
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteConfig
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.TagOpt

class Clone(repository: Repository, sourceUri: URIish, remoteName: String) extends Command(repository) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  @throws(classOf[Exception])
  protected def run {
    val t0 = System.currentTimeMillis
    
    saveRemote(sourceUri)
    val r = runFetch
    val branch = guessHEAD(r)
    doCheckout(branch)
    
    log.info("Cloned in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
  }

  @throws(classOf[IOException])
  @throws(classOf[URISyntaxException])
  private def saveRemote(uri: URIish) {
    val dstcfg = db.getConfig
    val rc = new RemoteConfig(dstcfg, remoteName)
    rc.addURI(uri)
    rc.addFetchRefSpec(new RefSpec().setForceUpdate(true).setSourceDestination(Constants.R_HEADS + "*",
                                                                               Constants.R_REMOTES + remoteName + "/*"))
    rc.update(dstcfg)
    dstcfg.save
  }

  @throws(classOf[NotSupportedException])
  @throws(classOf[URISyntaxException])
  @throws(classOf[TransportException])
  private def runFetch: FetchResult = {
    val tn = Transport.open(db, remoteName)
    val r = try {
      tn.setTagOpt(TagOpt.FETCH_TAGS)
      tn.fetch(new TextProgressMonitor, null)
    } finally {
      tn.close
    }
    showFetchResult(r)
    
    r
  }

  private def guessHEAD(result: FetchResult): Ref = {
    val idHEAD = result.getAdvertisedRef(Constants.HEAD)
    val availableRefs = new ArrayList[Ref]()
    var head: Ref = null
    val advertisedrefs = result.getAdvertisedRefs.iterator
    while (advertisedrefs.hasNext) {
      val r = advertisedrefs.next
      val n = r.getName
      if (n.startsWith(Constants.R_HEADS)) {
        availableRefs.add(r)
        if (head == null && idHEAD != null) {
          if (r.getObjectId.equals(idHEAD.getObjectId))
            head = r
        }
      }
    }
    Collections.sort(availableRefs, RefComparator.INSTANCE)
    if (idHEAD != null && head == null)
      head = idHEAD
    
    head
  }

  @throws(classOf[IOException])
  private def doCheckout(branch: Ref) {
    if (branch == null)
      throw new IOException(CLIText().cannotChekoutNoHeadsAdvertisedByRemote)
    if (!Constants.HEAD.equals(branch.getName)) {
      val u = db.updateRef(Constants.HEAD)
      u.disableRefLog
      u.link(branch.getName)
    }

    val commit = parseCommit(branch)
    val u = db.updateRef(Constants.HEAD)
    u.setNewObjectId(commit)
    u.forceUpdate

    val dc = db.lockDirCache
    val co = new DirCacheCheckout(db, dc, commit.getTree)
    co.checkout
  }

  @throws(classOf[MissingObjectException])
  @throws(classOf[IncorrectObjectTypeException])
  @throws(classOf[IOException])
  private def parseCommit(branch: Ref): RevCommit = {
    val rw = new RevWalk(db)
    val commit = try {
      rw.parseCommit(branch.getObjectId)
    } finally {
      rw.release
    }
    commit
  }
}

object Clone {
  /**
   * @param git dir which ends with '/.git'
   * @param local name of destination
   * @param source url
   * @param remote name, default is 'origin'
   */
  def apply(aGitDir: String, aLocalName: String, sourceUri: String, remoteName: String = Constants.DEFAULT_REMOTE_NAME) = {
    if (aLocalName != null && aGitDir != null) {
      throw new RuntimeException(CLIText().conflictingUsageOf_git_dir_andArguments)
    }
    
    val srcUri = new URIish(sourceUri)
    val gitDir = if (aGitDir == null) {
      val localName = if (aLocalName == null) {
        try {
          srcUri.getHumanishName
        } catch {
          case e: IllegalArgumentException => throw new Exception(MessageFormat.format(CLIText().cannotGuessLocalNameFrom, sourceUri))
        }
      } else aLocalName
      
      new File(localName, Constants.DOT_GIT).getAbsolutePath
    } else aGitDir
    
    val repository = Git.createFileRepository(gitDir)

    new Clone(repository, srcUri, remoteName)
  }
  
  // --- simple test
  def main(args: Array[String]) {
    val userHome = System.getProperty("user.home")
    val clone = Clone(userHome + File.separator + "gittest/.git", null, "git://github.com/dcaoyuan/dcaoyuan.github.com.git")
    clone.run
  }
}