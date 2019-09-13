/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.edu.grade.app.service.impl

import org.beangle.data.dao.impl.BaseServiceImpl
import org.beangle.data.dao.OqlBuilder
import org.openurp.edu.base.model.Semester
import org.openurp.edu.base.model.Project
import org.openurp.edu.grade.app.model.GradeInputSwitch
import org.openurp.edu.grade.app.service.GradeInputSwitchService
import java.time.Instant

class GradeInputSwithServiceImpl extends BaseServiceImpl with GradeInputSwitchService {

  def getSwitch(project: Project, semester: Semester): GradeInputSwitch = {
    val query = OqlBuilder.from(classOf[GradeInputSwitch], "switch")
    query.where("switch.project=:project", project)
    query.where("switch.semester=:semester", semester)
    query.where("switch.opened = true")
    entityDao.uniqueResult(query)
  }

  def getOpenedSemesters(project: Project): Seq[Semester] = {
    val query = OqlBuilder.from[Semester](classOf[GradeInputSwitch].getName, "s")
    query.where("s.project=:project", project)
    query.where("s.opened = true and s.endAt>=:now", Instant.now)
    query.orderBy("s.semester.beginOn").select("s.semester")
    entityDao.search(query)
  }
}
