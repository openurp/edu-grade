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
package org.openurp.edu.grade.transcript.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.collection.Order
import org.beangle.data.dao.EntityDao
import org.beangle.data.dao.OqlBuilder
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.transcript.service.TranscriptDataProvider
import scala.collection.mutable.Buffer
import org.openurp.edu.extern.exam.model.ExternExamGrade

class TranscriptPublishedExternExamGradeProvider extends TranscriptDataProvider {

  var entityDao: EntityDao = _

  override def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    val builder = OqlBuilder.from(classOf[ExternExamGrade], "eeg")
    builder.where("eeg.std in (:stds)", stds)
    builder.where("eeg.passed = true")
    builder.orderBy(Order.parse("eeg.score"))
    val eegs = entityDao.search(builder)
    val mapData = Collections.newMap[Student, Buffer[ExternExamGrade]]
    eegs foreach { eeg =>
      val lst = mapData.getOrElseUpdate(eeg.std, Collections.newBuffer[ExternExamGrade])
      lst += eeg
    }
    mapData
  }

  override def dataName: String = "externExamGrades"

}
