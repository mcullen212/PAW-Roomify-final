export const ROOM_FORM_LIMITS = {
    titleMaxLength: 50,
    cityMaxLength: 50,
    descriptionMaxLength: 500,
    priceIntegerDigits: 8,
    priceFractionDigits: 2,
} as const

const ROOM_TEXT_PATTERN = /^[\p{L}\p{N}\s.,!?¿¡'"-]*$/u
const ROOM_CITY_PATTERN = /^[\p{L}\s.'-]+$/u
const ROOM_PRICE_PATTERN = /^\d+(\.\d+)?$/

export type PostRoomValidationValues = {
    title: string
    city: string
    description: string
    price: string
}

export function validatePostRoomFields(values: PostRoomValidationValues): string | null {
    const title = values.title.trim()
    const city = values.city.trim()
    const description = values.description
    const price = values.price.trim()

    if (!title) {
        return "postRoom.errors.titleRequired"
    }
    if (title.length > ROOM_FORM_LIMITS.titleMaxLength) {
        return "postRoom.errors.titleTooLong"
    }
    if (!ROOM_TEXT_PATTERN.test(title)) {
        return "postRoom.errors.titlePattern"
    }

    if (!city) {
        return "postRoom.errors.cityRequired"
    }
    if (city.length > ROOM_FORM_LIMITS.cityMaxLength) {
        return "postRoom.errors.cityTooLong"
    }
    if (!ROOM_CITY_PATTERN.test(city)) {
        return "postRoom.errors.cityPattern"
    }

    if (description.length > ROOM_FORM_LIMITS.descriptionMaxLength) {
        return "postRoom.errors.descriptionTooLong"
    }
    if (!ROOM_TEXT_PATTERN.test(description)) {
        return "postRoom.errors.descriptionPattern"
    }

    if (!isValidRoomPrice(price)) {
        return "postRoom.errors.validPrice"
    }

    return null
}

function isValidRoomPrice(price: string) {
    if (!ROOM_PRICE_PATTERN.test(price)) {
        return false
    }

    const parsedPrice = Number(price)
    if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
        return false
    }

    const [integerPart, fractionPart = ""] = price.split(".")
    return integerPart.length <= ROOM_FORM_LIMITS.priceIntegerDigits
        && fractionPart.length <= ROOM_FORM_LIMITS.priceFractionDigits
}
