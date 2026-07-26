const MONTH_KEYS = [
    "months.january",
    "months.february",
    "months.march",
    "months.april",
    "months.may",
    "months.june",
    "months.july",
    "months.august",
    "months.september",
    "months.october",
    "months.november",
    "months.december",
] as const;

type TFunction = (key: string) => string;

type DateInput = Date | string;

export const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

export function formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, "0");
    const day = `${date.getDate()}`.padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function toDate(date: DateInput): Date {
    if (date instanceof Date) {
        return date;
    }

    return new Date(date.includes("T") ? date : `${date}T00:00:00`);
}

export function formatDateNumeric(date: DateInput): string {
    return new Intl.DateTimeFormat("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    }).format(toDate(date));
}

export function formatDateWithMonthText(
    date: DateInput,
    t: TFunction,
): string {
    const parsedDate = toDate(date);
    const day = parsedDate.getDate().toString().padStart(2, "0");
    const month = t(MONTH_KEYS[parsedDate.getMonth()]);
    const year = parsedDate.getFullYear();

    return `${day} ${month} ${year}`;
}

export function formatDateRangeWithMonthText(
    startDate: DateInput,
    endDate: DateInput,
    t: TFunction,
): string {
    const start = toDate(startDate);
    const end = toDate(endDate);
    const startDay = start.getDate().toString().padStart(2, "0");
    const endDay = end.getDate().toString().padStart(2, "0");
    const startMonth = t(MONTH_KEYS[start.getMonth()]);
    const endMonth = t(MONTH_KEYS[end.getMonth()]);
    const startYear = start.getFullYear();
    const endYear = end.getFullYear();

    if (startYear === endYear && start.getMonth() === end.getMonth()) {
        return `${startDay} - ${endDay} ${endMonth} ${endYear}`;
    }

    if (startYear === endYear) {
        return `${startDay} ${startMonth} - ${endDay} ${endMonth} ${endYear}`;
    }

    return `${startDay} ${startMonth} ${startYear} - ${endDay} ${endMonth} ${endYear}`;
}

export function formatApiDateNumeric(date: string): string {
    if (!date) return "";

    return formatDateNumeric(date);
}

export function formatApiDateWithMonthText(
    date: string,
    t: TFunction,
): string {
    if (!date) return "";

    return formatDateWithMonthText(date, t);
}

export function formatApiDateRangeWithMonthText(
    startDate: string,
    endDate: string,
    t: TFunction,
): string {
    if (!startDate || !endDate) return "";

    return formatDateRangeWithMonthText(startDate, endDate, t);
}

export function countDays(startDate?: string | null, endDate?: string | null) {
    if (!startDate || !endDate) return 0;

    const start = Date.parse(startDate);
    const end = Date.parse(endDate);
    if (!Number.isFinite(start) || !Number.isFinite(end)) return 0;

    return Math.max(0, Math.round((end - start) / MILLISECONDS_PER_DAY));
}

export function countDaysUntil(startDate?: string | null) {
    if (!startDate) return undefined;

    const start = Date.parse(startDate);
    if (!Number.isFinite(start)) return undefined;

    const today = getTodayDate();

    return Math.max(0, Math.ceil((start - today.getTime()) / MILLISECONDS_PER_DAY));
}

export function getTodayDate() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return today;
}

export function getTodayDateInputValue() {
    return formatDateForApi(getTodayDate());
}

export function isBeforeToday(dateValue?: string | null) {
    if (!dateValue) return false;

    return dateValue < getTodayDateInputValue();
}
