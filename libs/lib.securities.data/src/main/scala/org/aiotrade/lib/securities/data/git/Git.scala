package org.aiotrade.lib.securities.data.git

import java.io.File
import java.io.IOException
import java.util.logging.Logger
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.storage.file.FileRepository

object Git {
  private val log = Logger.getLogger(this.getClass.getName)

  @throws(classOf[IOException])
  def openGitDir(gitdir: String): Repository = {
    val rb = (new RepositoryBuilder)
    .setGitDir(if (gitdir != null) new File(gitdir) else null)
    .readEnvironment
    .findGitDir
    
    if (rb.getGitDir == null) throw new RuntimeException("cantFindGitDirectory")
    rb.build
  }
  
  @throws(classOf[Exception])
  def createFileRepository(gitDir: String) = {
    val dst = new FileRepository(gitDir)
    dst.create()
    val dstcfg = dst.getConfig
    dstcfg.setBoolean("core", null, "bare", false)
    dstcfg.save

    log.info("Initialized empty git repository in " + gitDir)
    dst
  }
 
}
