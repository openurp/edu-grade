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
package org.openurp.edu.grade.transcript.service.impl

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Student
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.domain.CourseGradeProvider
import org.openurp.edu.grade.course.service.impl.GradeFilterRegistry
import org.openurp.edu.grade.transcript.service.TranscriptDataProvider
import org.openurp.edu.grade.course.domain.GradeFilter
import org.openurp.edu.grade.course.domain.GpaPolicy
import org.openurp.edu.grade.course.model.StdGpa

/**
 * 成绩绩点提供者
 *
 * @since 2012-05-21
 */
class TranscriptGpaProvider extends TranscriptDataProvider {

  private var courseGradeProvider: CourseGradeProvider = _

  private var gpaPolicy: GpaPolicy = _

  private var gradeFilterRegistry: GradeFilterRegistry = _

  def dataName: String = "gpas"

  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    val matched = getFilters(options)
    val datas = Collections.newMap[Student, StdGpa]
    val gradeMap = courseGradeProvider.getPublished(stds)
    for (std <- stds) {
      var grades:Iterable[CourseGrade] = gradeMap(std)
      for (filter <- matched) grades = filter.filter(grades)
      datas.put(std, gpaPolicy.calc(std, grades, true))
    }
    datas
  }

  /**
   * 获取相应的过滤器
   *
   * @param name
   * @return
   */
  protected def getFilters(options: collection.Map[String, String]): Seq[GradeFilter] = {
    if (null == options || options.isEmpty) return List.empty
    gradeFilterRegistry.getFilters(options("gpa.filters"))
  }

}
