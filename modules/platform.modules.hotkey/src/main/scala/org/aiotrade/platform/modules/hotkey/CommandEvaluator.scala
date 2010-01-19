/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.aiotrade.platform.modules.hotkey;

import java.util.regex.Pattern;
import org.aiotrade.platform.modules.hotkey.ProviderModel.Category;
import org.aiotrade.platform.spi.hotkey.SearchProvider;
import org.aiotrade.platform.spi.hotkey.SearchRequest;
import org.aiotrade.platform.spi.hotkey.SearchResponse;
import org.openide.util.RequestProcessor;
import scala.collection.mutable.ArrayBuffer

/**
 * Command Evaluator. It evaluates commands from toolbar and creates results.
 * 
 * @author Jan Becicka, Dafe Simonek
 */
object CommandEvaluator {
    
  val RECENT = "Recent"
    
  /**
   * command pattern is:
   * "command arguments"
   */
  val COMMAND_PATTERN = Pattern.compile("(\\w+)(\\s+)(.+)")

  /** Narrow evaluation only to specified category if non null.
   * Evaluate all categories otherwise
   */
  var evalCat: ProviderModel.Category = _

  /** Temporary narrow evaluation to only specified category **/
  var isCatTemporary = false
    
  /**
   * Runs evaluation.
   *
   * @param command text to evauate, to search for
   *
   * @return task of this evaluation, which waits for all providers to complete
   * execution. Use returned instance to recognize if this evaluation still
   * runs and when it actually will finish.
   */
  def evaluate (command: String, model: ResultsModel): org.openide.util.Task = {
    val l = ArrayBuffer[CategoryResult]()
    val commands = parseCommand(command)
    val sRequest = Accessor.DEFAULT.createRequest(commands(1), null)
    val tasks = ArrayBuffer[RequestProcessor#Task]()

    val provCats = ArrayBuffer[Category]()
    val allResults = getProviderCategories(commands, provCats)

    for (curCat <- provCats) {
      val catResult = new CategoryResult(curCat, allResults)
      val sResponse = Accessor.DEFAULT.createResponse(catResult, sRequest)
      for (provider <- curCat.providers) {
        val t = runEvaluation(provider, sRequest, sResponse, curCat)
        if (t != null) {
          tasks += t
        }
      }
      l += catResult
    }

    model.content = l.toList

    new Wait4AllTask(tasks.toList)
  }

  private def parseCommand(command: String): Array[String] = {
    val results = new Array[String](2)

    val m = COMMAND_PATTERN.matcher(command)

    if (m.matches) {
      results(0) = m.group(1)
      if (ProviderModel.instance.isKnownCommand(results(0))) {
        results(1) = m.group(3)
      } else {
        results(0) = null
        results(1) = command
      }
    } else {
      results(1) = command
    }
                
    results
  }

  /** Returns array of providers to ask for evaluation according to
   * current evaluation rules.
   *
   * @return true if providers are expected to return all results, false otherwise
   */
  private def getProviderCategories(commands: Array[String], result: ArrayBuffer[Category]): Boolean = {
    val cats = ProviderModel.instance.categories

    // always include recent searches
    for (cat <- cats if RECENT.equals(cat.name)) {
      result += cat
    }

    // skip all but recent if empty string came
    if (commands(1) == null || commands(1).trim == "") {
      return false
    }

    // command string has biggest priority for narrow evaluation to category
    if (commands(0) != null) {
      for (curCat <- cats) {
        val commandPrefix = curCat.commandPrefix
        if (commandPrefix != null && commandPrefix.equalsIgnoreCase(commands(0))) {
          result += curCat
          return true
        }
      }
    }

    // evaluation narrowed to category perhaps?
    if (evalCat != null) {
      result += evalCat
      return true
    }

    // no narrowing
    result.clear
    result ++= cats

    false
  }

  private def runEvaluation(provider: SearchProvider, request: SearchRequest,
                            response: SearchResponse, cat: ProviderModel.Category): RequestProcessor#Task = {
    // actions are not happy outside EQ at all
    if (cat.name == "Actions") {
      provider.evaluate(request, response)
      return null
    }
        
    RequestProcessor.getDefault.post(new Runnable {
        def run {
          provider.evaluate(request, response)
        }
      })
  }

  /** Task implementation that computes nothing itself, it just waits
   * for all given RequestProcessor tasks to finish and then it finishes as well.
   */
  private class Wait4AllTask(tasks: List[RequestProcessor#Task]) extends org.openide.util.Task() with Runnable {

    private val TIMEOUT = 60000L

    override def run {
      try {
        notifyRunning
        for (task <- tasks) {
          try {
            // wait no longer then one minute
            task.waitFinished(TIMEOUT)
          } catch {case ex: InterruptedException =>}
        }
      } finally {
        notifyFinished
      }
    }
  }

}
