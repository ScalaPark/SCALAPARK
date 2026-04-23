package scalapark.api

import com.google.inject.AbstractModule
import scalapark.api.services.DashboardKafkaBridge

class Module extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[DashboardKafkaBridge]).asEagerSingleton()
