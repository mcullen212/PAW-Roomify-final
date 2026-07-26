import { describe, expect, it } from "vitest"
import {
    isAllowedRoomImageType,
    isRoomImageWithinSizeLimit,
    MAX_ROOM_IMAGE_SIZE_BYTES,
} from "../room-images"

describe("room image helpers", () => {
    it("allows the room image MIME types accepted by the API", () => {
        expect(isAllowedRoomImageType(new File(["image"], "room.jpg", { type: "image/jpeg" }))).toBe(true)
        expect(isAllowedRoomImageType(new File(["image"], "room.png", { type: "image/png" }))).toBe(true)
        expect(isAllowedRoomImageType(new File(["image"], "room.webp", { type: "image/webp" }))).toBe(true)
    })

    it("rejects unsupported room image MIME types", () => {
        expect(isAllowedRoomImageType(new File(["image"], "room.gif", { type: "image/gif" }))).toBe(false)
        expect(isAllowedRoomImageType(new File(["document"], "room.pdf", { type: "application/pdf" }))).toBe(false)
    })

    it("allows files up to 5MB", () => {
        expect(isRoomImageWithinSizeLimit(new File([new Uint8Array(MAX_ROOM_IMAGE_SIZE_BYTES)], "room.jpg"))).toBe(true)
        expect(isRoomImageWithinSizeLimit(new File([new Uint8Array(MAX_ROOM_IMAGE_SIZE_BYTES + 1)], "room.jpg"))).toBe(false)
    })
})
