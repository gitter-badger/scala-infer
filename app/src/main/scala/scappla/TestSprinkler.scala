package scappla

import scappla.Functions._
import scappla.distributions.Bernoulli
import scappla.optimization.SGD

object TestSprinkler extends App {

  import Real._

  val sgd = new SGD()
  val inRain = Bernoulli(sigmoid(sgd.param(0.0, 10.0)))
  val noRain = Bernoulli(sigmoid(sgd.param(0.0, 10.0)))

  val rainPost = Bernoulli(sigmoid(sgd.param(0.0, 10.0)))

  val model = infer {

    val sprinkle = (arg: Boolean) => {
      if (arg) {
        sample(Bernoulli(0.01), inRain)
      } else {
        sample(Bernoulli(0.4), noRain)
      }
    }

    val rain = sample(Bernoulli(0.2), rainPost)
    val sprinkled = sprinkle(rain)

    val p_wet = (rain, sprinkled) match {
      case (true, true) => 0.99
      case (false, true) => 0.9
      case (true, false) => 0.8
      case (false, false) => 0.001
    }

    // bind model to data / add observation
    observe(Bernoulli(p_wet), true)

    // return quantity we're interested in
    rain
  }


  val N = 10000
  // burn in
  for {_ <- 0 to N} {
    sample(model)
  }

  // measure
  val n_rain = Range(0, N).map { _ =>
    sample(model)
  }.count(identity)

  println(s"Expected number of rainy days: ${n_rain / 10000.0}")

  // See Wikipedia
  // P(rain = true | grass is wet) = 35.77 %
  val p_expected = 0.3577
  val n_expected = p_expected * N
  assert(math.abs(N * p_expected - n_rain) < 3 * math.sqrt(n_expected))

}