package util

import org.apache.logging.log4j.Logger

import scala.concurrent.{ExecutionContext, Future}

object Extensions {
  implicit class FutureOps[A](f : Future[A]) {
    def logOnFailure(implicit log: Logger, ec: ExecutionContext): Future[A] = f.recoverWith{
      case error => log.error(s"Future failed with error: $error")
        f
    }
  }
}
