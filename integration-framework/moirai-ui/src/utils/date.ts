const moment = require("moment-timezone");
/**
 * Returns the current time in UTC as an epoch time (in seconds)
 * @returns The current epoch time in seconds as a string
 */
export function getCurrentEpoch(): string {
  var now = new Date();
  // Converts from UTC milliseconds to seconds
  var secondsSinceEpoch = Math.round(now.getTime() / 1000);
  return secondsSinceEpoch.toString();
}

/**
 * Converts date to this project's standard format for displaying a date to a user
 * @param originalDate Date to be coverted
 * @returns The date in 'MM/DD/YYYY' where M and D are without a preceding 0
 */
export function toStandardViewDate(originalDate: Date): string {
  return originalDate.toLocaleDateString();
}

/**
 * Converts date to LocaleDateTime
 * @param originalDate Date to be coverted
 * @returns The date in 'MM/DD/YYYY, HH:MM:SS ?M' where M and D are without a preceding 0
 */
export function toLocaleDateTime(original: string): string {
  let date = new Date(original);
  return date.toLocaleString();
}

/**
 * Takes the standard date/time and returns it in the users timezone
 * @param standardTime time in UTC timezone
 *   - should be formatted 'YYYY-MM-DD tt:tt' ex. 2016-08-01 11:00
 * @param userTimeZone to convert standard time into
 * @returns The date in the user's respective timezone in 'YYYY-MM-DD HH:hh' ex. 2013-12-01 00:00
 */
export function standardToUserTimezone(standardTime: string, userTimeZone: string): string {
  let utcTime = moment.tz(standardTime, "UTC");
  let userTime = utcTime.clone().tz(userTimeZone ? userTimeZone : "America/New_York");

  return userTime.format("YYYY-MM-DD HH:mm");
}
/**
 * Takes the standard date/time and returns just the date in the users timezone
 * @param standardTime time in UTC timezone
 * @param userTimeZone time zone to convert to
 * @returns The date in user's time zone in 'YYYY-MM-DD'
 */
export function dateInUserTimeZone(standardTime: string, userTimeZone: string): string {
  let utcTime = moment.tz(standardTime, "UTC");
  let userTime = utcTime.clone().tz(userTimeZone ? userTimeZone : "America/New_York");

  return userTime.format("YYYY-MM-DD");
}

/**
 * Takes the standard date/time and returns just the time in the users timezone
 * @param standardTime time in UTC timezone
 * @param userTimeZone time zone to convert to
 * @returns The time in user's time zone in 'HH:mm:ss'
 */
export function timeInUserTimeZone(standardTime: string, userTimeZone: string): string {
  let utcTime = moment.tz(standardTime, "UTC");
  let userTime = utcTime.clone().tz(userTimeZone ? userTimeZone : "America/New_York");

  return userTime.format("HH:mm:ss"); //ex. 15:43:12
}

/**
 * Takes the date as input, the time as input, using the user’s timezone
 * returns the combination in the standard +0 timezone
 * @param userTime string returned by moment.format of user time
 * @param userTimeZone user's timezone
 * @returns The time in user's time zone in "YYYY-MM-DD HH:mm"
 */
export function userTimeToStandard(userTime: string, userTimeZone: string): string {
  let tempTime = moment.tz(userTime, userTimeZone ? userTimeZone : "America/New_York");
  let standardTime = tempTime.clone().tz("UTC");

  return standardTime.format();
}

export function dateInRange(startDate: any, endDate: any): boolean {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const current = new Date();

  return current >= start && current <= end;
}

export function compareDates(date1: any, date2: any): number {
  const a = new Date(date1);
  const b = new Date(date2);

  if (date1 < date2) {
    return -1;
  } else if (date1 > date2) {
    return 1;
  }
  return 0;
}
