package utils

import java.text.SimpleDateFormat
import java.util.Date

import controllers.{NameSeries, NameValue}
import org.apache.commons.lang3.time.DateUtils

object TimeChartUtils {
  val f = new SimpleDateFormat("MM-yyyy")
  def generateSeries(times :List[Long], name : String ) : NameSeries = {
    val ref = times.groupBy(x=>timeToStr(x))
    import java.util.Calendar
    val cal = Calendar.getInstance
    val lastYearMonths = (0 to 11).map(i=> DateUtils.addMonths(cal.getTime,i * -1)).reverse
    val monthWithCount =  lastYearMonths.map(t=>NameValue(f.format(t),ref.getOrElse(f.format(t),List.empty[String]).size ))
    NameSeries(name,monthWithCount.toList)
  }

  def generateQualitySeries(times :List[Int], name : String ) : NameSeries = {
    val ref = times.groupBy(x=>getQualityName(x))
    val monthWithCount =  ref.map(t=>NameValue(t._1,t._2.size))
    NameSeries(name,monthWithCount.toList)
  }

  def getQualityName(rate : Integer): String = {
    if (rate>=5) {
      "Very Good"
    }else if (rate ==4) {
      "Good"
    }else if (rate ==3) {
      "Satisfactory"
    }else if (rate ==2) {
      "Pass"
    }else  {
      "Critical"
    }
  }
  def timeToStr(t : Long) : String= {
    val d = new Date(t)
    f.format(d)
  }
}
