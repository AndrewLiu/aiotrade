package org.aiotrade.lib.securities.data.git

import java.util.List
import java.io.File
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.Transport

/**
 * One use case of "git fetch" is that the following will tell you any changes 
 * in the remote branch since your last pull, so you can check before doing an 
 * actual pull, which could change files in your working copy:
 * # git fetch 
 * # git diff origin/master
 * 
 * # Then update your local branch by:
 * # git merge origin/master
 */
class Fetch(repository: Repository, remoteName: String) extends Command(repository) {
  var timeout = -1

  private var fsck: Boolean = true
  def isFsck(b: Boolean) {
    fsck = b
  }

  private var thin: Boolean = Transport.DEFAULT_FETCH_THIN
  def isThin(b: Boolean) {
    thin = b
  }

  private var prune: Boolean = true
  private var dryRun: Boolean = _
  private var toget: List[RefSpec] = _

  @throws(classOf[Exception])
  protected def run {
    val git = new org.eclipse.jgit.api.Git(db)
    val fetch = git.fetch
    fetch.setCheckFetchedObjects(fsck)
    fetch.setRemoveDeletedRefs(prune)
    if (toget != null)
      fetch.setRefSpecs(toget)
    if (0 <= timeout)
      fetch.setTimeout(timeout)
    fetch.setDryRun(dryRun)
    fetch.setRemote(remoteName)
    fetch.setThin(thin)
    fetch.setProgressMonitor(new TextProgressMonitor)

    val result = fetch.call
    if (!result.getTrackingRefUpdates.isEmpty) {
      showFetchResult(result)
    }
  }
}

object Fetch {
  def apply(aGitDir: String, localName: String = null, remoteName: String = Constants.DEFAULT_REMOTE_NAME) = {
    val gitDir = Command.guessGitDir(aGitDir, localName)    
    val repo = Git.openGitDir(gitDir)

    new Fetch(repo, remoteName)
  }
  
  // --- simple test
  def main(args: Array[String]) {
    val userHome = System.getProperty("user.home")
    val command = Fetch(userHome + File.separator + "gittest/.git")
    command.run
  }
}