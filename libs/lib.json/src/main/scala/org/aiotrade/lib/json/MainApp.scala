package org.aiotrade.lib.json

import java.io.ByteArrayInputStream
import scala.collection.mutable.HashMap

object MainApp {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]) {
    testDecode
    testVmap
    testTuple2
    testDecode2
  }
  
  def testDecode {
    var text = """{"L":{"s":"600001.SS","t":1265737049852,"v":[202.04144,61.16,51.15,31.13,41.14,740.1218,660.10156,21.12]}}"""
    var json = JsonBuilder.readJson(text)
    println(json)

    json = JsonBuilder.readJson("""{"key1": "val1"}""")
    println(json)

    json = JsonBuilder.readJson("""{"key\"1" : "val1"}""")
    println(json)

    json = JsonBuilder.readJson("""{"key\"1" : ["val1", "val2"]}""")
    println(json)

    json = JsonBuilder.readJson("""["val1", "val2"]""")
    println(json)
        
    json = JsonBuilder.readJson("""[{"key1" : "val1"}, 123, -123.1, "ab\"c"]""")
    println(json)

    text = """
  {"businesses": [{"address1": "650 Mission Street",
                   "address2": "",
                   "avg_rating": 4.5,
                   "categories": [{"category_filter": "localflavor",
                                   "name": "Local Flavor",
                                   "search_url": "http://lightpole.net/search"}],
                   "city": "San Francisco, 旧金山",
                   "distance": 0.085253790020942688,
                   "id": "4kMBvIEWPxWkWKFN__8SxQ",
                   "latitude": 37.787185668945298,
                   "longitude": -122.40093994140599},
                  {"address1": "25 Maiden Lane",
                   "address2": "",
                   "avg_rating": 5.0,
                   "categories": [{"category_filter": "localflavor",
                                   "name": "Local Flavor",
                                   "search_url": "http://lightpole.net/search"}],
                   "city": "San Francisco",
                   "distance": 0.23186808824539185,
                   "id": "O1zPF_b7RyEY_NNsizX7Yw",
                   "latitude": 37.788387,
                   "longitude": -122.40401}]} \n\"""
    json = JsonBuilder.readJson(text)
    println(json)
  }

  def testDecode2 = {
    val text = """
     {"tickers":
        {"399964.SZ":{"1307416271000":[[5863.884,5902.327,5846.854,5906.357,5841.778,4.31656E8,7.372820494E9,38.443],[0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]]},
         "000011.SZ":{"1307416271000":[[10.95,11.21,10.9,11.27,10.8,1.1919046E7,1.3157490134E8,0.26],[11.21,19500.0,11.19,5800.0,11.22,2100.0,11.18,18121.0,11.23,16001.0,11.17,20451.0,11.24,54999.0,11.16,41800.0,11.25,103380.0,11.15,3100.0]]},
         "399305.SZ":{"1307416271000":[[5523.069,5533.325,5508.232,5535.06,5507.268,2.3090463E7,2.238610591E7,10.256],[0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]]}
        }
    }
"""
    val panelMf = """
      {"panelMoneyflows1mSnapshot":
        [{"008003.750918":
           {"TVi":[31900.0],"LVe":[0.0],"TAe":[227698.0],"sAe":[11800.0],"LA":[0.0],"SVi":[0.0],"LVo":[0.0],"TA":[271096.0],"LAo":[0.0],"LV":[0.0],"sV":[22200.0],"sVi":[31900.0],"LAi":[0.0],"SAe":[0.0],"SAo":[0.0],"SV":[0.0],"TV":[22200.0],"sVo":[9700.0],"SVo":[0.0],"TAi":[427265.0],"sA":[22200.0],"LVi":[0.0],"sAi":[31900.0],"LAe":[0.0],"TAo":[156169.0],"SA":[0.0],".":[1307424360000],"sAo":[9700.0],"TVe":[11800.0],"SAi":[0.0],"SVe":[0.0],"TVo":[156169.0],"sVe":[11800.0]}},
         {"008017.801860":
           {"TVi":[100.0],"LVe":[0.0],"TAe":[1580.0],"sAe":[100.0],"LA":[0.0],"SVi":[0.0],"LVo":[0.0],"TA":[1943.0],"LAo":[0.0],"LV":[0.0],"sV":[100.0],"sVi":[100.0],"LAi":[0.0],"SAe":[0.0],"SAo":[0.0],"SV":[0.0],"TV":[100.0],"sVo":[0.0],"SVo":[0.0],"TAi":[1943.0],"sA":[100.0],"LVi":[0.0],"sAi":[100.0],"LAe":[0.0],"TAo":[0.0],"SA":[0.0],".":[1307424360000],"sAo":[0.0],"TVe":[100.0],"SAi":[0.0],"SVe":[0.0],"TVo":[0.0],"sVe":[100.0]}}
        ]
      }
"""
    println("\n========= tickers decode ==============")
    val jin = new JsonInputStreamReader(new ByteArrayInputStream(text.getBytes), "utf-8")
    println(jin.readObject)

    println("\n========= panelMf decode ==============")
    val jin2 = new JsonInputStreamReader(new ByteArrayInputStream(panelMf.getBytes), "utf-8")
    println(jin2.readObject)
  }
  
  def testVmap {
    val vmap = new HashMap[String, Array[_]]
    vmap += ("." -> Array(1, 2, 3, 4))
    vmap += ("a" -> Array(1.1, 2.1, 3.1, 4.1))
    vmap += ("b" -> Array(Array("up", 10.0, "White"), Array("dn", 11.0, "White"), Array("up", 12.0, "White"), Array("up", 13.0, "White")))
    
    val bytes = Json.encode(vmap)
    val json = new String(bytes)
    println("\n========= vmap encode ==============")
    println(json)

    println("\n========= vmap decode ==============")
    val jsonObj = Json.decode(json)
    println(jsonObj)

  }

  def testTuple2 {
    val vmap = new HashMap[String, Array[_]]
    vmap += ("." -> Array(1, 2, 3, 4))
    vmap += ("a" -> Array(1.1, 2.1, 3.1, 4.1))
    vmap += ("b" -> Array(Array("up", 10.0, "White"), Array("dn", 11.0, "White"), Array("up", 12.0, "White"), Array("up", 13.0, "White")))

    val tuple = (1307598546000L, vmap)
    
    val bytes = Json.encode(tuple)
    val json = new String(bytes)
    println("\n========= tuple encode ==============")
    println(json)

    println("\n========= tuple decode ==============")
    val jsonObj = Json.decode(json)
    println(jsonObj)
  }
}
