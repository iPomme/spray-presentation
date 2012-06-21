import cc.spray.Directives
import cc.spray.directives._

trait CalculatorService extends Directives with CustomMarshallers {

  val calculatorService = {
    path("add" / DoubleNumber / DoubleNumber) {
      (a, b) =>
        get {
          completeWith(a + b)
        }
    } ~
      pathPrefix("substract") {
        pathPrefix(DoubleNumber) {
          a =>
            path(DoubleNumber) {
              b => // pathPrefix can be nested, but the final match needs to be done with the "path" directive
                get {
                  completeWith(a - b)
                }
            }
        }
      } ~
      path("multiply" / DoubleNumber / DoubleNumber) {
        (a, b) =>
          detach {
            // just for fun: everything following the 'detach' directive will be running in a newly spawned actor
            get {
              completeWith(a * b)
            }
          }
      } ~
      path("divide" / DoubleNumber / DoubleNumber) {
        (a, b) =>
          parameter('onDivZero ? "Cannot divide by zero") {
            onDivZero =>
              get {
                ctx =>
                  if (b == 0.0) {
                    ctx.complete(onDivZero) // we allow for an optional "onDivZero" parameter to supply the error result
                  } else {
                    ctx.complete(a / b)
                  }
              }
          }
      }
  }

}