/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.data

import org.genivi.sota.data.Namespace._
import org.genivi.sota.datatype.FirmwareCommon


case class Firmware(
  namespace: Namespace,
  module: Firmware.Module,
  firmwareId: Firmware.FirmwareId,
  lastModified: Long
)

object Firmware extends FirmwareCommon