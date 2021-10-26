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

package org.openurp.edu.grade.transcript.service.impl

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Student
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.domain.CourseGradeProvider
import org.openurp.edu.grade.course.service.impl.GradeFilterRegistry
import org.openurp.edu.grade.transcript.service.TranscriptDataProvider
import org.openurp.edu.grade.course.domain.GradeFilter

/**
 * 提供成绩单发布的成绩及其过滤逻辑
 *
 *
 * @since 2012-05-21
 */
class TranscriptPublishedGradeProvider extends TranscriptDataProvider {

  var registry: GradeFilterRegistry = _

  var courseGradeProvider: CourseGradeProvider = _

  def dataName: String = "grades"

  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    val datas = Collections.newMap[Student, collection.Iterable[CourseGrade]]
    val matched = getFilters(options)
    val gradeMap = courseGradeProvider.getPublished(stds)
    for (std <- stds) {
      var grades:Iterable[CourseGrade] = gradeMap(std)
      for (filter <- matched) {
        grades = filter.filter(grades)
      }
      datas.put(std, grades)
    }
    datas
  }

  protected def getFilters(options: collection.Map[String, String]): Seq[GradeFilter] = {
    if (null == options || options.isEmpty) return List.empty
    registry.getFilters(options("grade.filters"))
  }

}
