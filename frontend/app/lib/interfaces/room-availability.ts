export type ApiDateRange = {
    startDate: string;
    endDate: string;
};

export type RoomAvailabilityCalendar = {
    roomId: number;
    self?: string;
    room?: string;
    availabilityRanges: ApiDateRange[];
    bookedRanges: ApiDateRange[];
    selectableRanges: ApiDateRange[];
    firstSelectableDate: string | null;
    hasSelectableStay: boolean;
};
