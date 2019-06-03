/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2005, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.data.dao.impl

import org.beangle.commons.event.Event
import org.beangle.commons.event.EventMulticaster
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.beangle.data.dao.EntityDao
import org.beangle.commons.logging.Logging

/**
 * Abstract BaseServiceImpl class.
 *
 * @author chaostone
 */
abstract class BaseServiceImpl extends Logging {

  var entityDao: EntityDao = null

  var eventMulticaster: EventMulticaster = null

  def publish(e: Event): Unit = {
    if (null != eventMulticaster) eventMulticaster.multicast(e);
  }

}