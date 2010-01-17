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
package org.aiotrade.platform.modules.netbeans.ui.explorer;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.Calendar;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.chartview.AnalysisQuoteChartView
import org.aiotrade.lib.chartview.persistence.ContentsParseHandler
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.Stock
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.util.swing.action.GeneralAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.modules.netbeans.ui.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.modules.netbeans.ui.actions.AddSymbolAction;
import org.aiotrade.platform.modules.netbeans.ui.GroupDescriptor
import org.aiotrade.platform.modules.netbeans.ui.NetBeansPersistenceManager;
import org.aiotrade.platform.modules.netbeans.ui.windows.RealTimeChartsTopComponent;
import org.aiotrade.platform.modules.netbeans.ui.windows.RealTimeBoardTopComponent;
import org.aiotrade.platform.modules.netbeans.ui.windows.RealTimeWatchListTopComponent;
import org.aiotrade.platform.modules.ui.dialog.ImportSymbolDialog;
import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.loaders.XMLDataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.WindowManager;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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
  private val DEFAUTL_SOURCE_ICON = Utilities.loadImage("org/aiotrade/platform/modules/netbeans/ui/resources/symbol.gif");
  /** Deserialize a Symbol from xml file */

  private def readContents(node: Node): AnalysisContents = {
    val xdo = node.getLookup.lookup(classOf[XMLDataObject])
    if (xdo == null) {
      throw new IllegalStateException("Bogus file in Symbols folder: " + node.getLookup.lookup(classOf[FileObject]));
    }
    val readFrom = xdo.getPrimaryFile
    try {
      val is = readFrom.getInputStream
      val xmlReader = XMLUtil.createXMLReader
      val handler = new ContentsParseHandler
      xmlReader.setContentHandler(handler)
      xmlReader.parse(new InputSource(is))

      return handler.getContents
    } catch {
      case ex: IOException  => ErrorManager.getDefault.notify(ex)
      case ex: SAXException => ErrorManager.getDefault.notify(ex)
    }

    null
  }

  /** Getting the Symbol node and wrapping it in a FilterNode */
  class OneSymbolNode(symbolFileNode: Node, contents: AnalysisContents, content: InstanceContent
  ) extends FilterNode(symbolFileNode, new SymbolChildren(contents), new AbstractLookup(content)) {
    NetBeansPersistenceManager.putNode(contents, this)

    /* add the node to our own lookup */
    content.add(this);

    /* add additional items to the lookup */
    content.add(contents);
    content.add(new SymbolViewAction(this))
    content.add(new SymbolReimportDataAction(this))
    content.add(new SymbolRefreshDataAction(this))
    content.add(new SymbolSetDataSourceAction(this))
    content.add(new SymbolStartWatchAction(this))
    content.add(new SymbolStopWatchAction(this))
    content.add(new SymbolCompareToAction(this))
    content.add(new SymbolClearDataAction(this))

    /** add delegate's all lookup contents */
    val result = symbolFileNode.getLookup.lookup(new Lookup.Template[Object](classOf[Object])).allInstances.iterator
    while (result.hasNext) {
      content.add(result.next)
    }

    /* As the lookup needs to be constucted before Node's constructor is called,
     * it might not be obvious how to add Node or other objects into it without
     * type casting. Here is the recommended suggestion that uses public/private
     * pair of constructors:
     */
    @throws(classOf[IOException])
    @throws(classOf[IntrospectionException])
    def this(symbolFileNode: Node, contents: AnalysisContents) = {
      this(symbolFileNode, contents, new InstanceContent);
    }


    override def getDisplayName = {
      val contents = getLookup.lookup(classOf[AnalysisContents])
      contents.uniSymbol
    }

    override def getIcon(tpe: Int): Image = {
      var icon: Image = null

      val contents = getLookup.lookup(classOf[AnalysisContents]);
      contents.lookupActiveDescriptor(classOf[QuoteContract]) foreach {quoteContract =>
        icon = quoteContract.icon.getOrElse(null)
      }

      if (icon != null) icon else DEFAUTL_SOURCE_ICON
    }

    override def getOpenedIcon(tpe: Int): Image = {
      getIcon(0);
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

    override
    protected def createNodeListener: NodeListener = {
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
          if (NetBeansPersistenceManager.occupiedContentsOf(OneSymbolNode.this) != null) {
            getLookup.lookup(classOf[SymbolClearDataAction]).perform(false)
          }

          NetBeansPersistenceManager.removeNode(nodeEvent.getNode)
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
   * The root node of SymbolNode
   *  It will be 'Symbols' folder in default file system, usually the 'config' dir in userdir
   *  Physical folder "Symbols" is defined in layer.xml
   */
  @throws(classOf[DataObjectNotFoundException])
  @throws(classOf[IntrospectionException])
  class RootSymbolNode extends SymbolNode(
    DataObject.find(Repository.getDefault.getDefaultFileSystem.getRoot.getFileObject("Symbols")).getNodeDelegate
  ) {

    override def getDisplayName = {
      NbBundle.getMessage(classOf[SymbolNode], "SN_title")
    }
  }

  /** The child of the folder node, it may be a folder or Symbol ser file */
  private class SymbolFolderChildren(symbolFolderNode: Node) extends FilterNode.Children(symbolFolderNode) {

    var oneSymbolNode: Node = _


    override protected def createNodes(key: Node): Array[Node] = {
      val node = key;

      try {
        /** is a folder? if true, creat a folder node */
        if (node.getLookup.lookup(classOf[DataFolder]) != null) {
          return Array(new SymbolNode(node))
        } /**
           * else, deserilize a contents instance from the sec xml file,
           * and create a sec node for it
           */
        else {
          var contents = readContents(node)
          if (contents != null) {
            /**
             * check if has existed in application context, if true,
             * use the existed one
             */
            NetBeansPersistenceManager.lookupContents(contents.uniSymbol) foreach {existedOne =>
              contents = existedOne
            }

            oneSymbolNode = new OneSymbolNode(node, contents)
            val fileObject = oneSymbolNode.getLookup.lookup(classOf[DataObject]).getPrimaryFile

            val newAttr = fileObject.getAttribute("new")
            if (newAttr != null && newAttr.asInstanceOf[Boolean] == true) {
              fileObject.setAttribute("new", false)

              /** open view for new added sec */
              java.awt.EventQueue.invokeLater(new Runnable {
                  def run {
                    oneSymbolNode.getLookup.lookup(classOf[ViewAction]).execute
                  }
                })
            }

            return Array(oneSymbolNode)
          } else {
            // best effort
            return Array(new FilterNode(node))
          }

        }
      } catch {
        case ioe: IOException => ErrorManager.getDefault.notify(ioe)
        case exc: IntrospectionException => ErrorManager.getDefault.notify(exc)
      }

      // Some other type of Node (gotta do something)
      Array(new FilterNode(node))
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
  private class SymbolChildren(contents: AnalysisContents) extends Children.Keys[GroupDescriptor[AnalysisDescriptor[_]]] {

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
      val groups = PersistenceManager().lookupAllRegisteredServices(classOf[GroupDescriptor[AnalysisDescriptor[_]]], "DescriptorGroups")

      bufChildrenKeys.clear
      /** each symbol should create new NodeInfo instances that belong to itself */
      for (nodeInfo <- groups) {
        bufChildrenKeys add nodeInfo.clone.asInstanceOf[GroupDescriptor[AnalysisDescriptor[_]]]
      }
      setKeys(bufChildrenKeys)
    }

    def createNodes(key: GroupDescriptor[AnalysisDescriptor[_]]): Array[Node] = {
      try {
        Array(new GroupNode(key, contents))
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

  private class SymbolViewAction(node: Node) extends ViewAction {
    putValue(Action.NAME, "View")

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolViewAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents]);
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = contents.serProvider.asInstanceOf[Sec]
      var mayNeedsReload = false
      if (sec == null) {
        sec = new Stock(contents.uniSymbol, List(quoteContract))
        contents.serProvider = sec
      } else {
        mayNeedsReload = true
      }

      var analysisTc = AnalysisChartTopComponent.lookupTopComponent(sec.uniSymbol) getOrElse {
        /**
         * !NOTICE
         * close a TopComponent doen's mean this TopComponent is null, it still
         * exsit, just invsible
         */
        /** if TopComponent of this stock has been shown before, should reload quote data */
        if (mayNeedsReload) {
          sec.clearSer(quoteContract.freq)
        }
        /** here should be the only place to new AnalysisChartTopComponent instance */
        new AnalysisChartTopComponent(sec, contents)
      }

      if (!sec.isSerLoaded(quoteContract.freq)) {
        sec.loadSer(quoteContract.freq)
      }

      if (!analysisTc.isOpened) {
        analysisTc.open
      }

      analysisTc.requestActive
    }
  }

  class SymbolStartWatchAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Start Watching")
    putValue(Action.SMALL_ICON, "org/aiotrade/platform/modules/netbeans/ui/resources/startWatch.gif");

    def execute {
      /** is this a folder ? if true, go recursively */
      if (node.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- node.getChildren.getNodes) {
          child.getLookup.lookup(classOf[SymbolStartWatchAction]).execute
        }
        return
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents]);

      var sec = contents.serProvider match {
        case null =>
          contents.lookupActiveDescriptor(classOf[QuoteContract]) map {quoteContract =>
            val sec = new Stock(contents.uniSymbol, List(quoteContract))
            contents.serProvider = sec
            sec
          } getOrElse null
        case x: Sec => x
      }

      sec.subscribeTickerServer

      val rtWatchListWin = RealTimeWatchListTopComponent.getInstance
      rtWatchListWin.requestActive
      rtWatchListWin.watch(sec, node)

      val rtChartsWin = RealTimeChartsTopComponent.getInstance
      rtChartsWin.requestActive
      rtChartsWin.watch(sec, contents)

      val rtBoardWin = RealTimeBoardTopComponent.getInstance(sec, contents)
      rtBoardWin.watch
      rtBoardWin.requestActive

      node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
      this.setEnabled(false)
    }
  }

  class SymbolStopWatchAction(node: Node) extends GeneralAction {

    putValue(Action.NAME, "Stop Watching")
    putValue(Action.SMALL_ICON, "org/aiotrade/platform/modules/netbeans/ui/resources/stopWatch.gif");
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

      val sec = contents.serProvider match {
        case null => return
        case x: Sec => x
      }

      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
        RealTimeWatchListTopComponent.instanceRefs.head.get.unWatch(sec)
      }

      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
        RealTimeChartsTopComponent.instanceRefs.head.get.unWatch(sec)
      }

      val rtBoardWin = RealTimeBoardTopComponent.findInstance(sec)
      if (rtBoardWin != null) {
        rtBoardWin.unWatch
      }

      sec.unSubscribeTickerServer

      node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
      this.setEnabled(false)
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

    private val CLEAR = "Clear data in database";
    putValue(Action.NAME, CLEAR);

    def perform(shouldConfirm: Boolean) {
      /**
       * don't get descriptors from getLookup.lookup(..), becuase
       * if node destroy is invoked by parent node, such as folder,
       * the lookup content may has been destroyed before node destroyed.
       */
      val contents = NetBeansPersistenceManager.occupiedContentsOf(node)
      if (contents != null) {
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
      perform(true);
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
        sec = new Stock(contents.uniSymbol, List(quoteContract))
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
        for (child <- node.getChildren().getNodes()) {
          child.getLookup.lookup(classOf[SymbolRefreshDataAction]).execute;
        }
        return;
      }

      /** otherwise, it's an OneSymbolNode, do real things */
      val contents = node.getLookup.lookup(classOf[AnalysisContents]);
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = contents.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        sec = new Stock(contents.uniSymbol, List(quoteContract))
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
      val contents = node.getLookup.lookup(classOf[AnalysisContents]);

      val pane = new ImportSymbolDialog(
        WindowManager.getDefault.getMainWindow,
        contents.lookupActiveDescriptor(classOf[QuoteContract]).getOrElse(null),
        false);
      if (pane.showDialog != JOptionPane.OK_OPTION) {
        return;
      }

      contents.lookupAction(classOf[SaveAction]) foreach {_.execute}
      node.getLookup.lookup(classOf[SymbolReimportDataAction]).execute;
    }
  }

  private class SymbolCompareToAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, "Compare to Current")

    def execute {
      val contents = node.getLookup.lookup(classOf[AnalysisContents])
      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get

      var sec = contents.serProvider.asInstanceOf[Sec]
      if (sec == null) {
        sec = new Stock(contents.uniSymbol, List(quoteContract))
        contents.serProvider = sec
      }

      val analysisTc = AnalysisChartTopComponent.getSelected

      if (!sec.isSerLoaded(quoteContract.freq)) {
        val loadBegins = sec.loadSer(quoteContract.freq)
      }

      if (analysisTc != null) {
        val serToBeCompared = sec.serOf(quoteContract.freq).get
        val viewContainer = analysisTc.lookupViewContainer(serToBeCompared.freq).getOrElse(null)

        if (viewContainer == null) {
          return
        }

        val baseSer = viewContainer.controller.masterSer
        val quoteCompareIndicator = new QuoteCompareIndicator(baseSer)
        quoteCompareIndicator.shortDescription = sec.uniSymbol
        quoteCompareIndicator.serToBeCompared = serToBeCompared
        quoteCompareIndicator.computeFrom(0)

        viewContainer.controller.scrollReferCursorToLeftSide
        viewContainer.masterView.asInstanceOf[AnalysisQuoteChartView].addQuoteCompareChart(quoteCompareIndicator);

        analysisTc.setSelectedViewContainer(viewContainer)
        analysisTc.requestActive
      }

    }
  }

  /** Creating an action for adding a folder to organize stocks into groups */
  private class AddFolderAction(folder: DataFolder) extends AbstractAction {
    putValue(Action.NAME, NbBundle.getMessage(classOf[SymbolNode], "SN_addfolderbutton"));

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


  @throws(classOf[IntrospectionException])
  class SymbolNode(symbolFolderNode: Node, content: InstanceContent
  ) extends FilterNode(symbolFolderNode, new SymbolNodes.SymbolFolderChildren(symbolFolderNode), new AbstractLookup(content)) {

    /* add the node to our own lookup */
    content.add(this)

    /** add delegate's all lookup contents */
    val result = symbolFolderNode.getLookup.lookup(new Lookup.Template[Object](classOf[Object])).allInstances.iterator
    while (result.hasNext) {
      content.add(result.next)
    }

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


  }
}






