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

package org.openurp.edu.grade

import org.beangle.cdi.bind.BindModule
import org.openurp.edu.grade.app.service.impl.{GradeInputSwithServiceImpl, ReportTemplateServiceImpl}
import org.openurp.edu.grade.course.domain.{DefaultGpaPolicy, NumRounder}
import org.openurp.edu.grade.course.service.{CourseGradePublishStack, GradingModeHelper}
import org.openurp.edu.grade.course.service.impl._
import org.openurp.edu.grade.setting.service.impl.CourseGradeSettingsImpl
import org.openurp.edu.grade.transcript.service.impl._
import org.openurp.edu.program.domain.DefaultAlternativeCourseProvider

class GradeServiceModule extends BindModule {

  protected override def binding(): Unit = {
    bind("bestGradeCourseGradeProvider", classOf[BestCourseGradeProviderImpl])
    bind(classOf[CourseGradeSettingsImpl])
    bind("gradeRateService", classOf[GradeRateServiceImpl])
    bind("bestGradeFilter", classOf[BestGradeFilter])
    bind("gpaPolicy", classOf[DefaultGpaPolicy])
    bind("bestOriginGradeFilter", classOf[BestOriginGradeFilter])
    bind("gradeFilterRegistry", classOf[SpringGradeFilterRegistry])
    bind("courseGradeService", classOf[CourseGradeServiceImpl])
    bind("gradeInputSwithService", classOf[GradeInputSwithServiceImpl])
    bind(classOf[ReportTemplateServiceImpl])
    bind("scriptGradeFilter", classOf[ScriptGradeFilter])
    bind("courseGradeProvider", classOf[CourseGradeProviderImpl])
    bind("courseGradeCalculator", classOf[DefaultCourseGradeCalculator])
    bind("gpaService", classOf[DefaultGpaService])
    bind("bestGpaStatService", classOf[BestGpaStatService])
    bind("gradeCourseTypeProvider", classOf[GradeCourseTypeProviderImpl])
    bind("makeupStdStrategy", classOf[MakeupByExamStrategy])
    bind("gradingModeHelper", classOf[GradingModeHelper])
    bind("gradingModeStrategy", classOf[DefaultGradingModeStrategy])
    bind("stdGradeService", classOf[StdGradeServiceImpl])
    bind("makeupGradeFilter", classOf[MakeupGradeFilter])
    bind("recalcGpPublishListener", classOf[RecalcGpPublishListener])
    bind("examTakerGeneratePublishListener", classOf[ExamTakerGeneratePublishListener])
    bind("courseGradePublishStack", classOf[CourseGradePublishStack])
      .property("listeners", list(ref("recalcGpPublishListener"), ref("examTakerGeneratePublishListener")))
    bind(classOf[DefaultGradeTypePolicy])
    bind("NumRounder.Normal", NumRounder.Normal)
    bind(classOf[TranscriptPlanCourseProvider], classOf[TranscriptGpaProvider], classOf[TranscriptPublishedGradeProvider],
      classOf[TranscriptStdGraduationProvider], classOf[SpringTranscriptDataProviderRegistry], classOf[TranscriptPublishedExternExamGradeProvider])
      .shortName()

    bind("alternativeCourseProvider", classOf[DefaultAlternativeCourseProvider])
  }
}
