package monocle.date

import eu.timepit.refined.api.Refined
import monocle.Lens
import org.joda.time.DateTime

object joda {

  val millis = Lens[DateTime, Second](d => Refined.unsafeApply(d.getMillisOfSecond))(h => d => d.withMillisOfSecond(h.get))
  val second = Lens[DateTime, Second](d => Refined.unsafeApply(d.getSecondOfMinute))(h => d => d.withSecondOfMinute(h.get))
  val minute = Lens[DateTime, Minute](d => Refined.unsafeApply(d.getMinuteOfHour))(m => d => d.withMinuteOfHour(m.get))
  val hour   = Lens[DateTime, Hour](d => Refined.unsafeApply(d.getHourOfDay))(h => d => d.withHourOfDay(h.get))

}