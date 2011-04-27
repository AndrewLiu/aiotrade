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
package org.aiotrade.modules.ui.nodes

import java.awt.Image
import java.awt.event.ActionEvent
import java.beans.IntrospectionException
import java.beans.PropertyChangeEvent
import java.io.IOException
import java.io.PrintStream
import java.util.Calendar
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JOptionPane
import org.aiotrade.lib.view.securities.AnalysisChartView
import org.aiotrade.lib.view.securities.persistence.ContentParseHandler
import org.aiotrade.lib.view.securities.persistence.ContentPersistenceHandler
import javax.swing.SwingUtilities
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor
import org.aiotrade.lib.securities.dataserver.QuoteInfoContract
import org.aiotrade.lib.securities.dataserver.QuoteInfoHisContract
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.model.LightTicker
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.dataserver.MoneyFlowContract
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util.swing.action.GeneralAction
import org.aiotrade.lib.util.swing.action.SaveAction
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.aiotrade.modules.ui.actions.AddSymbolAction
import org.aiotrade.modules.ui.GroupDescriptor
import org.aiotrade.modules.ui.windows.RealTimeWatchListTopComponent
import org.aiotrade.modules.ui.dialog.ImportSymbolDialog
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.progress.ProgressUtils
import org.openide.ErrorManager
import org.openide.actions.CopyAction
import org.openide.actions.DeleteAction
import org.openide.actions.PasteAction
import org.openide.filesystems.FileLock
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.filesystems.Repository
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.loaders.DataShadow
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.nodes.NodeEvent
import org.openide.nodes.NodeListener
import org.openide.nodes.NodeMemberEvent
import org.openide.nodes.NodeOp
import org.openide.nodes.NodeReorderEvent
import org.openide.util.ImageUtilities
import org.openide.util.Lookup
import org.openide.util.actions.SystemAction
import org.openide.util.lookup.AbstractLookup
import org.openide.util.lookup.InstanceContent
import org.openide.util.lookup.ProxyLookup
import org.openide.windows.WindowManager
import org.openide.xml.XMLUtil
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import scala.collection.mutable




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

  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.nodes.Bundle")

  private val DEFAUTL_SOURCE_ICON = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/symbol.gif")

  private val contentToOccuptantNode = mutable.Map[Content, Node]()

  private val symbolNodeToSymbol = new mutable.WeakHashMap[OneSymbolNode, String]

  private val folderIcon = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/market.png")
  private val stockIcon  = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/stock.png")

  val favoriteFolderName = "Favorite"

  private var _favoriteNode: SymbolFolderNode = _
  def favoriteNode = _favoriteNode

  def occupantNodeOf(content: Content): Option[Node] =  {
    contentToOccuptantNode.get(content)
  }

  def occupiedContentOf(node: Node): Option[Content] = {
    contentToOccuptantNode find {case (content, aNode) => (aNode eq node)} map (_._1)
  }

  def contentOf(symbol: String): Option[Content] = {
    contentToOccuptantNode.keySet find {_.uniSymbol == symbol}
  }

  def nodeOf(symbol: String): Option[Node] = {
    contentOf(symbol) map occupantNodeOf get
  }

  def putNode(content: Content, node: Node) {
    contentToOccuptantNode += (content -> node)
  }

  /**
   * Remove node will not remove the content, we prefer content instances
   * long lives in application context, so if node is moved to other place, we
   * can just pick a content from here (if exists) instead of reading from xml
   * file, and thus makes the opened topcomponent needn't to refer to a new
   * created content instance.
   * So, just do
   * <code>putNode(content, null)</code>
   */
  def removeNode(node: Node) {
    /**
     * @NOTICE
     * When move a node from a folder to another folder, a new node could
     * be created first, then the old node is removed. so the nodeMap may
     * has been updated by the new node, and lookupContent(node) will
     * return a null since it lookup via the old node.
     * Check it here
     */
    occupiedContentOf(node) foreach {content =>
      contentToOccuptantNode -= content
    }
  }

  def createSymbolXmlFile(folder: DataFolder, symbol: String): Option[FileObject] =  {
    val folderObject = folder.getPrimaryFile
    val fileName = symbol
    
    if (folderObject.getFileObject(fileName, "sec") != null) {
      log.warning("Symbol :" + symbol + " under " + folder + " has existed.")
      return None
    }

    var lock: FileLock = null
    var out: PrintStream = null
    try {
      val fo = folderObject.createData(fileName, "sec")
      lock = fo.lock
      out = new PrintStream(fo.getOutputStream(lock))

      val content = PersistenceManager().defaultContent
      content.uniSymbol = symbol
      content.lookupDescriptors(classOf[DataContract[_]]) foreach {_.srcSymbol = symbol}
      out.print(ContentPersistenceHandler.dumpContent(content))

      Option(fo)
    } catch {
      case ex: IOException => ErrorManager.getDefault.notify(ex); None
    } finally {
      /** should remember to out.close() here */
      if (out != null) out.close
      if (lock != null) lock.releaseLock
    }

  }

  /** Deserialize a Symbol from xml file */
  private def readContent(node: Node): Option[Content] = {
    val fo = node.getLookup.lookup(classOf[DataObject]) match {
      case null => throw new IllegalStateException("Bogus file in Symbols folder: " + node.getLookup.lookup(classOf[FileObject]))
      case shadow: DataShadow => shadow.getOriginal.getPrimaryFile
      case dobj: DataObject => dobj.getPrimaryFile
    }
    readContent(fo)
  }

  private def readContent(fo: FileObject): Option[Content] = {
    var is = fo.getInputStream
    try {
      val xmlReader = XMLUtil.createXMLReader
      val handler = new ContentParseHandler
      xmlReader.setContentHandler(handler)
      xmlReader.parse(new InputSource(is))

      Some(handler.getContent)
    } catch {
      case ex: IOException  => ErrorManager.getDefault.notify(ex); None
      case ex: SAXException => ErrorManager.getDefault.notify(ex); None
    } finally {
      if (is != null) is.close
    }
  }

  def findSymbolNode(symbol: String): Option[OneSymbolNode] = {
    val rootFolder = RootSymbolsNode.getLookup.lookup(classOf[DataFolder])
    if (rootFolder != null) {
      val children = rootFolder.getChildren
      var i = 0
      while (i < children.length) {
        children(i) match {
          case x: DataFolder =>
            findSymbolNode(x, symbol) match {
              case None =>
              case some => return some
            }
          case _ =>
        }
        i += 1
      }
      None
    } else None
  }

  def findSymbolNode(folderNode: SymbolFolderNode, symbol: String): Option[OneSymbolNode] = {
    folderNode.getLookup.lookup(classOf[DataFolder]) match {
      case null => None
      case folder: DataFolder => findSymbolNode(folder, symbol)
    }
  }

  def findSymbolNode(folder: DataFolder, symbol: String): Option[OneSymbolNode] = {
    symbolNodeToSymbol find (_._2 == symbol) match {
      case None =>
        val children = folder.getChildren
        var i = 0
        while (i < children.length) {
          children(i) match {
            case x: DataFolder =>
            case x: DataObject =>
              if (symbolOf(x.getPrimaryFile) == symbol) {
                return Some(new OneSymbolNode(x.getNodeDelegate))
              }
          }
          i += 1
        }
        
        None
      case Some(x) => Some(x._1)
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

  private def getFolderNode(node: Node): Option[SymbolFolderNode] = {
    node match {
      case x: SymbolFolderNode => Some(x)
      case x: OneSymbolNode =>
        val parent = node.getParentNode
        if (parent != null) {
          getFolderNode(parent)
        } else None
    }
  }

  def openAllSymbolFolders {
    val rootFolder = RootSymbolsNode.getLookup.lookup(classOf[DataFolder])
    if (rootFolder != null) {
      val children = rootFolder.getChildren
      var i = 0
      while (i < children.length) {
        children(i) match {
          case x: DataFolder => openSymbolFolder(x)
          case _ =>
        }
        i += 1
      }
    }
  }

  def openSymbolFolder(folder: DataFolder) {
    val symbolFolderNode = try {
      NodeOp.findPath(RootSymbolsNode, Array(folder.getName)) match {
        case x: SymbolFolderNode => x
        case _ => return
      }
    } catch {
      case _ => return
    }

    val start = System.currentTimeMillis
    log.info("Opening folder: " + folder)
    val uniSymbols = symbolsOf(folder)
    log.info("Symbols under folder: " + folder + " were collected: " + uniSymbols.length)
    watchSymbols(symbolFolderNode, uniSymbols)
    log.info("Opened folder: " + folder + " in " + (System.currentTimeMillis - start) + " ms")
  }

  private def symbolOf(fo: FileObject): String = {
    val name = fo.getName
    val extIdx = name.indexOf(".sec")
    if (extIdx > 0) {
      name.substring(0, extIdx)
    } else name
  }

  private def symbolsOf(folder: DataFolder): Array[String] = {
    val children = folder.getChildren
    val uniSymbols = new ArrayList[String]
    var i = 0
    while (i < children.length) {
      children(i) match {
        case x: DataFolder =>
        case x: DataObject =>
          uniSymbols += symbolOf(x.getPrimaryFile)
      }
      i += 1
    }

    uniSymbols.toArray
  }

  private def watchSymbols(folderNode: SymbolFolderNode, uniSymbols: Array[String]) {
    val watchListTc = RealTimeWatchListTopComponent.getInstance(folderNode)
    watchListTc.requestActive

    val lastTickers = new ArrayList[LightTicker]
    var i = 0
    while (i < uniSymbols.length) {
      val uniSymbol = uniSymbols(i)
      Exchange.secOf(uniSymbol) match {
        case Some(sec) =>
          watchListTc.watch(sec)
          sec.exchange.uniSymbolToLastTradingDayTicker.get(uniSymbol) foreach (lastTickers += _)
        case None =>
      }
      i += 1
    }

    watchListTc.watchListPanel.updateByTickers(lastTickers.toArray)
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
      Bundle.getString("SN_title")
    }
  }

  /** Getting the Symbol node and wrapping it in a FilterNode */
  class OneSymbolNode private (symbolFileNode: Node, ic: InstanceContent
  ) extends FilterNode(symbolFileNode, new SymbolChildren, new ProxyLookup(symbolFileNode.getLookup,
                                                                           new AbstractLookup(ic))
  ) {

    val content = readContent(symbolFileNode) match {
      case Some(content) =>
        // check if has existed in application context, if true, use the existed one
        val content1 = contentOf(content.uniSymbol).getOrElse(content)
        putNode(content1, this)
        ic.add(content1)
        content1
      case None => null
    }

    symbolNodeToSymbol.put(this, content.uniSymbol)

    /* add additional items to the lookup */
    ic.add(new SymbolViewAction(this))
    ic.add(new SymbolReimportDataAction(this))
    ic.add(new SymbolRefreshDataAction(this))
    ic.add(new SymbolSetDataSourceAction(this))
    ic.add(new SymbolStartWatchAction(this))
    ic.add(new SymbolStopWatchAction(this))
    ic.add(new SymbolCompareToAction(this))
    ic.add(new SymbolClearDataAction(this))
    ic.add(new SymbolAddToFavoriteAction(this))

    log.info("OneSymbolNode(" + content.uniSymbol + ") created.")

    /* As the lookup needs to be constucted before Node's constructor is called,
     * it might not be obvious how to add Node or other objects into it without
     * type casting. Here is the recommended suggestion that uses public/private
     * pair of constructors:
     */
    @throws(classOf[IOException])
    @throws(classOf[IntrospectionException])
    def this(symbolFileNode: Node) = this(symbolFileNode, new InstanceContent)

    override def getDisplayName = {
      val uniSymbol = content.uniSymbol
      Exchange.secOf(uniSymbol) match {
        case Some(sec) => uniSymbol + " (" + sec.name + ")"
        case None => uniSymbol
      }
    }

    override def getIcon(tpe: Int): Image = {
      stockIcon
    }

    override def getOpenedIcon(tpe: Int): Image = {
      getIcon(0)
    }

    override def canRename = false

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
        null,
        SystemAction.get(classOf[CopyAction]),
        SystemAction.get(classOf[DeleteAction]),
        null,
        getLookup.lookup(classOf[SymbolAddToFavoriteAction])
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
          if (occupiedContentOf(OneSymbolNode.this) != null) {
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
           * else, create a sec node for it, which will deserilize a content instance from the sec xml file,
           */
          val fo = symbolFileNode.getLookup.lookup(classOf[DataObject]).getPrimaryFile
          val uniSymbol = symbolOf(fo)
          val oneSymbolNode = symbolNodeToSymbol find (_._2 == uniSymbol) match {
            case Some(x) => x._1
            case None => new OneSymbolNode(symbolFileNode)
          }

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
  private class SymbolChildren extends Children.Keys[GroupDescriptor[Descriptor[_]]] {

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
    private val bufChildrenKeys = new java.util.HashSet[GroupDescriptor[Descriptor[_]]]()

    @unchecked
    override protected def addNotify {
      val groups = PersistenceManager().lookupAllRegisteredServices(classOf[GroupDescriptor[Descriptor[_]]],
                                                                    "DescriptorGroups")

      bufChildrenKeys.clear
      /** each symbol should create new NodeInfo instances that belongs to itself */
      for (nodeInfo <- groups) {
        bufChildrenKeys add nodeInfo.clone.asInstanceOf[GroupDescriptor[Descriptor[_]]]
      }
      setKeys(bufChildrenKeys)
    }

    def createNodes(key: GroupDescriptor[Descriptor[_]]): Array[Node] = {
      try {
        // lookup Content in parent node
        val content = this.getNode.getLookup.lookup(classOf[Content])
        Array(new GroupNode(key, content))
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
  class SymbolFolderNode(folderNode: Node, ic: InstanceContent
  ) extends FilterNode(folderNode, new SymbolFolderChildren(folderNode), new ProxyLookup(folderNode.getLookup,
                                                                                         new AbstractLookup(ic))
  ) {

    if (folderNode.getName == favoriteFolderName) {
      _favoriteNode = this
    }

    /* add additional items to the lookup */
    ic.add(SystemAction.get(classOf[AddSymbolAction]))
    ic.add(new SymbolStartWatchAction(this))
    ic.add(new SymbolStopWatchAction(this))
    ic.add(new SymbolRefreshDataAction(this))
    ic.add(new SymbolReimportDataAction(this))
    ic.add(new SymbolViewAction(this))

    this.addNodeListener(new NodeListener {
        def childrenAdded(nodeMemberEvent: NodeMemberEvent) {
          // Is this node added to a folder that has an opened corresponding watchlist tc ?
          RealTimeWatchListTopComponent.instanceOf(SymbolFolderNode.this) match {
            case Some(listTc) if listTc.isOpened =>
              nodeMemberEvent.getDelta foreach {
                case node: OneSymbolNode =>
                  val content = node.content
                  Exchange.secOf(content.uniSymbol) foreach {sec =>
                    content.lookupActiveDescriptor(classOf[QuoteContract]) foreach {contract => sec.quoteContracts = List(contract)}
                    content.lookupActiveDescriptor(classOf[MoneyFlowContract]) foreach {contract => sec.moneyFlowContracts = List(contract)}
                    content.serProvider = sec
                    
                    sec.subscribeTickerServer(true)
                    listTc.watch(sec)
                    node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(false)
                    node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
                  }
                case _ =>
              }
            case _ =>
          }
        }

        def childrenRemoved(nodeMemberEvent: NodeMemberEvent) {
          // Is this node added to a folder that has an opened corresponding watchlist tc ?
          RealTimeWatchListTopComponent.instanceOf(SymbolFolderNode.this) match {
            case Some(listTc) if listTc.isOpened =>
              nodeMemberEvent.getDelta foreach {
                case node: OneSymbolNode =>
                  val content = node.content
                  Exchange.secOf(content.uniSymbol) foreach {sec =>
                    content.lookupActiveDescriptor(classOf[QuoteContract]) foreach {contract => sec.quoteContracts = List(contract)}
                    content.lookupActiveDescriptor(classOf[MoneyFlowContract]) foreach {contract => sec.moneyFlowContracts = List(contract)}
                    content.serProvider = sec

                    sec.unSubscribeTickerServer
                    listTc.unWatch(sec)
                    node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
                    node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(false)
                  }
                case _ =>
              }
            case _ =>
          }
        }

        def childrenReordered(nodeReorderEvent: NodeReorderEvent) {}
        def nodeDestroyed(nodeEvent: NodeEvent) {}
        def propertyChange(evt: PropertyChangeEvent) {}
      })

    /**
     * Declaring the children of the root sec node
     *
     *
     * @param symbolFolderNode: the folder or file('stockname.ser') which delegated to this node
     */
    @throws(classOf[DataObjectNotFoundException])
    @throws(classOf[IntrospectionException])
    def this(symbolFolderNode: Node) = this(symbolFolderNode, new InstanceContent)

    /** Declaring the actions that can be applied to this node */
    override def getActions(popup: Boolean): Array[Action] = {
      val df = getLookup.lookup(classOf[DataFolder])
      Array(
        getLookup.lookup(classOf[SymbolStartWatchAction]),
        getLookup.lookup(classOf[SymbolStopWatchAction]),
        null,
        getLookup.lookup(classOf[AddSymbolAction]),
        new AddFolderAction(df),
        null,
        getLookup.lookup(classOf[SymbolViewAction]),
        null,
        getLookup.lookup(classOf[SymbolRefreshDataAction]),
        getLookup.lookup(classOf[SymbolReimportDataAction]),
        null,
        SystemAction.get(classOf[PasteAction]),
        SystemAction.get(classOf[DeleteAction])
      )
    }

    override def getIcon(tpe: Int): Image = {
      folderIcon
    }

    override def getOpenedIcon(tpe: Int): Image = {
      getIcon(0)
    }

    override def getDisplayName: String = {
      if (Bundle.containsKey(getName)) Bundle.getString(getName) else getName
    }
  }

  // ----- node actions

  private class SymbolViewAction(node: Node) extends ViewAction {
    putValue(Action.NAME, Bundle.getString("AC_view"))

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        /* for (child <- node.getChildren.getNodes) {
         child.getLookup.lookup(classOf[ViewAction]).execute
         } */
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val content = node.getLookup.lookup(classOf[Content])
      var mayNeedsReload = false
      Exchange.secOf(content.uniSymbol) match {
        case Some(sec) =>
          content.serProvider = sec
          content.lookupActiveDescriptor(classOf[QuoteContract]) foreach {contract => sec.quoteContracts = List(contract)}
          content.lookupActiveDescriptor(classOf[MoneyFlowContract]) foreach {contract => sec.moneyFlowContracts = List(contract)}
          content.lookupActiveDescriptor(classOf[QuoteInfoContract]) foreach {contract => sec.quoteInfoContract =contract }
          content.lookupActiveDescriptor(classOf[QuoteInfoHisContract]) foreach {contract => sec.quoteInfoHisContracts = List(contract)}
          
          val standalone = getValue(AnalysisChartTopComponent.STANDALONE) match {
            case null => false
            case x => x.asInstanceOf[Boolean]
          }
          
          log.info("Open standalone AnalysisChartTopComponent: " + standalone)
          val analysisTc = AnalysisChartTopComponent(content, standalone)
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
    putValue(Action.NAME, Bundle.getString("AC_start_watching"))
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/resources/startWatch.gif")

    def execute {
      val folderNode = getFolderNode(node) getOrElse (return)
      val handle = ProgressHandleFactory.createHandle(Bundle.getString("MSG_init_symbols") + " " + node.getDisplayName + " ...")
      ProgressUtils.showProgressDialogAndRun(new Runnable {
          def run {
            log.info("Start collecting node children")
            val content = getContentViaNode(node, handle)
            handle.finish
            log.info("Finished collecting node children: " + content.length)
            watchSymbols(folderNode, content)
          }
        }, handle, false)
    }

    /** Not as efficient as getSymbolContentViaFolder if nodes were not inited previously */
    private def getContentViaNode(node: Node, handle: ProgressHandle) = {
      val symbolNodes = new ArrayList[Node]
      collectSymbolNodes(node, symbolNodes, handle)
      symbolNodes map (_.getLookup.lookup(classOf[Content]))
    }

    private def collectSymbolNodes(node: Node, symbolNodes: ArrayList[Node], handle: ProgressHandle) {
      node match {
        case x: OneSymbolNode => symbolNodes += node
        case x: SymbolFolderNode =>
          /** it's a folder, go recursively */
          val children = node.getChildren
          val count = children.getNodesCount
          handle.switchToDeterminate(count)
          var i = 0
          while (i < count) {
            handle.progress(i)
            val node_i = children.getNodeAt(i)
            collectSymbolNodes(node_i, symbolNodes, handle)
            i += 1
          }
        case _ =>
      }
    }

    private def getContentViaFolder(node: Node, handle: ProgressHandle) = {
      val contents = new ArrayList[Content]

      val folder = node.getLookup.lookup(classOf[DataFolder])
      if (folder == null) {
        // it's an OneSymbolNode, do real things
        readContent(node) foreach (contents += _)
      } else {
        collectSymbolContents(folder, contents, handle)
      }
      contents
    }

    private def collectSymbolContents(dob: DataObject, contents: ArrayList[Content], handle: ProgressHandle) {
      dob match {
        case x: DataFolder =>
          /** it's a folder, go recursively */
          val children = x.getChildren
          val count = children.length
          handle.switchToDeterminate(count)
          var i = 0
          while (i < count) {
            handle.progress(i)
            val child = children(i)
            collectSymbolContents(child, contents, handle)
            i += 1
          }
        case x: DataObject =>
          val fo = x.getPrimaryFile
          readContent(fo) foreach (contents += _)
        case x => log.warning("Unknown DataObject: " + x)
      }
    }

    private def watchSymbols(folderNode: SymbolFolderNode, symbolContents: ArrayList[Content]) {
      node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
      this.setEnabled(false)

      watchSymbolInFolder(folderNode, symbolContents.toArray)
    }
  }

  class SymbolStopWatchAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_stop_watching"))
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/resources/stopWatch.gif")

    if (node.getLookup.lookup(classOf[DataFolder]) != null) {
      this.setEnabled(true)
    } else {
      this.setEnabled(false)
    }

    def execute {
      node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
      this.setEnabled(false)

      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolStopWatchAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val content = node.getLookup.lookup(classOf[Content])
      Exchange.secOf(content.uniSymbol) match {
        case Some(sec) =>
          sec.unSubscribeTickerServer

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
//      RealTimeBoardTopComponent(content) foreach {rtBoardWin =>
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
    putValue(Action.NAME, Bundle.getString("AC_clear_data"))

    def perform(shouldConfirm: Boolean) {
      /**
       * don't get descriptors from getLookup.lookup(..), becuase
       * if node destroy is invoked by parent node, such as folder,
       * the lookup content may has been destroyed before node destroyed.
       */
      occupiedContentOf(node) foreach {content =>
        val confirm = if (shouldConfirm) {
          JOptionPane.showConfirmDialog(
            WindowManager.getDefault.getMainWindow(),
            "Are you sure you want to clear data of : " + content.uniSymbol + " ?",
            "Clearing data ...",
            JOptionPane.YES_NO_OPTION
          )
        } else JOptionPane.YES_OPTION

        if (confirm == JOptionPane.YES_OPTION) {
          val symbol = content.uniSymbol
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
    putValue(Action.NAME, Bundle.getString("AC_reimport_data"))

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
        }
        return
      }


      /** otherwise, it's an OneSymbolNode, do real things */
      val content = node.getLookup.lookup(classOf[Content])
      val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get

      val cal = Calendar.getInstance
      cal.clear

      cal.setTime(quoteContract.beginDate)
      val fromTime = cal.getTimeInMillis

      val freq = quoteContract.freq
      PersistenceManager().deleteQuotes(content.uniSymbol, freq, fromTime, Long.MaxValue)

      var sec = content.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(content.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(content.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        content.serProvider = sec
      } else {
        sec.dataContract = quoteContract
      }

      /**
       * @TODO
       * need more works, the clear(long) in default implement of Ser doesn't work good!
       */
      sec.resetSers
      val ser = sec.serOf(freq).get

      node.getLookup.lookup(classOf[ViewAction]).execute
    }
  }

  private class SymbolRefreshDataAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_refresh_data"))

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolRefreshDataAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val content = node.getLookup.lookup(classOf[Content])
      val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = content.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(content.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(content.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        content.serProvider = sec
      } else {
        sec.dataContract = quoteContract
      }

      sec.resetSers
      val ser = sec.serOf(quoteContract.freq).get

      node.getLookup.lookup(classOf[ViewAction]).execute
    }
  }

  private class SymbolSetDataSourceAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_set_data_source"))

    def execute {
      val content = node.getLookup.lookup(classOf[Content])

      val pane = new ImportSymbolDialog(
        WindowManager.getDefault.getMainWindow,
        content.lookupActiveDescriptor(classOf[QuoteContract]).getOrElse(null),
        false)
      if (pane.showDialog != JOptionPane.OK_OPTION) {
        return
      }

      content.lookupAction(classOf[SaveAction]) foreach {_.execute}
      node.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
    }
  }

  private class SymbolCompareToAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_compare_to_current"))

    def execute {
      val content = node.getLookup.lookup(classOf[Content])
      val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = content.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        //sec = new Sec(content.uniSymbol, List(quoteContract))
        sec = Exchange.secOf(content.uniSymbol) getOrElse (return)
        sec.quoteContracts = List(quoteContract)
        content.serProvider = sec
      }

      val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}

      val serToBeCompared = sec.serOf(quoteContract.freq).get
      if (!serToBeCompared.isLoaded) {
        val loadBegins = sec.loadSer(serToBeCompared)
      }

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

  private class SymbolAddToFavoriteAction(node: Node) extends AddToFavoriteAction {
    putValue(Action.NAME, Bundle.getString("AC_add_to_favorite"))

    def execute {
      val dobj = node.getLookup.lookup(classOf[DataObject])
      val favFolder = favoriteNode.getLookup.lookup(classOf[DataFolder])
      if (!favFolder.getChildren.exists(_.getName == node.getName)) {
        dobj.createShadow(favFolder)
      }

      val content = node.getLookup.lookup(classOf[Content])
      watchSymbolInFolder(favoriteNode, Array(content))
    }
  }

  /** Creating an action for adding a folder to organize stocks into groups */
  private class AddFolderAction(folder: DataFolder) extends AbstractAction {
    putValue(Action.NAME, Bundle.getString("AC_add_folder"))

    def actionPerformed(ae: ActionEvent) {
      var floderName = JOptionPane.showInputDialog(
        WindowManager.getDefault.getMainWindow,
        Bundle.getString("SN_askfolder_msg"),
        Bundle.getString("AC_add_folder"),
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

  private def watchSymbolInFolder(folderNode: SymbolFolderNode, symbolContents: Array[Content]) {
    val watchListTc = RealTimeWatchListTopComponent.getInstance(folderNode)
    watchListTc.requestActive

    val lastTickers = new ArrayList[LightTicker]
    var i = 0
    while (i < symbolContents.length) {
      val content = symbolContents(i)
      val uniSymbol = content.uniSymbol
      Exchange.secOf(uniSymbol) match {
        case Some(sec) =>
          watchListTc.watch(sec)
          sec.exchange.uniSymbolToLastTradingDayTicker.get(uniSymbol) foreach (lastTickers += _)
        case None =>
      }
      
      i += 1
    }

    watchListTc.watchListPanel.updateByTickers(lastTickers.toArray)
  }

}

abstract class AddToFavoriteAction extends GeneralAction
