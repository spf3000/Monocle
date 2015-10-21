package monocle.date

import eu.timepit.refined.api.Refined
import monocle.Lens
import org.joda.time.DateTime

object joda {
  val millis = Lens[DateTime, Millis](d => Refined.unsafeApply(d.getMillisOfSecond))(h => d => d.withMillisOfSecond(h.get))
  val second = Lens[DateTime, Second](d => Refined.unsafeApply(d.getSecondOfMinute))(h => d => d.withSecondOfMinute(h.get))
  val minute = Lens[DateTime, Minute](d => Refined.unsafeApply(d.getMinuteOfHour))(m => d => d.withMinuteOfHour(m.get))
  val hour   = Lens[DateTime, Hour](d => Refined.unsafeApply(d.getHourOfDay))(h => d => d.withHourOfDay(h.get))
  val day    = Lens[DateTime, Day](d => Refined.unsafeApply(d.getDayOfMonth))(day => d => d.withDayOfMonth(day.get))
  val month  = Lens[DateTime, Month](d => Refined.unsafeApply(d.getMonthOfYear))(m => d => d.withMonthOfYear(m.get))
  val year   = Lens[DateTime, Year](d => Refined.unsafeApply(d.getYear))(y => d => d.withYear(y.get))
}