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
package org.openurp.edu.grade.course.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.functor.Predicate
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.domain.GradeFilter

/**
 * 只留下通过课程的成绩
 */
class PassedGradeFilter extends GradeFilter {

  override def filter(grades: Seq[CourseGrade]): Seq[CourseGrade] = {
    grades.filter(_.passed)
  }
}
