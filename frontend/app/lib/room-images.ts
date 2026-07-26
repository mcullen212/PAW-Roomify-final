export const MAX_ROOM_IMAGE_SIZE_BYTES = 5 * 1024 * 1024

export const ALLOWED_ROOM_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"])

export function isAllowedRoomImageType(file: File) {
    return ALLOWED_ROOM_IMAGE_TYPES.has(file.type)
}

export function isRoomImageWithinSizeLimit(file: File) {
    return file.size <= MAX_ROOM_IMAGE_SIZE_BYTES
}
