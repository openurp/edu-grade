package org.openurp.edu.grade

import org.openurp.edu.grade.app.service.impl.GradeInputSwithServiceImpl
import org.openurp.edu.grade.app.service.impl.ReportTemplateServiceImpl
import org.beangle.cdi.bind.BindModule
import org.openurp.edu.grade.course.service.{CourseGradePublishStack, GradingModeHelper}
import org.openurp.edu.grade.course.service.impl.BestGpaStatService
import org.openurp.edu.grade.course.service.impl.BestGradeFilter
import org.openurp.edu.grade.course.service.impl.BestOriginGradeFilter
import org.openurp.edu.grade.course.service.impl.DefaultCourseGradeCalculator
import org.openurp.edu.grade.course.domain.DefaultGpaPolicy
import org.openurp.edu.grade.course.service.impl.DefaultGpaService
import org.openurp.edu.grade.course.service.impl.DefaultGradeTypePolicy
import org.openurp.edu.grade.course.service.impl.DefaultGradingModeStrategy
import org.openurp.edu.grade.course.service.impl.ExamTakerGeneratePublishListener
import org.openurp.edu.grade.course.service.impl.MakeupByExamStrategy
import org.openurp.edu.grade.course.service.impl.MakeupGradeFilter
import org.openurp.edu.grade.course.domain.NumRounder
import org.openurp.edu.grade.course.service.impl.PassedGradeFilter
import org.openurp.edu.grade.course.service.impl.RecalcGpPublishListener
import org.openurp.edu.grade.course.service.impl.ScriptGradeFilter
import org.openurp.edu.grade.course.service.impl.SpringGradeFilterRegistry
import org.openurp.edu.grade.course.service.impl.StdGradeServiceImpl
import org.openurp.edu.grade.course.service.internal.BestCourseGradeProviderImpl
import org.openurp.edu.grade.course.service.internal.CourseGradeProviderImpl
import org.openurp.edu.grade.course.service.internal.CourseGradeServiceImpl
import org.openurp.edu.grade.course.service.internal.GradeCourseTypeProviderImpl
import org.openurp.edu.grade.course.service.internal.GradeRateServiceImpl
import org.openurp.edu.grade.setting.service.impl.CourseGradeSettingsImpl
import org.openurp.edu.grade.transcript.service.impl.SpringTranscriptDataProviderRegistry
import org.openurp.edu.grade.transcript.service.impl.TranscriptGpaProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPlanCourseProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPublishedExternExamGradeProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPublishedGradeProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptStdGraduationProvider

class GradeServiceModule extends BindModule {

  protected override def binding() {
    bind("bestGradeCourseGradeProvider", classOf[BestCourseGradeProviderImpl])
    bind(classOf[CourseGradeSettingsImpl])
    bind("gradeRateService", classOf[GradeRateServiceImpl])
    bind("bestGradeFilter", classOf[BestGradeFilter])
    bind("gpaPolicy", classOf[DefaultGpaPolicy])
    bind("bestOriginGradeFilter", classOf[BestOriginGradeFilter])
    bind("passedGradeFilter", classOf[PassedGradeFilter])
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
    bind("NumRounder.Normal",NumRounder.Normal)
    bind(classOf[TranscriptPlanCourseProvider], classOf[TranscriptGpaProvider], classOf[TranscriptPublishedGradeProvider], 
      classOf[TranscriptStdGraduationProvider], classOf[SpringTranscriptDataProviderRegistry], classOf[TranscriptPublishedExternExamGradeProvider])
      .shortName()
  }
}
