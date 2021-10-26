/*
 * Copyright (C) 2005, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.edu.grade.app.model

import java.time.Instant

import org.beangle.commons.collection.Collections
import org.beangle.data.model.LongId
import org.openurp.code.edu.model.GradeType
import org.openurp.base.edu.model.Project
import org.openurp.base.edu.model.Semester

/**
 * 成绩录入开关
 */
@SerialVersionUID(6765368922449105678L)
class GradeInputSwitch extends LongId {

  var project: Project = _

  var semester: Semester = _

  /** 开始时间 */
  var beginAt: Instant = _

  /** 关闭时间 */
  var endAt: Instant = _

  /** 允许录入成绩类型 */
  var types = Collections.newSet[GradeType]

  /** 成绩录入开关 */
  var opened: Boolean = _

  /** 成绩录入验证开关 */
  var needValidate: Boolean = false

  /** 备注 */
  var remark: String = _

  /**
   * 检查该开关是否开放
   *
   * @param date
   * @return
   */
  def checkOpen(date: Instant): Boolean = {
    if (null == beginAt || null == endAt) {
      return false
    }
    if (date.isAfter(endAt) || beginAt.isAfter(date)) {
      false
    } else {
      opened
    }
  }

  def checkOpen(): Boolean = checkOpen(Instant.now)
}
