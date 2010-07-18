package org.aiotrade.lib.util.config

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.Properties
import java.util.ResourceBundle
import net.lag.configgy.Configgy
import net.lag.configgy.ParseException
import net.lag.logging.Logger

class ConfigurationException(message: String) extends RuntimeException(message)

/**
 * Loads up the configuration (from the app.conf file).
 */
object Config {
  val logger = Logger.get(this.getClass.getName)

  val app = "aiotrade"
  val version = "0.10"

  lazy val configDir: Option[String] = List("./conf", "./etc") find {x =>
    val f = new File(x)
    f.exists && f.isDirectory
  }

  val config = {
    val classLoader = Thread.currentThread.getContextClassLoader
    
    if (System.getProperty(app + ".config", "") != "") {
      val configFile = System.getProperty(app + ".config", "")
      try {
        Configgy.configure(configFile)
        logger.info("Config loaded from -D" + app + ".config=%s", configFile)
      } catch {
        case e: ParseException => throw new ConfigurationException(
            "Config could not be loaded from -D" + app + ".config=" + configFile +
            "\n\tdue to: " + e.toString)
      }
      Configgy.config
    } else if (configDir.isDefined) {
      try {
        val configFile = configDir.get + "/" + app + ".conf"
        Configgy.configure(configFile)
        logger.info("configDir is defined as [%s], config loaded from [%s].", configDir.get, configFile)
      } catch {
        case e: ParseException => throw new ConfigurationException(
            "configDir is defined as [" + configDir.get + "] " +
            "\n\tbut the '" + app + ".conf' config file can not be found at [" + configDir.get + "/" + app + ".conf]," +
            "\n\tdue to: " + e.toString)
      }
      Configgy.config
    } else if (classLoader.getResource(app + ".conf") != null) {
      try {
        Configgy.configureFromResource(app + ".conf", classLoader)
        logger.info("Config loaded from the application classpath.")
      } catch {
        case e: ParseException => throw new ConfigurationException(
            "Can't load '" + app + ".conf' config file from application classpath," +
            "\n\tdue to: " + e.toString)
      }
      Configgy.config
    } else {
      logger.warning(
        "\nCan't load '" + app + ".conf'." +
        "\nOne of the three ways of locating the '" + app + ".conf' file needs to be defined:" +
        "\n\t1. Define the '-D" + app + ".config=...' system property option." +
        "\n\t2. Define './conf' directory." +
        "\n\t3. Put the '" + app + ".conf' file on the classpath." +
        "\nI have no way of finding the '" + app + ".conf' configuration file." +
        "\nUsing default values everywhere.")
      net.lag.configgy.Config.fromString("<" + app + "></" + app + ">") // default empty config
    }
  }

  val configVersion = config.getString(app + ".version", version)
  if (version != configVersion) throw new ConfigurationException(
    app + " version [" + version + "] is different than the provided config ('" + app + ".conf') version [" + configVersion + "]")

  val startTime = System.currentTimeMillis
  def uptime = (System.currentTimeMillis - startTime) / 1000


  // --- todo for properties
  def loadProperties(fileName: String) {
    val props = new Properties
    val file = new File(fileName)
    if (file.exists) {
      try {
        val is = new FileInputStream(file)
        if (is != null) props.load(is)
        is.close
      } catch {case _ =>}
    }
  }

  private val SUFFIX = ".properties"
  def loadProperties($name: String, LOAD_AS_RESOURCE_BUNDLE: Boolean = false): Properties = {
    var name = $name
    if ($name.startsWith("/"))  name = $name.substring(1)
    
    if ($name.endsWith(SUFFIX)) name = $name.substring(0, $name.length - SUFFIX.length)
    
    val props = new Properties

    val loader = classLoader
    var in: InputStream = null
    try {
      if (LOAD_AS_RESOURCE_BUNDLE) {
        name = name.replace('/', '.')
        val rb = ResourceBundle.getBundle(name, Locale.getDefault, loader)
        val keys = rb.getKeys
        while (keys.hasMoreElements) {
          props.put(keys.nextElement.asInstanceOf[String], rb.getString(keys.nextElement.asInstanceOf[String]))
        }
      } else {
        name = name.replace('.', '/')
        if (!name.endsWith(SUFFIX)) name = name.concat(SUFFIX)
        in = loader.getResourceAsStream(name)
        if (in != null) {
          props.load(in) // can throw IOException
        }
      }
      in.close
    } catch {case _ =>}
    props
  }

  // ### Classloading

  def classLoader: ClassLoader = config.getString(app + ".classLoader") match {
    //case Some(cld: ClassLoader) => cld
    case _ => Thread.currentThread.getContextClassLoader
  }
  def loadClass[C](name: String): Class[C] =
    Class.forName(name, true, classLoader).asInstanceOf[Class[C]]
  def newObject[C](name: String, default: => C): C = config.getString(name) match {
    case Some(s: String) => loadClass[C](s).newInstance
    case _ => default
  }
}
