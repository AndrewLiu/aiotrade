/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.modules.ui.netbeans.nodes

import java.awt.Image
import java.awt.event.ActionEvent
import java.beans.IntrospectionException
import java.beans.PropertyChangeEvent
import java.io.IOException
import java.io.PrintStream
import java.util.Calendar
import java.util.logging.Logger
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JOptionPane
import org.aiotrade.lib.view.securities.AnalysisChartView
import org.aiotrade.lib.view.securities.persistence.ContentsParseHandler
import org.aiotrade.lib.view.securities.persistence.ContentsPersistenceHandler
import javax.swing.SwingUtilities
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.model.LightTicker
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util.swing.action.GeneralAction
import org.aiotrade.lib.util.swing.action.SaveAction
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.netbeans.windows.AnalysisChartTopComponent
import org.aiotrade.modules.ui.netbeans.actions.AddSymbolAction;
import org.aiotrade.modules.ui.netbeans.GroupDescriptor
import org.aiotrade.modules.ui.netbeans.windows.RealTimeWatchListTopComponent
import org.aiotrade.modules.ui.dialog.ImportSymbolDialog
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.progress.ProgressUtils
import org.openide.ErrorManager
import org.openide.actions.DeleteAction
import org.openide.filesystems.FileLock
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.filesystems.Repository
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.nodes.NodeEvent
import org.openide.nodes.NodeListener
import org.openide.nodes.NodeMemberEvent
import org.openide.nodes.NodeReorderEvent
import org.openide.util.ImageUtilities
import org.openide.util.Lookup
import org.openide.util.NbBundle
import org.openide.util.actions.SystemAction
import org.openide.util.lookup.AbstractLookup
import org.openide.util.lookup.InstanceContent
import org.openide.util.lookup.ProxyLookup
import org.openide.windows.WindowManager
import org.openide.xml.XMLUtil
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet




/**
 * SymbolNode is a representation for sec serialization file "xxx.ser"  or
 * folder contains these files.
 *
 *  The tree view of Symbol and others
 *  + Symbols (config/Symbols)
 *    +- sunw (sunw.ser)
 *       +- Indicators (DescriptorGroupNode)
 *       |  +- MACD (DescriptorNode)
 *       |  |   +-opt1
 *       |  |   +-opt2
 *       |  +- ROC
 *       |     +-opt1
 *       |     +-opt2
 *       +- Drawings (DescriptorGroupNode)
 *          +- layer1
 *          |  +- line
 *          |  +- parallel
 *          |  +- gann period
 *          +- layer2
 *
 *
 *
 *
 * @author Caoyuan Deng
 */
object SymbolNodes {
  private val log = Logger.getLogger(this.getClass.getName)

  private val DEFAUTL_SOURCE_ICON = ImageUtilities.loadImage("org/aiotrade/modules/ui/netbeans/resources/symbol.gif")

  private val contentToOccuptantNode = new HashMap[AnalysisContents, Node]

  private var isInited = false

  def initSymbolNodes {
    log.info("Start init symbol nodes")
    initSymbolNode(RootSymbolsNode)
    isInited = true
    log.info("Finished init symbol nodes")
  }

  private def initSymbolNode(node: Node) {
    if (node.getLookup.lookup(classOf[DataFolder]) != null) { // is a folder
      for (child <- node.getChildren.getNodes) {
        initSymbolNode(child)
      }
    }
  }

  def occupantNodeOf(contents: AnalysisContents): Option[Node] =  {
    contentToOccuptantNode.get(contents)
  }

  def occupiedContentsOf(node: Node): Option[AnalysisContents] = {
    contentToOccuptantNode find {case (contents, aNode) => (aNode eq node)} map (_._1)
  }

  def contentsOf(symbol: String): Option[AnalysisContents] = {
    contentToOccuptantNode.keySet find {_.uniSymbol == symbol}
  }

  def nodeOf(symbol: String): Option[Node] = {
    contentsOf(symbol) map occupantNodeOf get
  }

  def putNode(contents: AnalysisContents, node: Node) {
    contentToOccuptantNode += (contents -> node)
  }

  /**
   * Remove node will not remove the contents, we prefer contents instances
   * long lives in application context, so if node is moved to other place, we
   * can just pick a contents from here (if exists) instead of reading from xml
   * file, and thus makes the opened topcomponent needn't to refer to a new
   * created contents instance.
   * So, just do
   * <code>putNode(contents, null)</code>
   */
  def removeNode(node: Node) {
    /**
     * @NOTICE
     * When move a node from a folder to another folder, a new node could
     * be created first, then the old node is removed. so the nodeMap may
     * has been updated by the new node, and lookupContents(node) will
     * return a null since it lookup via the old node.
     * Check it here
     */
    occupiedContentsOf(node) foreach {contents =>
      contentToOccuptantNode -= contents
    }
  }

  def createSymbolXmlFile(folder: DataFolder, symbol: String, quoteContracts: QuoteContract*): Option[FileObject] =  {
    val folderObject = folder.getPrimaryFile
    val fileName = symbol
    
    if (folderObject.getFileObject(fileName, "sec") != null) {
      log.warning("Symbol :" + symbol + " under " + folder + " has existed.")
      return None
    }

    var lock: FileLock = null
    var out: PrintStream = null
    var fo: FileObject = null
    try {
      fo = folderObject.createData(fileName, "sec")
      lock = fo.lock
      out = new PrintStream(fo.getOutputStream(lock))

      val contents = PersistenceManager().defaultContents
      /** clear default dataSourceContract */
      contents.clearDescriptors(classOf[DataContract[_, _]])

      contents.uniSymbol = symbol
      for (quoteContract <- quoteContracts) {
        contents.addDescriptor(quoteContract)
      }

      out.print(ContentsPersistenceHandler.dumpContents(contents))

    } catch {
      case ex: IOException => ErrorManager.getDefault.notify(ex)
    } finally {
      /** should remember to out.close() here */
      if (out != null) out.close
      if (lock != null) lock.releaseLock
    }

    Some(fo)
  }

  /** Deserialize a Symbol from xml file */
  private def readContents(node: Node): Option[AnalysisContents] = {
    val dobj = node.getLookup.lookup(classOf[DataObject])
    if (dobj == null) {
      throw new IllegalStateException("Bogus file in Symbols folder: " + node.getLookup.lookup(classOf[FileObject]))
    }
    val fo = dobj.getPrimaryFile
    readContents(fo)
  }

  private def readContents(fo: FileObject): Option[AnalysisContents] = {
    var is = fo.getInputStream
    try {
      val xmlReader = XMLUtil.createXMLReader
      val handler = new ContentsParseHandler
      xmlReader.setContentHandler(handler)
      xmlReader.parse(new InputSource(is))

      Some(handler.getContents)
    } catch {
      case ex: IOException  => ErrorManager.getDefault.notify(ex); None
      case ex: SAXException => ErrorManager.getDefault.notify(ex); None
    } finally {
      if (is != null) is.close
    }
  }

  def findSymbolNode(symbol: String): Option[Node] = {
    findSymbolNode(RootSymbolsNode, symbol)
  }

  private def findSymbolNode(node: Node, symbol: String): Option[Node] = {
    if (node.getLookup.lookup(classOf[DataFolder]) == null) { // not a folder
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      if (contents != null && contents.uniSymbol == symbol) {
        Some(node)
      } else None
    } else {
      for (child <- node.getChildren.getNodes) {
        findSymbolNode(child, symbol) match {
          case None =>
          case some => return some
        }
      }
      None
    }
  }

  private def displayNameOf(node: Node): String = {
    if (node.getLookup.lookup(classOf[DataFolder]) != null) {
      Exchange.allExchanges find (_.code == node.getName) match {
        case Some(x) => x.shortDescription
        case None => node.getName
      }
    } else {
      // @todo symbol local name + (symbol)?
      node.getName
    }
  }
  
  // ----- Node classes

  /**
   * The root node of SymbolNode
   * It will be 'symbols' folder in default file system, usually the 'config' dir in userdir
   * Physical folder "symbols" is defined in layer.xml
   */
  @throws(classOf[DataObjectNotFoundException])
  @throws(classOf[IntrospectionException])
  object RootSymbolsNode extends SymbolFolderNode(
    DataObject.find(FileUtil.getConfigFile("symbols")).getNodeDelegate
  ) {

    override def getDisplayName = {
      NbBundle.getMessage(classOf[SymbolFolderNode], "SN_title")
    }
  }

  /** Getting the Symbol node and wrapping it in a FilterNode */
  class OneSymbolNode private (symbolFileNode: Node, content: InstanceContent
  ) extends FilterNode(symbolFileNode, new SymbolChildren, new ProxyLookup(symbolFileNode.getLookup,
                                                                           new AbstractLookup(content))
  ) {
    private var analysisContents: AnalysisContents = _

    readContents(symbolFileNode) match {
      case Some(contents) =>
        // check if has existed in application context, if true, use the existed one
        analysisContents = contentsOf(contents.uniSymbol).getOrElse(contents)
        putNode(analysisContents, this)
        content.add(analysisContents)
      case None =>
    }

    /* add the node to our own lookup */
    content.add(this)

    /* add additional items to the lookup */
    content.add(new SymbolViewAction(this))
    content.add(new SymbolReimportDataAction(this))
    content.add(new SymbolRefreshDataAction(this))
    content.add(new SymbolSetDataSourceAction(this))
    content.add(new SymbolStartWatchAction(this))
    content.add(new SymbolStopWatchAction(this))
    content.add(new SymbolCompareToAction(this))
    content.add(new SymbolClearDataAction(this))

    /* As the lookup needs to be constucted before Node's constructor is called,
     * it might not be obvious how to add Node or other objects into it without
     * type casting. Here is the recommended suggestion that uses public/private
     * pair of constructors:
     */
    @throws(classOf[IOException])
    @throws(classOf[IntrospectionException])
    def this(symbolFileNode: Node) = {
      this(symbolFileNode, new InstanceContent)
    }

    override def getDisplayName = {
      analysisContents.uniSymbol
    }

    override def getIcon(tpe: Int): Image = {
      val icon_? = analysisContents.lookupActiveDescriptor(classOf[QuoteContract]) map (_.icon) get

      icon_? getOrElse DEFAUTL_SOURCE_ICON
    }

    override def getOpenedIcon(tpe: Int): Image = {
      getIcon(0)
    }

    override def getActions(context: Boolean): Array[Action] = {
      Array(
        getLookup.lookup(classOf[SymbolViewAction]),
        getLookup.lookup(classOf[SymbolRefreshDataAction]),
        getLookup.lookup(classOf[SymbolReimportDataAction]),
        null,
        getLookup.lookup(classOf[SymbolStartWatchAction]),
        getLookup.lookup(classOf[SymbolStopWatchAction]),
        null,
        getLookup.lookup(classOf[SymbolCompareToAction]),
        null,
        getLookup.lookup(classOf[SymbolSetDataSourceAction]),
        null,
        getLookup.lookup(classOf[SymbolClearDataAction]),
        SystemAction.get(classOf[DeleteAction])
      )
    }

    /**
     * The getPreferredAction() simply returns the action that should be
     * run if the user double-clicks this node
     */
    override def getPreferredAction: Action = {
      getActions(true)(0)
    }

    override protected def createNodeListener: NodeListener = {
      val delegate = super.createNodeListener
      val newListener = new NodeListener {

        def childrenAdded(nodeMemberEvent: NodeMemberEvent) {
          delegate.childrenAdded(nodeMemberEvent)
        }

        def childrenRemoved(nodeMemberEvent: NodeMemberEvent) {
          delegate.childrenRemoved(nodeMemberEvent)
        }

        def childrenReordered(nodeReorderEvent: NodeReorderEvent) {
          delegate.childrenReordered(nodeReorderEvent)
        }

        def nodeDestroyed(nodeEvent: NodeEvent) {
          /**
           * We should check if this is a delete call, and clear data in db
           * only when true, since it will also be called when you move a
           * node from a folder to another.
           * The checking is simplely like the following code:
           * if returns null, means another copy-pasted node has been created,
           * and owned the descriptors now, so returns null, in this case,
           * it's a moving call. Otherwise, if returns no null, we are sure
           * this is a real delete call other than moving.
           * @NOTICE
           * Here we should find via OneSymbolNode.this instead of nodeEvent.getNode(),
           * which may return the delegated node.
           */
          if (occupiedContentsOf(OneSymbolNode.this) != null) {
            getLookup.lookup(classOf[SymbolClearDataAction]).perform(false)
          }

          removeNode(nodeEvent.getNode)
          delegate.nodeDestroyed(nodeEvent)
        }

        def propertyChange(evt: PropertyChangeEvent) {
          delegate.propertyChange(evt)
        }
      }
      newListener
    }
  }
  
  /**
   * The child of the folder node, it may be a folder or Symbol ser file
   */
  private class SymbolFolderChildren(symbolFolderNode: Node) extends FilterNode.Children(symbolFolderNode) {

    /**
     * @param a file node
     */
    override protected def createNodes(key: Node): Array[Node] = {
      val symbolFileNode = key

      try {
        if (symbolFileNode.getLookup.lookup(classOf[DataFolder]) != null) {
          /** it's a folder, so creat a folder node */
          return Array(new SymbolFolderNode(symbolFileNode))
        } else {
          /**
           * else, create a sec node for it, which will deserilize a contents instance from the sec xml file,
           */
          val oneSymbolNode = new OneSymbolNode(symbolFileNode)
          val fo = symbolFileNode.getLookup.lookup(classOf[DataObject]).getPrimaryFile

          // with "open" hint ?
          fo.getAttribute("open") match {
            case attr: java.lang.Boolean if attr.booleanValue =>
              fo.setAttribute("open", null)

              // @Error when a /** */ at there, causes syntax highlighting disappear, but /* */ is ok
              // open it
              SwingUtilities.invokeLater(new Runnable {
                  def run {
                    oneSymbolNode.getLookup.lookup(classOf[ViewAction]).execute
                  }
                })
            case _ =>
          }

          return Array(oneSymbolNode)

        }
      } catch {
        case ioe: IOException => ErrorManager.getDefault.notify(ioe)
        case exc: IntrospectionException => ErrorManager.getDefault.notify(exc)
      }

      // Some other type of Node (gotta do something)
      Array(symbolFileNode)
    }
  }

  /**
   * The children wrap class
   * ------------------------------------------------------------------------
   *
   * Defining the all children of a Symbol node
   * They will be representation for descriptorGroups in this case. it's simply
   * a wrap class of DescriptorGroupNode with addNotify() and createNodes()
   * implemented
   *
   * Typical usage of Children.Keys:
   *
   *  1. Subclass.
   *  2. Decide what type your key should be.
   *  3. Implement createNodes(java.lang.Object) to create some nodes (usually exactly one) per key.
   *  4. Override Children.addNotify() to construct a set of keys and set it using setKeys(Collection). The collection may be ordered.
   *  5. Override Children.removeNotify() to just call setKeys on Collections.EMPTY_SET.
   *  6. When your model changes, call setKeys with the new set of keys. Children.Keys will be smart and calculate exactly what it needs to do effficiently.
   *  7. (Optional) if your notion of what the node for a given key changes (but the key stays the same), you can call refreshKey(java.lang.Object). Usually this is not necessary.
   */
  private class SymbolChildren extends Children.Keys[GroupDescriptor[AnalysisDescriptor[_]]] {

    /**
     * Called when children are first asked for nodes. Typical implementations at this time
     * calculate their node list (or keys for Children.Keys etc.).
     *
     * !Notice: call to getNodes() inside of this method will return an empty array of nodes.
     *
     * Since setKeys(childrenKeys) will copy the elements of childrenKeys, it's safe to
     * use a repeatly used bufChildrenKeys here.
     * And, to sort them in letter order, we can use a SortedSet to copy from collection.(TODO)
     */
    private val bufChildrenKeys = new java.util.HashSet[GroupDescriptor[AnalysisDescriptor[_]]]()

    @unchecked
    override protected def addNotify {
      val groups = PersistenceManager().lookupAllRegisteredServices(classOf[GroupDescriptor[AnalysisDescriptor[_]]],
                                                                    "DescriptorGroups")

      bufChildrenKeys.clear
      /** each symbol should create new NodeInfo instances that belongs to itself */
      for (nodeInfo <- groups) {
        bufChildrenKeys add nodeInfo.clone.asInstanceOf[GroupDescriptor[AnalysisDescriptor[_]]]
      }
      setKeys(bufChildrenKeys)
    }

    def createNodes(key: GroupDescriptor[AnalysisDescriptor[_]]): Array[Node] = {
      try {
        // lookup AnalysisContents in parent node
        val analysisContents = this.getNode.getLookup.lookup(classOf[AnalysisContents])
        Array(new GroupNode(key, analysisContents))
      } catch {
        case ex: IntrospectionException =>
          ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex)
          /** Should never happen - no reason for it to fail above */
          Array(
            new AbstractNode(Children.LEAF) {
              override def getHtmlDisplayName = {
                "<font color='red'>" + ex.getMessage() + "</font>"
              }
            }
          )
      }
    }
  }
  
  @throws(classOf[IntrospectionException])
  class SymbolFolderNode(symbolFolderNode: Node, content: InstanceContent
  ) extends FilterNode(symbolFolderNode, new SymbolFolderChildren(symbolFolderNode), new ProxyLookup(symbolFolderNode.getLookup,
                                                                                                     new AbstractLookup(content))
  ) {

    /* add the node to our own lookup */
    content.add(this)

    /* add additional items to the lookup */
    content.add(SystemAction.get(classOf[AddSymbolAction]))
    content.add(new SymbolStartWatchAction(this))
    content.add(new SymbolStopWatchAction(this))
    content.add(new SymbolRefreshDataAction(this))
    content.add(new SymbolReimportDataAction(this))
    content.add(new SymbolViewAction(this))

    /**
     * Declaring the children of the root sec node
     *
     *
     * @param symbolFolderNode: the folder or file('stockname.ser') which delegated to this node
     */
    @throws(classOf[DataObjectNotFoundException])
    @throws(classOf[IntrospectionException])
    def this(symbolFolderNode: Node) = {
      this(symbolFolderNode, new InstanceContent)
    }

    /** Declaring the actions that can be applied to this node */
    override def getActions(popup: Boolean): Array[Action] = {
      val df = getLookup.lookup(classOf[DataFolder])
      Array(
        getLookup.lookup(classOf[AddSymbolAction]),
        new AddFolderAction(df),
        null,
        getLookup.lookup(classOf[SymbolViewAction]),
        null,
        getLookup.lookup(classOf[SymbolStartWatchAction]),
        getLookup.lookup(classOf[SymbolStopWatchAction]),
        null,
        getLookup.lookup(classOf[SymbolRefreshDataAction]),
        getLookup.lookup(classOf[SymbolReimportDataAction]),
        null,
        SystemAction.get(classOf[DeleteAction])
      )
    }

    override def getDisplayName: String = {
      displayNameOf(this)
    }
  }



  // ----- node actions

  private class SymbolViewAction(node: Node) extends ViewAction {
    putValue(Action.NAME, "View")

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        /* for (child <- node.getChildren.getNodes) {
         child.getLookup.lookup(classOf[ViewAction]).execute
         } */
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var mayNeedsReload = false
      Exchange.secOf(contents.uniSymbol) match {
        case Some(sec) =>
          sec.quoteContracts = List(quoteContract)
          contents.serProvider = sec
          
          val standalone = getValue(AnalysisChartTopComponent.STANDALONE) match {
            case null => false
            case x => x.asInstanceOf[Boolean]
          }
          val analysisTc = AnalysisChartTopComponent(contents, standalone)
          analysisTc.setActivatedNodes(Array(node))
          /**
           * !NOTICE
           * close a TopComponent doen's mean this TopComponent is null, it still
           * exsit, just invsible
           */
          /** if TopComponent of this stock has been shown before, should reload quote data, why */
          /* if (mayNeedsReload) {
           sec.clearSer(quoteContract.freq)
           } */

          if (!analysisTc.isOpened) {
            analysisTc.open
          }

          analysisTc.requestActive
        case None =>
      }
    }
  }

  class SymbolStartWatchAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Start Watching")
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/netbeans/resources/startWatch.gif")

    def execute {
      val folderName = getFolderName(node)
      val handle = ProgressHandleFactory.createHandle("Initing symbols of " + folderName + " ...")
      ProgressUtils.showProgressDialogAndRun(new Runnable {
          def run {
            log.info("Start collecting node children")
            val analysisContents = getAnalysisContentsViaNode(node, handle)
            handle.finish
            log.info("Finished collecting node children: " + analysisContents.length)
            watchSymbols(folderName, analysisContents)
          }
        }, handle, false)
    }

    /** Not as efficient as getSymbolContentsViaFolder if nodes were not inited previously */
    private def getAnalysisContentsViaNode(node: Node, handle: ProgressHandle) = {
      val symbolNodes = new ArrayList[Node]
      collectSymbolNodes(node, symbolNodes, handle)
      symbolNodes map (_.getLookup.lookup(classOf[AnalysisContents]))
    }

    private def getAnalysisContentsViaFolder(node: Node) = {
      val analysisContents = new ArrayList[AnalysisContents]

      val folder = node.getLookup.lookup(classOf[DataFolder])
      if (folder == null) {
        // it's an OneSymbolNode, do real things
        readContents(node) foreach (analysisContents += _)
      } else {
        collectSymbolContents(folder, analysisContents)
      }
      analysisContents
    }

    private def collectSymbolContents(dob: DataObject, analysisContents: ArrayList[AnalysisContents]) {
      dob match {
        case x: DataFolder =>
          /** it's a folder, go recursively */
          for (child <- x.getChildren) {
            collectSymbolContents(child, analysisContents)
          }
        case x: DataObject =>
          val fo = x.getPrimaryFile
          readContents(fo) foreach (analysisContents += _)
        case x => log.warning("Unknown DataObject: " + x)
      }
    }

    private def collectSymbolNodes(node: Node, symbolNodes: ArrayList[Node], handle: ProgressHandle) {
      if (node.getLookup.lookup(classOf[DataFolder]) == null) {
        // it's an OneSymbolNode, do real things
        symbolNodes += node
      } else {
        /** it's a folder, go recursively */
        val children = node.getChildren
        val nodesCount = children.getNodesCount
        handle.switchToDeterminate(nodesCount)
        var i = 0
        while (i < nodesCount) {
          handle.progress(i)
          val node_i = children.getNodeAt(i)
          collectSymbolNodes(node_i, symbolNodes, handle)
          i += 1
        }
      }
    }

    private def getFolderName(node: Node): String = {
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        displayNameOf(node)
      } else {
        getFolderName(node.getParentNode)
      }
    }

    private def watchSymbols(folderName: String, symbolContents: ArrayList[AnalysisContents]) {
      val watchListTc = RealTimeWatchListTopComponent.getInstance(folderName)
      watchListTc.requestActive

      val lastTickers = new ArrayList[LightTicker]
      val tickerServers = new HashSet[TickerServer]
      for (contents <- symbolContents) {
        val uniSymbol = contents.uniSymbol
        Exchange.secOf(uniSymbol) match {
          case Some(sec) =>
            contents.serProvider = sec
            contents.lookupActiveDescriptor(classOf[QuoteContract]) foreach {quoteContract =>
              sec.quoteContracts = List(quoteContract)
            }

            sec.subscribeTickerServer(false) match {
              case Some(tickerServer) =>
                tickerServers += tickerServer

                watchListTc.watch(sec, node)
                TickerServer.uniSymbolToLastTicker.get(uniSymbol) foreach (lastTickers += _)

                node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
                this.setEnabled(false)
              case _ =>
            }

          case None =>
        }
      }

      watchListTc.watchListPanel.updateByTickers(lastTickers.toArray)

      for (tickerServer <- tickerServers if tickerServer != null) {
        tickerServer.startRefresh(50)
      }
    }
  }

  class SymbolStopWatchAction(node: Node) extends GeneralAction {

    putValue(Action.NAME, "Stop Watching")
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/netbeans/resources/stopWatch.gif")
    if (node.getLookup.lookup(classOf[DataFolder]) != null) {
      this.setEnabled(true)
    } else {
      this.setEnabled(false)
    }

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolStopWatchAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      Exchange.secOf(contents.uniSymbol) match {
        case Some(sec) =>
          sec.unSubscribeTickerServer

          node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
          this.setEnabled(false)
        case None =>
      }



//      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
//        RealTimeWatchListTopComponent.instanceRefs.head.get.unWatch(sec)
//      }
//
//      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
//        RealTimeChartsTopComponent.instanceRefs.head.get.unWatch(sec)
//      }
//
//      RealTimeBoardTopComponent(contents) foreach {rtBoardWin =>
//        rtBoardWin.unWatch
//      }

    }
  }

  /**
   * We We shouldn't implement deleting data in db in NodeListener#nodeDestroyed(NodeEvent),
   * since  it will be called also when you move a node from a folder to another
   * folder. So we need a standalone action here.
   *
   * @TODO
   */
  class SymbolClearDataAction(node: OneSymbolNode) extends GeneralAction {

    private val CLEAR = "Clear data in database"
    putValue(Action.NAME, CLEAR)

    def perform(shouldConfirm: Boolean) {
      /**
       * don't get descriptors from getLookup.lookup(..), becuase
       * if node destroy is invoked by parent node, such as folder,
       * the lookup content may has been destroyed before node destroyed.
       */
      occupiedContentsOf(node) foreach {contents =>
        val confirm = if (shouldConfirm) {
          JOptionPane.showConfirmDialog(
            WindowManager.getDefault.getMainWindow(),
            "Are you sure you want to clear data of : " + contents.uniSymbol + " ?",
            "Clearing data ...",
            JOptionPane.YES_NO_OPTION
          )
        } else JOptionPane.YES_OPTION

        if (confirm == JOptionPane.YES_OPTION) {
          val symbol = contents.uniSymbol
          /** drop tables in database */
          PersistenceManager().dropAllQuoteTables(symbol)
        }
      }
    }

    def execute {
      perform(true)
    }
  }

  class SymbolReimportDataAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Reimport Data")

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
        }
        return
      }


      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      val cal = Calendar.getInstance
      cal.clear

      cal.setTime(quoteContract.beginDate)
      val fromTime = cal.getTimeInMillis

      val freq = quoteContract.freq
      PersistenceManager().deleteQuotes(contents.uniSymbol, freq, fromTime, Long.MaxValue)

      var sec = contents.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(contents.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(contents.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        contents.serProvider = sec
      } else {
        sec.dataContract = quoteContract
      }

      /**
       * @TODO
       * need more works, the clear(long) in default implement of Ser doesn't work good!
       */
      sec.clearSer(freq)

      node.getLookup.lookup(classOf[ViewAction]).execute
    }
  }

  private class SymbolRefreshDataAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Refresh Data")

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolRefreshDataAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = contents.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(contents.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(contents.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        contents.serProvider = sec
      } else {
        sec.dataContract = quoteContract
      }

      sec.clearSer(quoteContract.freq)

      node.getLookup.lookup(classOf[ViewAction]).execute
    }
  }

  private class SymbolSetDataSourceAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Set Data Source")

    def execute {
      val contents = node.getLookup.lookup(classOf[AnalysisContents])

      val pane = new ImportSymbolDialog(
        WindowManager.getDefault.getMainWindow,
        contents.lookupActiveDescriptor(classOf[QuoteContract]).getOrElse(null),
        false)
      if (pane.showDialog != JOptionPane.OK_OPTION) {
        return
      }

      contents.lookupAction(classOf[SaveAction]) foreach {_.execute}
      node.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
    }
  }

  private class SymbolCompareToAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Compare to Current")

    def execute {
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = contents.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(contents.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(contents.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        contents.serProvider = sec
      }

      val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}

      if (!sec.isSerLoaded(quoteContract.freq)) {
        val loadBegins = sec.loadSer(quoteContract.freq)
      }

      val serToBeCompared = sec.serOf(quoteContract.freq).get
      val viewContainer = analysisTc.viewContainer

      val baseSer = viewContainer.controller.baseSer
      val quoteCompareIndicator = new QuoteCompareIndicator(baseSer)
      quoteCompareIndicator.shortDescription = sec.uniSymbol
      quoteCompareIndicator.serToBeCompared = serToBeCompared
      quoteCompareIndicator.computeFrom(0)

      viewContainer.controller.scrollReferCursorToLeftSide
      viewContainer.masterView.asInstanceOf[AnalysisChartView].addQuoteCompareChart(quoteCompareIndicator)

      analysisTc.requestActive
    }

  }

  /** Creating an action for adding a folder to organize stocks into groups */
  private class AddFolderAction(folder: DataFolder) extends AbstractAction {
    putValue(Action.NAME, NbBundle.getMessage(classOf[SymbolFolderNode], "SN_addfolderbutton"))

    def actionPerformed(ae: ActionEvent) {
      var floderName = JOptionPane.showInputDialog(
        WindowManager.getDefault.getMainWindow,
        "Please Input Folder Name",
        "Add Folder",
        JOptionPane.OK_CANCEL_OPTION
      )

      if (floderName == null) {
        return
      }

      floderName = floderName.trim

      try {
        DataFolder.create(folder, floderName)
      } catch {case ex: IOException => ErrorManager.getDefault().notify(ex)}
    }
  }

}
