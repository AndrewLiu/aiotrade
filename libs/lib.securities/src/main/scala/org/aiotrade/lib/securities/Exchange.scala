package org.aiotrade.lib.securities

import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import scala.collection.immutable.TreeMap
import scala.swing.Publisher
import scala.swing.event.Event

/**
 *
 * @author dcaoyuan
 */
object Exchange extends Publisher {
  case class ExchangeOpened(exchange: Exchange) extends Event
  case class ExchangeClosed(exchange: Exchange) extends Event

  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.securities.Bundle")
  private val ONE_DAY = 24 * 60 * 60 * 1000

  val N   = new Exchange("N",  TimeZone.getTimeZone("America/New_York"), 9, 30, 16, 00)  // New York
  val SS  = new Exchange("SS", TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shanghai
  val SZ  = new Exchange("SZ", TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shenzhen
  val L   = new Exchange("L",  TimeZone.getTimeZone("UTC"), 9, 30, 15, 0) // London

  val allExchanges = List(N, SS, SZ, L)

  val exchangeCodes = Map(
    "CI"  -> "Abidjan Stock Exchange",
    "E"   -> "AEX Options and Futures Exchange",
    "AS"  -> "AEX Stock Exchange",
    "AL"  -> "Alpha Trading Systems",
    "A"   -> "American Stock Exchange",
    "AM"  -> "Amman Stock Exchange",
    "AX"  -> "Australian Stock Exchange",
    "BH"  -> "Bahrain Stock Exchange",
    "MC"  -> "Barcelona Stock Exchange - CATS Feed",
    "BC"  -> "Barcelona Stock Exchange - Floor Trading",
    "BY"  -> "Beirut Stock Exchange",
    "b"   -> "Belfox",
    "BE"  -> "Berlin Stock Exchange",
    "BN"  -> "Berne Stock Exchange",
    "BI"  -> "Bilbao Stock Exchange",
    "BBK" -> "BlockBook ATS",
    "BO"  -> "Bombay Stock Exchange",
    "B"   -> "Boston Stock Exchange",
    "BT"  -> "Botswana Share Market",
    "BM"  -> "Bremen Stock Exchange",
    "BR"  -> "Brussels Stock Exchange",
    "CA"  -> "Cairo and Alexandria Stock Exchange",
    "CL"  -> "Calcutta Stock Exchange",
    "V"   -> "Canadian Ventures Exchange",
    "CH"  -> "Channel Islands",
    "W"   -> "Chicago Board Options Exchange",
    "MW"  -> "Chicago Stock Exchange",
    "CE"  -> "Chile Electronic Exchange",
    "INS" -> "CHI-X Exchange",
    "C"   -> "Cincinnati Stock Exchange",
    "CM"  -> "Colombo Stock Exchange",
    "CO"  -> "Copenhagen Stock Exchange",
    "DL"  -> "Dehli Stock Exchange",
    "QA"  -> "Doha Securities Market",
    "DU"  -> "Dubai Financial Market",
    "DI"  -> "Dubai International Financial Exchange",
    "D"   -> "Dusseldorf Stock Exchange",
    "EB"  -> "Electronic Stock Exchange  of Venezuela",
    "F"   -> "Frankfurt Stock Exchange",
    "FU"  -> "Fukuoka Stock Exchange",
    "GH"  -> "Ghana Stock Exchange",
    "H"   -> "Hamburg Stock Exchange",
    "HA"  -> "Hanover Stock Exchange",
    "HE"  -> "Helsinki Stock Exchange",
    "HK"  -> "Hong Kong Stock Exchange",
    "IC"  -> "Iceland Stock Exchange",
    "IN"  -> "Interbolsa (Portugal)",
    "Y"   -> "International Securities Exchange (ISE)",
    "I"   -> "Irish Stock Exchange",
    "IS"  -> "Istanbul Stock Exchange",
    "JK"  -> "Jakarta Stock Exchange",
    "Q"   -> "Japanese Securities Dealers Association (JASDAQ)",
    "J"   -> "Johannesburg Stock Exchange",
    "KAB" -> "Kabu.com PTS",
    "KA"  -> "Karachi Stock Exchange",
    "KZ"  -> "Kazakhstan Stock Exchange",
    "KFE" -> "Korean Futures Exchange",
    "KS"  -> "Korea Stock Exchange",
    "KQ"  -> "KOSDAQ (Korea)",
    "KL"  -> "Kuala Lumpur Stock Exchange",
    "KW"  -> "Kuwait Stock Exchange",
    "KY"  -> "Kyoto Stock Exchange",
    "LG"  -> "Lagos Stock Exchange",
    "LA"  -> "Latin American Market in Spain (LATIBEX)",
    "LN"  -> "Le Nouveau Marche",
    "LM"  -> "Lima Stock Exchange",
    "LS"  -> "Lisbon Stock Exchange (Portugal)",
    "L"   -> "London Stock Exchange",
    "LZ"  -> "Lusaka Stock Exchange",
    "LU"  -> "Luxembourg Stock Exchange",
    "MD"  -> "Madras Stock Exchange",
    "MA"  -> "Madrid Stock Exchange - Floor Trading",
    "MT"  -> "Malta Stock Exchange",
    "MZ"  -> "Mauritius Stock Exchange",
    "ML"  -> "Medellin Stock Excahnge",
    "MX"  -> "Mexican Stock Exchange",
    "MI"  -> "Milan Stock Exchange",
    "p"   -> "MONEP Paris Stock Options",
    "M"   -> "Montreal Exchange",
    "MM"  -> "Moscow Inter Bank Currency Exchange",
    "MO"  -> "Moscow Stock Exchange",
    "MU"  -> "Munich Stock Exchange",
    "OM"  -> "Muscat Stock Exchange",
    "NG"  -> "Nagoya Stock Exchange",
    "NR"  -> "Nairobi Stock Exchange",
    "NM"  -> "Namibia Stock Exchange",
    "OQ"  -> "NASDAQ",
    "OB"  -> "NASDAQ Dealers - Bulletin Board",
    "OJ"  -> "NASDAQ Japan",
    "NS"  -> "National Stock Exchange of India",
    "NW"  -> "NewEx (Austria)",
    "N"   -> "New York Stock Exchange",
    "NZ"  -> "New Zealand Stock Exchange",
    "MP"  -> "NYSE MatchPoint",
    "OD"  -> "Occidente Stock Exchange",
    "OS"  -> "Osaka Stock Exchange",
    "OL"  -> "Oslo Stock Exchange",
    "P"   -> "Pacific Stock Exchange",
    "PA"  -> "Paris Stock Exchange",
    "PH"  -> "Philadelphia Stock Exchange",
    "X"   -> "Philadelphia Stock Exchange Options",
    "PS"  -> "Phillipine Stock Exchange",
    "PNK" -> "Pink Sheets (National Quotation Bureau)",
    "PR"  -> "Prague Stock Exchange",
    "PT"  -> "Pure Trading",
    "RQ"  -> "RASDAQ (Romania)",
    "RI"  -> "Riga Stock Exchange",
    "SO"  -> "Rio de Janeiro OTC Stock Exchange (SOMA)",
    "RTS" -> "Russian Trading System",
    "SN"  -> "Santiago Stock Exchange",
    "SA"  -> "Sao Paulo Stock Exchange",
    "SP"  -> "Sapporo Stock Exchange",
    "SE"  -> "Saudi Stock Exchange",
    "JNX" -> "SBI Japannext",
    "SBI" -> "SBI Stock Exchange (Sweden)",
    "SS"  -> "Shanghai Stock Exchange",
    "SZ"  -> "Shenzhen Stock Exchange",
    "SIM" -> "Singapore Exchange - Derivatives",
    "SI"  -> "Singapore Stock Exchange",
    "ST"  -> "Stockholm Stock Exchange",
    "PE"  -> "St. Petersburg Stock Exchange",
    "SG"  -> "Stuttgart Stock Exchange",
    "SU"  -> "Surabaya Stock Exchange",
    "QMH" -> "SWX Quotematch AG",
    "S"   -> "SWX Swiss Exchange",
    "SFE" -> "Sydney Futures Exchange",
    "TWO" -> "Taiwan OTC Securities Exchange",
    "TW"  -> "Taiwan Stock Exchange",
    "TL"  -> "Tallinn Stock Exchange",
    "TA"  -> "Tel Aviv Stock Exchange",
    "BK"  -> "Thailand Stock Exchange",
    "TH"  -> "Third Market",
    "TCE" -> "Tokyo Commodity Exchange",
    "TFF" -> "Tokyo Financial Futures Exchange",
    "T"   -> "Tokyo Stock Exchange",
    "K"   -> "Toronto Options Exchange",
    "TO"  -> "Toronto Stock Exchange",
    "TP"  -> "Tradepoint Stock Exchange",
    "TN"  -> "Tunis Stock Exchange",
    "TQ"  -> "Turquoise",
    "PFT" -> "Ukraine PFTS",
    "VA"  -> "Valencia Stock Exchange",
    "VI"  -> "Vienna Stock Exchange",
    "VL"  -> "Vilnus Stock Exchange",
    "VX"  -> "virt-x",
    "DE"  -> "Xetra",
    "ZA"  -> "Zagreb Stock Exchange",
    "ZI"  -> "Zimbabwe Stock Exchange"
  )

  def symbolsOf(exchange: Exchange): List[String] = {
    exchange.code match {
      case "N" =>
        List("GOOG", "ORCL", "YHOO")
      case "SS" =>
        SSSymbols
      case "SZ" =>
        List("000001.SZ", "000002.SZ", "000003.SZ", "000004.SZ")
      case "L" =>
        List("BP.L", "VOD.L", "BT-A.L", "BARC.L", "BAY.L", "TSCO.L", "HSBA.L")
      case _ => Nil
    }
  }

  lazy val SSSymbols = SSSymToName.keySet map (_ + ".SS") toList

  lazy val SSSymToName = TreeMap(
    "600000" -> "浦发银行",
    "600001" -> "邯郸钢铁",
    "600002" -> "齐鲁石化",
    "600003" -> "东北高速",
    "600004" -> "白云机场",
    "600005" -> "武钢股份",
    "600006" -> "东风汽车",
    "600007" -> "中国国贸",
    "600008" -> "首创股份",
    "600009" -> "上海机场",
    "600010" -> "钢联股份",
    "600011" -> "华能国际",
    "600012" -> "皖通高速",
    "600015" -> "华夏银行",
    "600016" -> "民生银行",
    "600018" -> "上港集箱",
    "600019" -> "宝钢股份",
    "600020" -> "中原高速",
    "600021" -> "上海电力",
    "600026" -> "中海发展",
    "600028" -> "中国石化",
    "600029" -> "南方航空",
    "600030" -> "中信证券",
    "600031" -> "三一重工",
    "600033" -> "福建高速",
    "600036" -> "招商银行",
    "600037" -> "歌华有线",
    "600038" -> "哈飞股份",
    "600039" -> "四川路桥",
    "600050" -> "中国联通",
    "600051" -> "宁波联合",
    "600052" -> "浙江广厦",
    "600053" -> "*ST江纸",
    "600054" -> "黄山旅游",
    "600055" -> "万东医疗",
    "600056" -> "中技贸易",
    "600057" -> "夏新电子",
    "600058" -> "五矿发展",
    "600059" -> "古越龙山",
    "600060" -> "海信电器",
    "600061" -> "中纺投资",
    "600062" -> "双鹤药业",
    "600063" -> "皖维高新",
    "600064" -> "南京高科",
    "600065" -> "大庆联谊",
    "600066" -> "宇通客车",
    "600067" -> "冠城大通",
    "600068" -> "葛洲坝",
    "600069" -> "银鸽投资",
    "600070" -> "浙江富润",
    "600071" -> "凤凰光学",
    "600072" -> "江南重工",
    "600073" -> "上海梅林"
  )

  private val timer = new Timer
  for (exchange <- allExchanges) {
    val preOpen = exchange.open
    preOpen.add(Calendar.MINUTE, -15)
    timer.schedule(new TimerTask {
        def run {
          // @todo process vacation here
          publish(ExchangeOpened(exchange))
        }
      }, preOpen.getTime, ONE_DAY)

    val postClose = exchange.close
    postClose.add(Calendar.MINUTE, +15)
    timer.schedule(new TimerTask {
        def run {
          // @todo process vacation here
          publish(ExchangeClosed(exchange))
        }
      }, postClose.getTime, ONE_DAY)
  }

}

import Exchange._
class Exchange(val code: String, val timeZone: TimeZone, openHour: Int, openMin: Int, closeHour: Int, closeMin: Int) {

  val longDescription:  String = BUNDLE.getString(code + "_Long")
  val shortDescription: String = BUNDLE.getString(code + "_Short")

  val openTimeOfDay: Long = (openHour * 60 + openMin) * 60 * 1000

  private var _symbols = List[String]()

  def this(openHour: Int, openMin: Int, closeHour: Int, closeMin: Int) {
    this("NYSE", TimeZone.getTimeZone("UTC"), openHour, openMin, closeHour, closeMin)
  }

  def open: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal
  }

  def close: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal
  }


  def openTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal.getTimeInMillis
  }

  def closeTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal.getTimeInMillis
  }

  def symbols = _symbols
  def symbols_=(symbols: List[String]) {
    _symbols = symbols
  }

  override def toString: String = {
    code + " -> " + timeZone.getDisplayName
  }
}
