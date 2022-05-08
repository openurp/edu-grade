/*
 * Copyright (C) 2014, The OpenURP Software.
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

package org.openurp.edu.grade.app.service

import org.openurp.base.model.Semester
import org.openurp.base.model.Project
import org.openurp.edu.grade.app.model.GradeInputSwitch

trait GradeInputSwitchService {

  def getSwitch(project: Project, semester: Semester): GradeInputSwitch

  /**
   * 查询结束日期在当前时间之后，且开放的学年学期
   *
   * @param project
   * @return
   */
  def getOpenedSemesters(project: Project): Seq[Semester]
}
