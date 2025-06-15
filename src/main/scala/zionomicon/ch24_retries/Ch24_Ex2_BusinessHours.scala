package zionomicon.ch24_retries

import zio.*

import java.time.{DayOfWeek, ZoneId}

/**
 * Create a schedule that only retries during “business hours” (9 AM to 5 PM) on
 * week days, with a one-hour delay between attempts.
 */
object Ch24_Ex2_BusinessHours:

  val zoneId: ZoneId = ZoneId.of("Europe/Berlin")

  val businessDays: Range.Inclusive =
    DayOfWeek.MONDAY.getValue to DayOfWeek.FRIDAY.getValue
  val businessHours: Range =
    9 until 18

  val businessHoursSchedule =
    Schedule.fixed(1.hour)
      .mapZIO(_ => Clock.instant.map(_.atZone(zoneId)))
      // Will be available in ZIO 2.2.0
      //.filterOutput(dt =>
      //  (businessDays `contains` dt.getDayOfWeek) &&
      //    (businessHours `contains` dt.getHour)
      //)
