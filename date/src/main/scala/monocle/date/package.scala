package monocle

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval

package object date {
  type Millis = Int Refined Interval[W.`0`.T, W.`999`.T]
  type Second = Int Refined Interval[W.`0`.T, W.`59`.T]
  type Minute = Int Refined Interval[W.`0`.T, W.`59`.T]
  type Hour   = Int Refined Interval[W.`0`.T, W.`23`.T]
  type Day    = Int Refined Interval[W.`1`.T, W.`31`.T] // [29, 31] is not always correct
  type Month  = Int Refined Interval[W.`1`.T, W.`12`.T]
  type Year   = Int Refined Interval[W.`-292275054`.T, W.`292278993`.T] // might be joda specific
}
