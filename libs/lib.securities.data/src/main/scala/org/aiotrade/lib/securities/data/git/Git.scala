package org.aiotrade.lib.securities.data.git

import java.io.File
import java.io.IOException
import java.text.MessageFormat
import java.util.logging.Level
import java.util.logging.Logger
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.URIish

/**
 *
 * @author Caoyuan Deng
 */
object Git {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _monitor: ProgressMonitor = _
  private def monitor = {
    if (_monitor == null) {
      _monitor = new TextProgressMonitor
    }
    _monitor
  }
  def monitor_=(monitor: ProgressMonitor) {
    _monitor = monitor
  }
  
  def clone(gitPath: String, sourceUri: String, localName: String = null, remoteName: String = Constants.DEFAULT_REMOTE_NAME) = {
    val gitDir = guessGitDir(gitPath, localName, sourceUri)    
    if (gitDir.exists) {
      log.info(gitDir.getAbsolutePath + " existed, will delete it first.")
      try {
        deleteDir(gitDir)
        log.info(gitDir.getAbsolutePath + " deleted: " + !gitDir.exists)
      } catch {
        case e => log.log(Level.SEVERE, e.getMessage, e) 
      }
    }
    
    try {
      gitDir.mkdirs
    } catch {
      case e => log.log(Level.SEVERE, e.getMessage, e) 
    }

    val cmd = new CloneCommand
    cmd.setDirectory(gitDir)
    cmd.setURI(sourceUri)
    cmd.setRemote(remoteName)
    // @Note:
    // default branch in CloneCommand is Constants.HEAD, which will bypass 
    //   if (branch.startsWith(Constants.R_HEADS)) {
    //     final RefUpdate head = repo.updateRef(Constants.HEAD);
    //     head.disableRefLog();
    //     head.link(branch);
    //   }
    // and causes no branch is linked, the actual HEAD name should be Constants.R_HEADS + Constants.MASTER
    cmd.setBranch(Constants.R_HEADS + Constants.MASTER)
    
    val t0 = System.currentTimeMillis
    cmd.setProgressMonitor(monitor)
    val git = try {
      cmd.call
    } catch {
      case e => log.log(Level.SEVERE, e.getMessage, e); null
    }
    /* @see cmd.setBranch(Constants.R_HEADS + Constants.MASTER) */
    fixCloneCommond(git.getRepository, Constants.R_HEADS + Constants.MASTER, remoteName)

    log.info("Cloned in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
    git
  }
  
  /**
   * Change branch name from "refs/heads/master" to "master"
   */
  private def fixCloneCommond(repo: Repository, branch: String, remote: String) {
    // remove [branch "refs/heads/master"]
    repo.getConfig.unsetSection(ConfigConstants.CONFIG_BRANCH_SECTION, branch)
    
    // set back contents to [branch "master"]
    repo.getConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION, Constants.MASTER, 
                             ConfigConstants.CONFIG_KEY_REMOTE, remote)
    repo.getConfig.setString(ConfigConstants.CONFIG_BRANCH_SECTION, Constants.MASTER, 
                             ConfigConstants.CONFIG_KEY_MERGE, branch)

    repo.getConfig.save
  }
  
  def pull(gitPath: String, localName: String = null, remoteName: String = Constants.DEFAULT_REMOTE_NAME) {
    val gitDir = guessGitDir(gitPath, localName)    
    val repo = openGitRepository(gitDir)

    val git = new org.eclipse.jgit.api.Git(repo)
    val cmd = git.pull
    
    val t0 = System.currentTimeMillis
    cmd.setProgressMonitor(monitor)
    try {
      cmd.call
    } catch {
      case e => log.log(Level.SEVERE, e.getMessage, e) 
    }
    log.info("Pulled in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
  }
  
  // --- helper
  
  def guessGitDir(gitPath: String, aLocalName: String = null, sourceUri: String = null): File = {
    if (aLocalName != null && gitPath != null) {
      throw new RuntimeException(CLIText().conflictingUsageOf_git_dir_andArguments)
    }
    
    val gitDir = if (gitPath == null) {
      val localName = if (aLocalName == null && sourceUri != null) {
        try {
          new URIish(sourceUri).getHumanishName
        } catch {
          case e: IllegalArgumentException => throw new Exception(MessageFormat.format(CLIText().cannotGuessLocalNameFrom, sourceUri))
        }
      } else aLocalName
      
      new File(localName).getAbsolutePath
    } else gitPath
    
    new File(gitDir)
  }
  
  @throws(classOf[IOException])
  private def deleteDir(dir: File) {
    val files = dir.listFiles
    var i = 0
    while (i < files.length) {
      val file = files(i)
      if (file.isDirectory) {
        deleteDir(file)
      } else {
        file.delete
      }
      i += 1
    }
    dir.delete
  }
  
  @throws(classOf[IOException])
  private def openGitRepository(gitPath: File): Repository = {
    val rb = (new RepositoryBuilder).setGitDir(gitPath).readEnvironment.findGitDir
    if (rb.getGitDir == null) throw new RuntimeException(CLIText().cantFindGitDirectory)
    
    rb.build
  }
  
//  @Note: 'val dst = new FileRepository(gitDir)' in this method cause NetBeans run out of stack space 
//  @throws(classOf[Exception])
//  private def createFileRepository(gitDir: String) = {
//    val dst = new org.eclipse.jgit.storage.file.FileRepository(gitDir)
//    dst.create()
//    val dstcfg = dst.getConfig
//    dstcfg.setBoolean("core", null, "bare", false)
//    dstcfg.save
//
//    log.info("Initialized empty git repository in " + gitDir)
//    dst
//  }

  // --- simple test
  def main(args: Array[String]) {
    val userHome = System.getProperty("user.home")
    val tmpPath = userHome + File.separator + "gittest" + File.separator
    val gitPath = tmpPath + "clone_test"
    
    val git = clone(gitPath, "file://" + tmpPath + "origin_test.git")
    
    pull(gitPath + File.separator + Constants.DOT_GIT)
  }
}
