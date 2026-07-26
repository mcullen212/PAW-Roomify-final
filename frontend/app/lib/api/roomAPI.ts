import type { AxiosInstance } from 'axios';
import api from './api';
import { VndType } from './vndTypes';
import type { Room } from '@/types';
import type { RoomAvailabilityCalendar } from '@/lib/interfaces/room-availability';
import type {
    RoomAvailabilityQuery,
    RoomCreateRequestPayload,
    RoomDetail,
    RoomUpdatePayload,
} from '@/lib/interfaces/rooms';

export type {
    RoomAvailabilityQuery,
    RoomAvailabilityRange,
    RoomCreatePayload,
    RoomCreateRequestPayload,
    RoomDetail,
    RoomUpdatePayload,
} from '@/lib/interfaces/rooms';

export const createRoomAPI = (client: AxiosInstance) => {

    const getRooms = (filters: any) => {
        const params = Object.fromEntries(
            Object.entries({
                page: filters.page || 1,
                pageSize: filters.pageSize,
                destination: filters.destination,
                checkIn: filters.checkIn,
                checkOut: filters.checkOut,
                roomType: filters.roomType,
                bedType: filters.bedType,
                privateBathroom: filters.privateBathroom,
                privateKitchen: filters.privateKitchen,
                amenities: filters.amenities,
                userId: filters.userId,
            }).filter(([, value]) => value !== undefined && value !== null && value !== "")
        );

        return client.get<Room[]>('/rooms', {
            headers: {
                Accept: 'application/vnd.roomify.room.v1.list+json'
            },
            params
        });
    };

    const getMyRooms = (userId: number, page = 1, pageSize = 100) => {
        return getRooms({ userId, page, pageSize });
    };

    const getRoomById = (id: number) => {
        return client.get<RoomDetail>(`/rooms/${id}`, {
            headers: {
                Accept: 'application/vnd.roomify.room.v1+json'
            },
        });

    };

    const createImage = (image: File) => {
        const formData = new FormData();
        formData.append("image", image);

        return client.post<void>('/images', formData, {
            headers: {
                Accept: VndType.APPLICATION_IMAGE
            }
        });
    };

    const createRoom = (roomData: RoomCreateRequestPayload) => {
        return client.post('/rooms', roomData, {
            headers: {
                Accept: VndType.APPLICATION_ROOM_DETAIL,
                "Content-Type": VndType.APPLICATION_ROOM_DETAIL
            }
        });
    };

    const deleteRoom = (id: number) => {
        return client.delete(`/rooms/${id}`, {
            headers: {
                Accept: 'application/vnd.roomify.room.v1+json'
            }
        });
    };

    const updateRoom = (id: number, updateData: RoomUpdatePayload) => {
        return client.patch(`/rooms/${id}`, updateData, {
            headers: {
                Accept: VndType.APPLICATION_ROOM_DETAIL,
                "Content-Type": VndType.APPLICATION_ROOM_DETAIL
            }
        });
    };

    const getRoomAvailability = (id: number, query: RoomAvailabilityQuery = {}) => {
        const params = query.startDate && query.endDate
            ? {
                startDate: query.startDate,
                endDate: query.endDate,
            }
            : undefined;
        const config = {
            headers: {
                Accept: VndType.APPLICATION_ROOM_AVAILABILITY
            },
            ...(params ? { params } : {}),
        };

        return client.get<RoomAvailabilityCalendar>(`/rooms/${id}/availabilities`, config);
    };

    return {
        getRooms,
        getMyRooms,
        getRoomById,
        createImage,
        createRoom,
        deleteRoom,
        updateRoom,
        getRoomAvailability,
    };
};

const roomApi = createRoomAPI(api);

export default roomApi;
