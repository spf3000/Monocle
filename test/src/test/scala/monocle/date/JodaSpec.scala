package monocle.date

import monocle.MonocleSuite
import monocle.law.discipline.LensTests
import eu.timepit.refined.scalacheck.numericArbitrary._

class JodaSpec extends MonocleSuite {
  checkAll("millis", LensTests(joda.millis))
  checkAll("second", LensTests(joda.second))
  checkAll("minute", LensTests(joda.minute))
  checkAll("hour"  , LensTests(joda.hour))
  checkAll("month" , LensTests(joda.month))
  checkAll("year"  , LensTests(joda.year))
}
