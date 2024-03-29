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

package org.openurp.edu.grade.setting.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.code.service.CodeService
import org.openurp.code.edu.model.GradeType
import org.openurp.base.model.Project
import org.openurp.edu.grade.course.service.CourseGradeSettings
import CourseGradeSettingsImpl._
import org.openurp.edu.grade.course.service.CourseGradeSetting
import com.google.gson.Gson
import org.openurp.edu.grade.BaseServiceImpl

object CourseGradeSettingsImpl {

  private val COURSEGRADESETTING = "course.grade.setting"
}

class CourseGradeSettingsImpl extends BaseServiceImpl with CourseGradeSettings {

  private var cache = Collections.newMap[Integer, CourseGradeSetting]

  private var codeService: CodeService = _

  def getSetting(project: Project): CourseGradeSetting = {
    val settingStr = project.properties.get(COURSEGRADESETTING).orNull
    var setting: CourseGradeSetting = null
    if (Strings.isNotBlank(settingStr)) {
      val gson = new Gson()
      try {
        setting = gson.fromJson(settingStr, classOf[CourseGradeSetting])
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    if (null == setting) {
      setting = cache.get(project.id).orNull
      if (setting == null) {
        setting = new CourseGradeSetting(project)
        cache.put(project.id, setting)
      }
    }
    setting
  }

  def setCodeService(codeService: CodeService): Unit = {
    this.codeService = codeService
  }
}
