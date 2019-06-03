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
package org.openurp.edu.grade.course.service

import java.text.NumberFormat

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Numbers
import org.beangle.commons.lang.Strings
import org.beangle.commons.script.ExpressionEvaluator
import org.openurp.edu.grade.model.GradeRateConfig
import org.openurp.edu.grade.model.GradeRateItem

class ScoreConverter(private var config: GradeRateConfig, private var expressionEvaluator: ExpressionEvaluator) {

  /** 默认成绩 */
  var defaultScoreMap = Collections.newMap[String, Float]

  if (null != config) {
    var iterator = config.items.iterator
    while (iterator.hasNext) {
      val item = iterator.next()
      defaultScoreMap.put(item.grade, item.defaultScore)
    }
  }

  /**
   * 将数字按照成绩记录方式转换成字符串.<br>
   * 空成绩将转换成""
   *
   * @param score
   * @return
   */
  def convert(score: Option[Float]): String = {
    score match {
      case None => ""
      case Some(s) =>
        if (null == config) {
          NumberFormat.getInstance.format(s)
        } else {
          config.convert(s)
        }
    }
  }

  def passed(score: Option[Float]): Boolean = {
    if (null == config || score.isEmpty) {
      false
    } else {
      java.lang.Float.compare(score.get, config.passScore) >= 0
    }
  }

  /**
   * 将字符串按照成绩记录方式转换成数字.<br>
   * 空成绩将转换成null
   *
   * @param score
   * @param gradingMode
   * @return
   */
  def convert(score: String): Option[Float] = {
    if (Strings.isBlank(score)) return null
    if (null == config || config.items.size == 0) {
      if (Numbers.isDigits(score)) Some(Numbers.toFloat(score)) else None
    } else {
      defaultScoreMap.get(score) match {
        case None =>
          if (Numbers.isDigits(score)) {
            Some(Numbers.toFloat(score))
          } else {
            None
          }
        case p @ Some(newScore) => p
      }
    }
  }

  /**
   * 计算分数对应的绩点
   *
   * @param score
   * @param conifg
   * @return
   */
  def calcGp(score: Option[Float]): Option[Float] = {
    if (score.isEmpty || score.get <= 0) {
      Some(0)
    } else {
      val s = score.get
      config.items foreach { gri =>
        if (gri.contains(s)) {
          gri.gpExp match {
            case None => None
            case Some(exp) =>
              val data = Collections.newMap[String, Any]
              data.put("score", s)
              Some(expressionEvaluator.eval(exp, data, classOf[java.lang.Float]).floatValue)
          }
        }
      }
    }
    // 默认绩点为00;
    Some(0f)
  }
}
