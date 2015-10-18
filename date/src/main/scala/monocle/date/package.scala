package monocle

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval

package object date {

  type Millis = Int Refined Interval[W.`0`.T, W.`999`.T]
  type Second = Int Refined Interval[W.`0`.T, W.`59`.T]
  type Minute = Int Refined Interval[W.`0`.T, W.`59`.T]
  type Hour   = Int Refined Interval[W.`0`.T, W.`23`.T]

}
